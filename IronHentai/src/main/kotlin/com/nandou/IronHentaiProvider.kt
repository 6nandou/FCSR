package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class IronHentaiProvider : MainAPI() {
    override var mainUrl = "https://ironhentai.com"
    override var name = "IronHentai"
    override var lang = "es"
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "directorio/?estado=2" to "En Emisión",
        "directorio/?censura=0" to "Sin Censura",
        "directorio/?genero=15" to "Vanilla",
        "directorio/?genero=13" to "Harem",
    )

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a > p") ?: this.selectFirst("p")
        val titleText = title?.text() ?: this.selectFirst("img")?.attr("alt") ?: return null
        
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val poster = this.selectFirst("img")?.attr("src")

        return newAnimeSearchResponse(titleText, href, TvType.NSFW) {
            this.posterUrl = fixUrl(poster ?: "")
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) "$mainUrl/${request.data}" else "$mainUrl/${request.data}&page=$page"
        val document = app.get(url).document
        val items = document.select("ul.directorio li article, article").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/directorio/?q=$query").document
        return document.select("ul.directorio li article, article").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text() ?: ""
        val poster = document.selectFirst(".portada img, .skeleton-loaded")?.attr("src") ?: ""
        val plot = document.selectFirst(".sinopsis")?.text() ?: ""

        val episodes = ArrayList<Episode>()
        val items = document.select("ul#eps li > a")

        if (items.isNotEmpty()) {
            items.reversed().forEachIndexed { index, element ->
                val epHref = fixUrl(element.attr("href"))
                if (epHref.contains("/ver/")) {
                    episodes.add(newEpisode(epHref) {
                        this.name = "Episodio ${index + 1}"
                        this.episode = index + 1
                    })
                }
            }
        } else {
            val slug = url.trimEnd('/').substringAfterLast("/")
            episodes.add(newEpisode("$mainUrl/ver/$slug-1") {
                this.name = "Episodio 1"
                this.episode = 1
            })
        }

        return newAnimeLoadResponse(title, url, TvType.NSFW) {
            this.posterUrl = fixUrl(poster)
            this.plot = plot
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        val links = document.select("iframe, #iframe-element, .descargas a, .video-player iframe")
        links.forEach { element ->
            val src = if (element.tagName() == "a") element.attr("href") else element.attr("src")
            if (src.isNullOrBlank()) return@forEach
            
            val fixedSrc = fixUrl(src)
            
            if (fixedSrc.contains("redirect.php?id=")) {
                val realUrl = fixedSrc.substringAfter("id=")
                if (realUrl.startsWith("http")) {
                    loadExtractor(realUrl, data, subtitleCallback, callback)
                }
            } else if (fixedSrc.endsWith(".mp4") || fixedSrc.contains("archive.org")) {
                callback.invoke(
                    ExtractorLink(
                        source = this.name,
                        name = "Mirror Direct",
                        url = fixedSrc,
                        referer = data,
                        quality = Qualities.Unknown.value,
                        isM3u8 = fixedSrc.contains(".m3u8")
                    )
                )
            } else if (fixedSrc.startsWith("http") && !fixedSrc.contains("google") && !fixedSrc.contains("facebook")) {
                loadExtractor(fixedSrc, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
