package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import java.util.Base64

class ChileM3UProvider : MainAPI() {
    override var mainUrl = "https://m3u.cl/lista-iptv-chile.php"
    override var name = "Chile M3U"
    override var lang = "es"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Live)

    private suspend fun getChannels(): List<M3uChannel> {
        val document = app.get(mainUrl).document
        val channels = mutableListOf<M3uChannel>()
        
        val rows = document.select("#tabla_canales tbody tr")
        
        for (row in rows) {
            try {
                val channelCell = row.select("td.videojs").first()
                if (channelCell != null) {
                    val encodedUrl = channelCell.attr("reproducir_canal")
                    val streamUrl = String(Base64.getDecoder().decode(encodedUrl))
                    val name = channelCell.attr("reproducir_nombre_canal")
                    val logoUrl = channelCell.attr("reproducir_logo_canal")
                    
                    val fullLogoUrl = if (logoUrl.startsWith("//")) {
                        "https:$logoUrl"
                    } else if (logoUrl.startsWith("/")) {
                        "https://m3u.cl$logoUrl"
                    } else {
                        logoUrl
                    }
                    
                    if (name.isNotBlank() && streamUrl.isNotBlank()) {
                        channels.add(M3uChannel(name, streamUrl, fullLogoUrl))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return channels
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = getChannels().map { it.toSearchResult() }
        return newHomePageResponse("Canales de Chile", items)
    }

    private fun M3uChannel.toSearchResult(): SearchResponse {
        return newLiveSearchResponse(
            name = this.name,
            url = this.url,
            apiName = this@ChileM3UProvider.name,
            type = TvType.Live,
            posterUrl = this.logo
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return getChannels().filter { it.name.contains(query, ignoreCase = true) }.map {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val channels = getChannels()
        val channel = channels.firstOrNull { it.url == url }
        val name = channel?.name ?: "Canal"
        
        return LiveLoadResponse(
            name = name,
            url = url,
            apiName = this.name,
            streamUrl = url,
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
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = data,
                referer = mainUrl,
                quality = Qualities.Unknown.value,
                isM3u8 = data.contains(".m3u8"),
                headers = mapOf("Referer" to mainUrl)
            )
        )
        return true
    }

    data class M3uChannel(val name: String, val url: String, val logo: String)
}
