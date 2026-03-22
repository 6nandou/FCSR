package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

class ChileM3UProvider : MainAPI() {
    override var mainUrl = "https://m3u.cl/lista/CL.m3u"
    override var name = "Chile M3U"
    override var lang = "es"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Live)

    private suspend fun getM3uData(): List<M3uChannel> {
        val res = app.get(mainUrl).text
        val channels = mutableListOf<M3uChannel>()
        val lines = res.split("\n")
        
        var currentName = ""
        var currentLogo = ""
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF")) {
                currentName = trimmed.substringAfter("tvg-name=\"", "").substringBefore("\"", "")
                if (currentName.isEmpty()) {
                    currentName = trimmed.substringAfter(",").trim()
                }
                currentLogo = trimmed.substringAfter("tvg-logo=\"", "").substringBefore("\"", "")
            } else if (trimmed.startsWith("http")) {
                if (currentName.isNotBlank()) {
                    channels.add(M3uChannel(currentName, trimmed, currentLogo))
                }
            }
        }
        return channels
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = getM3uData().map { it.toSearchResult() }
        return newHomePageResponse("Canales de Chile", items)
    }

    private fun M3uChannel.toSearchResult(): SearchResponse {
        return LiveSearchResponse(
            name = this.name,
            url = this.url,
            apiName = this@ChileM3UProvider.name,
            type = TvType.Live,
            posterUrl = this.logo
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return getM3uData().filter { it.name.contains(query, ignoreCase = true) }.map {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val data = getM3uData()
        val channel = data.firstOrNull { it.url == url }
        val name = channel?.name ?: "Canal"
        
        return LiveLoadResponse(
            name = name,
            url = url,
            apiName = this.name,
            dataUrl = url,
            posterUrl = channel?.logo
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        callback.invoke(
            ExtractorLink(
                source = this.name,
                name = this.name,
                url = data,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = data.contains(".m3u8")
            )
        )
        return true
    }

    data class M3uChannel(val name: String, val url: String, val logo: String)
}
