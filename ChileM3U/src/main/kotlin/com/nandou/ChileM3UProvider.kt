package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

class LaTeleProvider : MainAPI() {
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
            if (line.startsWith("#EXTINF")) {
                currentName = line.substringAfter("tvg-name=\"").substringBefore("\"")
                if (currentName.isEmpty()) currentName = line.substringAfter(",").trim()
                currentLogo = line.substringAfter("tvg-logo=\"").substringBefore("\"")
            } else if (line.startsWith("http")) {
                channels.add(M3uChannel(currentName, line.trim(), currentLogo))
            }
        }
        return channels
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = getM3uData().map { 
            LiveSearchResponse(it.name, it.url, this.name, TvType.Live, it.logo)
        }
        return newHomePageResponse("Canales de Chile", items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return getM3uData().filter { it.name.contains(query, ignoreCase = true) }.map {
            LiveSearchResponse(it.name, it.url, this.name, TvType.Live, it.logo)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val channel = getM3uData().firstOrNull { it.url == url }
        val name = channel?.name ?: "Canal"
        return LiveLoadResponse(name, url, this.name, url, channel?.logo)
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        callback.invoke(
            newExtractorLink(
                this.name,
                this.name,
                data,
                "",
                Qualities.Unknown.value,
                data.contains(".m3u8")
            )
        )
        return true
    }

    data class M3uChannel(val name: String, val url: String, val logo: String)
}
