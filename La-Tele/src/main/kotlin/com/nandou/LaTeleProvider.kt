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
        val items = document.select("button.boton-canal").map { it.toSearchResult() }
        return newHomePageResponse("Canales Chilenos", items)
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.text().trim()
        return LiveSearchResponse(
            title,
            title,
            this@LaTeleProvider.name,
            TvType.Live,
            null
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get(mainUrl).document
        return document.select("button.boton-canal")
            .map { it.toSearchResult() }
            .filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun load(url: String): LoadResponse {
        return LiveLoadResponse(
            url,
            url,
            this.name,
            url,
            null,
            dataUrl = url
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(mainUrl).document
        
        val videoSrc = document.selectFirst("video source")?.attr("src")
            ?: document.selectFirst("video")?.attr("src")
            ?: document.html().substringAfter("source: '", "").substringBefore("'")
            ?: document.html().substringAfter("file: \"", "").substringBefore("\"")

        if (videoSrc.isNotBlank()) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = data,
                    url = fixUrl(videoSrc),
                    refererUrl = "$mainUrl/",
                ) {
                    this.quality = Qualities.Unknown.value
                }
            )
        }

        return true
    }
}
