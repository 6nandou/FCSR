package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class IronHentaiProvider : MainAPI() {
    override var mainUrl = "https://ironhentai.com/"
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
        val title = this.selectFirst("a p, .card-title p")?.text() 
            ?: this.selectFirst("img")?.attr("alt") 
            ?: return null
        
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val poster = this.selectFirst("img")?.attr("src")

        return newAnimeSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = fixUrl(poster ?: "")
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) "$mainUrl/${request.data}" else "$mainUrl/${request.data}&page=$page"
        val document = app.get(url).document
        val items = document.select("ul.directorio li article, .card").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/directorio/?q=$query").document
        return document.select("ul.directorio li article").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text() ?: ""
        val poster = document.selectFirst(".portada img, img.skeleton-loaded")?.attr("src") ?: ""
        val plot = document.selectFirst(".sinopsis")?.text() ?: ""

        val episodes = ArrayList<Episode>()
        val items = document.select(".episodios-wrapper li a, .lista-episodios a")
        
        if (items.isNotEmpty()) {
            items.forEachIndexed { index, element ->
                val epHref = fixUrl(element.attr("href"))
                episodes.add(newEpisode(epHref) {
                    this.name = element.selectFirst("p")?.text() ?: "Episodio ${index + 1}"
                    this.episode = index + 1
                })
            }
        } else {
            val slug = url.trimEnd('/').substringAfterLast("/")
            val verUrl = "$mainUrl/ver/$slug-1"
            episodes.add(newEpisode(verUrl) {
                this.name = title
                this.episode = 1
            })
        }

        return newAnimeLoadResponse(title, url, TvType.NSFW) {
            this.posterUrl = fixUrl(poster)
            this.plot = plot
            addEpisodes(DubStatus.Subbed, episodes.reversed())
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        document.select("iframe, .reproductor iframe").forEach { iframe ->
            val src = fixUrl(iframe.attr("src"))
            
            if (src.contains("redirect.php?id=")) {
                val realUrl = src.substringAfter("id=")
                if (realUrl.startsWith("http")) {
                    loadExtractor(realUrl, data, subtitleCallback, callback)
                }
            } else if (!src.contains("google") && !src.contains("facebook")) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }

        document.select("script").forEach { script ->
            val code = script.data()
            if (code.contains("var frame = '") || code.contains("src=\"")) {
                val extractedUrl = code.substringAfter("src=\"", "").substringBefore("\"", "")
                if (extractedUrl.contains("http")) {
                    loadExtractor(fixUrl(extractedUrl), data, subtitleCallback, callback)
                }
            }
        }

        return true
    }
}
