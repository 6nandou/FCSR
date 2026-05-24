package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element
import java.lang.Exception

class Animeav1Provider : MainAPI() {
    override var mainUrl = "https://animeav1.com"
    override var name = "Sub_AnimeAV1"
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

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3, .title") ?: this.selectFirst("a > p") ?: this.selectFirst("p")
        val titleText = title?.text() ?: this.selectFirst("img")?.attr("alt") ?: return null
        
        val href = this.selectFirst("a")?.attr("href") ?: this.attr("href") ?: return null
        val poster = this.selectFirst("img")?.attr("src")

        return newAnimeSearchResponse(titleText, fixUrl(href)) {
            this.posterUrl = fixUrl(poster ?: "")
        }
    }
    
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = ArrayList<HomePageList>()
        
        try {
            val doc = app.get(mainUrl).document
            val latestEpisodes = doc.select("div.grid article, .latest-episodes a, ul.directorio li article").mapNotNull { 
                it.toSearchResult() 
            }
            if (latestEpisodes.isNotEmpty()) {
                items.add(HomePageList("Últimos Episodios", latestEpisodes, true))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val url = if (page <= 1) "$mainUrl/${request.data}" else "$mainUrl/${request.data}&page=$page"
            val doc = app.get(url).document
            val categoryItems = doc.select("div.grid article, article, ul.directorio li article").mapNotNull { 
                it.toSearchResult() 
            }
            if (categoryItems.isNotEmpty()) {
                items.add(HomePageList(request.name, categoryItems, false))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return newHomePageResponse(items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResults = mutableListOf<SearchResponse>()
        try {
            val doc = app.get("$mainUrl/catalogo?search=$query").document
            doc.select("div.grid article, .search-results a, ul.directorio li article, article").forEach { element ->
                val result = element.toSearchResult()
                if (result != null) {
                    searchResults.add(result)
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
        val poster = doc.selectFirst("img.poster, div.cover img, .portada img")?.attr("src") ?: ""
        val description = doc.selectFirst("p.synopsis, div.description p, .sinopsis")?.text()
        
        doc.select("a[href*=/media/], ul#eps li > a").forEach { epLink ->
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
        
        if (sortedEpisodes.isEmpty()) {
            val slug = url.trimEnd('/').substringAfterLast("/")
            episodes.add(
                newEpisode("$mainUrl/ver/$slug-1") {
                    this.name = "Episodio 1"
                    this.episode = 1
                }
            )
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = fixUrl(poster)
            addEpisodes(DubStatus.Subbed, if (sortedEpisodes.isNotEmpty()) sortedEpisodes else episodes)
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
            val defaultIframe = doc.selectFirst("iframe[title='Episodio Embebido'], iframe.aspect-video, #iframe-element, .video-player iframe")?.attr("src")
            if (!defaultIframe.isNullOrBlank()) {
                loadExtractor(fixUrl(defaultIframe), data, subtitleCallback, callback)
            }
            
            val serverButtons = doc.select("div.flex-1.flex-wrap button, button.btn, .descargas a")
            serverButtons.forEach { button ->
                val src = if (button.tagName() == "a") button.attr("href") else button.attr("data-src").ifBlank { button.attr("data-video") }
                if (!src.isNullOrBlank()) {
                    val fixedSrc = fixUrl(src)
                    if (fixedSrc.contains("redirect.php?id=")) {
                        val realUrl = fixedSrc.substringAfter("id=")
                        if (realUrl.startsWith("http")) {
                            loadExtractor(realUrl, data, subtitleCallback, callback)
                        }
                    } else if (fixedSrc.startsWith("http") && 
                        !fixedSrc.contains("google") && 
                        !fixedSrc.contains("facebook") &&
                        !fixedSrc.contains("mirror_direct")) {
                        loadExtractor(fixedSrc, data, subtitleCallback, callback)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }
}
