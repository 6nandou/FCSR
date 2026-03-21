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
        "directorio/?estado=2" to "En Emision",
        "directorio/?censura=0" to "Sin Censura",
        "directorio/?genero=15" to "Vanilla",
        "directorio/?genero=13" to "Harem",
    )

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".card-title p")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val poster = this.selectFirst("img")?.attr("src")

        return newAnimeSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = fixUrl(poster ?: "")
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // La URL se construye automáticamente usando request.data y la página
        val url = if (page <= 1) {
            "$mainUrl/${request.data}"
        } else {
            "$mainUrl/${request.data}&page=$page"
        }
        
        val document = app.get(url).document
        val items = document.select(".card").mapNotNull {
            it.toSearchResult()
        }
        
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/directorio/?q=$query").document
        return document.select(".card").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        val title = document.selectFirst("h1")?.text() ?: ""
        val poster = document.selectFirst(".portada img")?.attr("src") ?: ""
        val plot = document.selectFirst(".sinopsis")?.text()

        val episodes = ArrayList<Episode>()
        
        document.select(".lista-episodios a, .episodios-wrapper a").forEachIndexed { index, element ->
            val epHref = fixUrl(element.attr("href"))
            episodes.add(newEpisode(epHref) {
                this.name = element.text()
                this.episode = index + 1
            })
        }

        if (episodes.isEmpty()) {
            episodes.add(newEpisode(url) {
                this.name = "Película/OVA"
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
        
        document.select("iframe").forEach { iframe ->
            val src = fixUrl(iframe.attr("src"))
            if (!src.contains("google") && !src.contains("facebook")) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }
        
        document.select("script").forEach { script ->
            val code = script.data()
            if (code.contains("var frame = '")) {
                val url = code.substringAfter("src=\"").substringBefore("\"")
                loadExtractor(fixUrl(url), data, subtitleCallback, callback)
            }
        }

        return true
    }
}
