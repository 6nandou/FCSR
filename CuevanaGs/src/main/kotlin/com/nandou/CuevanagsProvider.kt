package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class CuevanaGsProvider : MainAPI() {
    override var mainUrl = "https://cuevana.gs"
    override var name = "Cuevana"
    override var lang = "es"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = ArrayList<HomePageList>()
        val sections = listOf(
            Pair(mainUrl, "Estrenos"),
            Pair("$mainUrl/peliculas", "Películas"),
            Pair("$mainUrl/series", "Series")
        )

        sections.forEach { (url, title) ->
            try {
                val doc = app.get(url).document
                val res = doc.select("div.group").mapNotNull { element ->
                    element.toSearchResult()
                }
                if (res.isNotEmpty()) {
                    items.add(HomePageList(title, res))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return newHomePageResponse(items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkElement = this.selectFirst("a.relative.block") ?: return null
        val href = fixUrl(linkElement.attr("href"))
        val title = this.selectFirst("h3")?.text()?.trim() ?: return null
        val posterUrl = fixUrl(this.selectFirst("img")?.attr("src") ?: "")
        
        val tvType = if (href.contains("/series/") || href.contains("/animes/")) {
            TvType.TvSeries
        } else {
            TvType.Movie
        }
        
        return newMovieSearchResponse(title, href, tvType) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=${query.replace(" ", "+")}"
        val doc = app.get(url).document
        return doc.select("div.group").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text()?.trim() ?: ""
        val poster = fixUrl(doc.selectFirst("img.rounded-lg")?.attr("src") ?: "")
        
        return if (url.contains("/series/") || url.contains("/animes/")) {
            val episodes = mutableListOf<Episode>()
            val episodeLinks = doc.select("a[href*=/episodio/]")
            
            episodeLinks.forEachIndexed { index, link ->
                val episodeUrl = fixUrl(link.attr("href"))
                val episodeNumber = index + 1
                
                episodes.add(newEpisode() {
                    this.data = episodeUrl
                    this.name = "Episodio $episodeNumber"
                    this.episode = episodeNumber
                })
            }
            
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        loadExtractor(data, data, subtitleCallback, callback)
        return true
    }
}
