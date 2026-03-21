package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Element

class IronHentaiProvider : MainAPI() {
    override var mainUrl = "https://ironhentai.com"
    override var name = "IronHentai"
    override var lang = "es"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.NSFW)

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".entry-title a")?.text() ?: return null
        val href = fixUrl(this.selectFirst(".entry-title a")?.attr("href") ?: return null)
        val poster = this.selectFirst(".entry-thumbnail img")?.attr("src")

        return newAnimeSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = fixUrl(poster ?: "")
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/page/$page/").document
        val items = document.select("article.entry-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(listOf(HomePageList("Latest Updates", items)), true)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article.entry-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title")?.text() ?: ""
        val poster = document.selectFirst(".entry-thumbnail img")?.attr("src")
        
        val images = document.select(".entry-content img").map { it.attr("src") }

        return newAnimeLoadResponse(title, url, TvType.NSFW) {
            this.posterUrl = fixUrl(poster ?: "")
            this.plot = "Read ${images.size} pages on IronHentai"
            addEpisodes(DubStatus.Subbed, listOf(newEpisode(url) {
                this.name = "Full Gallery"
                this.episode = 1
            }))
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select(".entry-content img").forEach { img ->
            val link = img.attr("src")
        }
        return true
    }
}
