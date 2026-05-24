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
        val titleElement = this.selectFirst("h3, .title, p.title, a > p, p")
        var titleText = titleElement?.text()?.trim() ?: ""
        
        if (titleText.isBlank() || titleText.equals("backdrop", ignoreCase = true)) {
            titleText = this.selectFirst("img")?.attr("alt")?.trim() ?: ""
        }
        
        if (titleText.isBlank() || titleText.equals("backdrop", ignoreCase = true)) {
            return null
        }
        
        val href = this.selectFirst("a")?.attr("href") ?: this.attr("href") ?: return null
        val poster = this.selectFirst("img")?.attr("src") ?: return null

        return newAnimeSearchResponse(titleText, fixUrl(href)) {
            this.posterUrl = fixUrl(poster)
        }
    }
    
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = ArrayList<HomePageList>()
        
        if (request.data.isBlank()) {
            try {
                val doc = app.get(mainUrl).document
                val latestEpisodes = doc.select("main div.grid article, section div.grid article, .latest-episodes a").mapNotNull { 
                    it.toSearchResult() 
                }
                if (latestEpisodes.isNotEmpty()) {
                    items.add(HomePageList("Últimos Episodios", latestEpisodes, true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                val url = if (page <= 1) "$mainUrl/${request.data}" else "$mainUrl/${request.data}&page=$page"
                val doc = app.get(url).document
                val categoryItems = doc.select("div.grid article, article").mapNotNull { 
                    it.toSearchResult() 
                }
                if (categoryItems.isNotEmpty()) {
                    items.add(HomePageList(request.name, categoryItems, false))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return newHomePageResponse(items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResults = mutableListOf<SearchResponse>()
        try {
            val doc = app.get("$mainUrl/catalogo?search=$query").document
            doc.select("div.grid article, article").forEach { element ->
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
        
        val slug = url.trimEnd('/').substringAfterLast("/")
        
        var totalEpisodesDetected = 0
        val countRegex = Regex(""""episodes_count"\s*:\s*(\d+)""")
        
        doc.select("script").forEach { script ->
            val scriptData = script.data()
            val match = countRegex.find(scriptData)
            if (match != null) {
                totalEpisodesDetected = match.groupValues[1].toIntOrNull() ?: 0
            }
        }

        if (totalEpisodesDetected > 0) {
            for (i in 1..totalEpisodesDetected) {
                episodes.add(
                    newEpisode("$mainUrl/ver/$slug-$i") {
                        this.episode = i
                        this.name = "Episodio $i"
                    }
                )
            }
        } else {
            val epRegex = Regex("""/media/[^\s"'\\]+""")
            doc.select("script").forEach { script ->
                val scriptData = script.data()
                if (scriptData.contains("/media/")) {
                    epRegex.findAll(scriptData).forEach { match ->
                        val epUrl = match.value
                        val epNum = epUrl.trimEnd('/').substringAfterLast("/").toIntOrNull()
                        if (epNum != null) {
                            val isDuplicate = episodes.any { it.episode == epNum }
                            if (!isDuplicate) {
                                episodes.add(
                                    newEpisode(fixUrl(epUrl)) {
                                        this.episode = epNum
                                        this.name = "Episodio $epNum"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (episodes.isEmpty()) {
                doc.select("a[href*=/media/], ul#eps li > a").forEach { epLink ->
                    val epUrl = epLink.attr("href")
                    val epNum = epUrl.trimEnd('/').substringAfterLast("/").toIntOrNull()
                    if (epNum != null) {
                        val isDuplicate = episodes.any { it.episode == epNum }
                        if (!isDuplicate) {
                            episodes.add(
                                newEpisode(fixUrl(epUrl)) {
                                    this.episode = epNum
                                    this.name = "Episodio $epNum"
                                }
                            )
                        }
                    }
                }
            }
        }
        
        val sortedEpisodes = episodes.distinctBy { it.episode }.sortedBy { it.episode }
        
        if (sortedEpisodes.isEmpty()) {
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
            var foundLinks = false
            
            val urlRegex = Regex("""https?://(?:www\.)?(?:pixeldrain\.com/u/|mega\.nz/file/|mp4upload\.com/|1fichier\.com/\?[a-zA-Z0-9]+)[^\s"']*""")
            
            doc.select("script").forEach { script ->
                val scriptData = script.data()
                if (scriptData.contains("pixeldrain") || scriptData.contains("mega.nz") || scriptData.contains("mp4upload")) {
                    val matches = urlRegex.findAll(scriptData)
                    matches.forEach { match ->
                        val url = match.value
                        
                        val isDub = scriptData.substring(
                            maxOf(0, scriptData.indexOf(url) - 150), 
                            scriptData.indexOf(url)
                        ).contains("DUB", ignoreCase = true)
                        
                        if (!isDub) {
                            loadExtractor(url, data, subtitleCallback, callback)
                            foundLinks = true
                        }
                    }
                }
            }
            
            if (!foundLinks) {
                val defaultIframe = doc.selectFirst("iframe[title='Episodio Embebido'], iframe.aspect-video, #iframe-element, .video-player iframe")?.attr("src")
                if (!defaultIframe.isNullOrBlank()) {
                    loadExtractor(fixUrl(defaultIframe), data, subtitleCallback, callback)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }
}
