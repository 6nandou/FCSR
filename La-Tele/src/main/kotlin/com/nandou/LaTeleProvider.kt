package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.nodes.Element

class LaTeleProvider : MainAPI() {
    override var mainUrl = "https://alplox.github.io/la-tele"
    override var name = "La Tele"
    override var lang = "es"
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Live)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val items = document.select(".card, .channel-item, article, .col-md-3").mapNotNull { it.toSearchResult() }
        return newHomePageResponse("Canales", items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".card-title, h5, p, .name")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val poster = this.selectFirst("img")?.attr("src")

        return newAnimeSearchResponse(title, href, TvType.Live) {
            this.posterUrl = fixUrl(poster ?: "")
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get(mainUrl).document
        return document.select(".card, .channel-item, article, .col-md-3").mapNotNull { it.toSearchResult() }.filter { 
            it.name.contains(query, ignoreCase = true) 
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1, .channel-title, .card-title, .name")?.text() ?: "Canal TV"
        val poster = document.selectFirst("img.channel-logo, .card-img-top, img")?.attr("src") ?: ""
        
        val episodes = listOf(
            newEpisode(url) {
                this.name = title
            }
        )

        return newAnimeLoadResponse(title, url, TvType.Live) {
            this.posterUrl = fixUrl(poster)
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
        
        val videoSrc = document.selectFirst("video source")?.attr("src") 
            ?: document.selectFirst("video")?.attr("src")
            ?: document.html().substringAfter("source: '", "").substringBefore("'")
            ?: document.html().substringAfter("file: \"", "").substringBefore("\"")
            ?: document.selectFirst("iframe")?.attr("src")

        if (!videoSrc.isNullOrBlank()) {
            val finalUrl = fixUrl(videoSrc)
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = finalUrl,
                    referer = "$mainUrl/",
                ) {
                    this.quality = Qualities.Unknown.value
                    this.isM3u8 = finalUrl.contains(".m3u8")
                }
            )
        }

        return true
    }
}
