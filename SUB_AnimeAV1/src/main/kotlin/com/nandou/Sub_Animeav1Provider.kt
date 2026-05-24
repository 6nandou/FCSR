package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Document
import java.lang.Exception

class Animeav1Provider : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA", ignoreCase = true) || t.contains("Especial", ignoreCase = true)) TvType.OVA
            else if (t.contains("Película", ignoreCase = true) || t.contains("Movie", ignoreCase = true)) TvType.AnimeMovie
            else TvType.Anime
        }
    }

    override var mainUrl = "https://animeav1.com"
    override var name = "AnimeAV1"
    override var lang = "es"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val hasQuickSearch = false
    override val supportedTypes = setOf(
        TvType.AnimeMovie,
        TvType.OVA,
        TvType.Anime,
    )
    
    override val mainPage = mainPageOf(
        "catalogo?status=emision" to "En emision",
        "catalogo?genre=accion" to "Accion",
        "catalogo?genre=comedia&genre=romance" to "RomCom",
        "catalogo?genre=romance" to "Romance",
        "catalogo?genre=comedia" to "Comedia",
        "catalogo?genre=fantasia" to "Fantasia",
        "catalogo?genre=shounen" to "Shounen",
        "catalogo?genre=ecchi" to "Ecchi",
    )
    
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = ArrayList<HomePageList>()
        
        try {
            val doc = app.get(mainUrl).document
            val latestEpisodes = doc.select("div.grid article, .latest-episodes a").mapNotNull {
                val title = it.selectFirst("h3, .title")?.text() ?: return@mapNotNull null
                val poster = it.selectFirst("img")?.attr("src") ?: return@mapNotNull null
                val href = it.selectFirst("a")?.attr("href") ?: it.attr("href") ?: return@mapNotNull null
                
                newAnimeSearchResponse(title, fixUrl(href)) {
                    this.posterUrl = fixUrl(poster)
                }
            }
            
            if (latestEpisodes.isNotEmpty()) {
                items.add(HomePageList("Últimos Episodios", latestEpisodes, true))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return newHomePageResponse(items, false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResults = mutableListOf<SearchResponse>()
        try {
            val doc = app.get("$mainUrl/?search=$query").document
            doc.select("div.grid article, .search-results a").forEach { element ->
                val title = element.selectFirst("h3, .title")?.text() ?: return@forEach
                val poster = element.selectFirst("img")?.attr("src") ?: ""
                val href = element.selectFirst("a")?.attr("href") ?: element.attr("href") ?: ""
                if (href.isNotEmpty()) {
                    searchResults.add(
                        newAnimeSearchResponse(title, fixUrl(href)) {
                            this.posterUrl = fixUrl(poster)
                        }
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return searchResults
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val episodes = ArrayList<Episode>()
        val title = doc.selectFirst("h1")?.text() ?: ""
        val poster = doc.selectFirst("img.poster, div.cover img")?.attr("src") ?: ""
        val description = doc.selectFirst("p.synopsis, div.description p")?.text()
        doc.select("a[href*=/media/]").forEach { epLink ->
            val epUrl = epLink.attr("href")
            val epNum = epUrl.trimEnd('/').substringAfterLast("/").toIntOrNull()
            if (epNum != null) {
                episodes.add(
                    newEpisode(fixUrl(epUrl)) {
                        this.episode = epNum
                        this.name = "Episodio $epNum"
                    }
                )
            }
        }
        val sortedEpisodes = episodes.distinctBy { it.episode }.sortedBy { it.episode }
        return newAnimeLoadResponse(title, url, getType(title)) {
            this.posterUrl = fixUrl(poster)
            addEpisodes(DubStatus.Subbed, sortedEpisodes)
            this.plot = description
        }
    }
    
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        try {
            val doc = app.get(data).document
            val defaultIframe = doc.selectFirst("iframe[title='Episodio Embebido'], iframe.aspect-video")?.attr("src")
            if (!defaultIframe.isNullOrBlank()) {
                loadExtractor(fixUrl(defaultIframe), data, subtitleCallback, callback)
            }
            
            val serverButtons = doc.select("div.flex-1.flex-wrap button, button.btn")
            serverButtons.forEach { button ->
                val alternativeUrl = button.attr("data-src").ifBlank { button.attr("data-video") }
                
                if (alternativeUrl.isNotBlank()) {
                    loadExtractor(fixUrl(alternativeUrl), data, subtitleCallback, callback)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }
}
