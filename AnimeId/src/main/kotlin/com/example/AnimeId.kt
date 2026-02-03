package com.example

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class AnimeId : MainAPI() { 
    override var mainUrl = "https://animeidhentai.com"
    override var name = "AnimeIdHentai"
    override val hasMainPage = true
    override var lang = "es"
    override val supportedTypes = setOf(TvType.NSFW)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) "$mainUrl/genre/2020/" else "$mainUrl/genre/2020/page/$page/"
        val document = app.get(url).document
        val items = document.select("article.item, div.anime-card, .hentai-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(listOf(HomePageList("Animes 2020", items)), true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3, .title, header")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrl(this.selectFirst("img")?.attr("src") ?: "")

        return newAnimeSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article.item, div.anime-card").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.title, .entry-title")?.text() ?: ""
        val poster = fixUrl(document.selectFirst("div.thumb img, .poster img")?.attr("src") ?: "")
        val plot = document.selectFirst("p.sinopsis, .description, .entry-content p")?.text()

        return newAnimeLoadResponse(title, url, TvType.NSFW) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return true
    }
}
