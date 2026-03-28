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
                var elements = doc.select("div.group")
                if (elements.isEmpty()) {
                    elements = doc.select("article")
                }
                if (elements.isEmpty()) {
                    elements = doc.select(".item")
                }
                if (elements.isEmpty()) {
                    elements = doc.select(".movie-item")
                }
                
                val res = elements.mapNotNull { element ->
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
        var linkElement = this.selectFirst("a.relative.block")
        if (linkElement == null) {
            linkElement = this.selectFirst("a[href*=/pelicula/]")
        }
        if (linkElement == null) {
            linkElement = this.selectFirst("a[href*=/serie/]")
        }
        if (linkElement == null) {
            linkElement = this.selectFirst("a")
        }
        
        val href = fixUrl(linkElement?.attr("href") ?: return null)
        
        var title = this.selectFirst("h3")?.text()?.trim()
        if (title.isNullOrEmpty()) {
            title = this.selectFirst("h2")?.text()?.trim()
        }
        if (title.isNullOrEmpty()) {
            title = this.selectFirst(".title")?.text()?.trim()
        }
        if (title.isNullOrEmpty()) {
            title = this.selectFirst("img")?.attr("alt")?.trim()
        }
        if (title.isNullOrEmpty()) return null
        
        var posterUrl = this.selectFirst("img")?.attr("src")
        if (posterUrl.isNullOrEmpty()) {
            posterUrl = this.selectFirst("img")?.attr("data-src")
        }
        if (posterUrl.isNullOrEmpty()) {
            posterUrl = this.selectFirst("img")?.attr("data-original")
        }
        if (posterUrl.isNullOrEmpty()) return null
        
        posterUrl = fixUrl(posterUrl)
        
        val tvType = if (href.contains("/series/") || href.contains("/anime/")) {
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
        var elements = doc.select("div.group")
        if (elements.isEmpty()) {
            elements = doc.select("article")
        }
        return elements.mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        
        var title = doc.selectFirst("h1")?.text()?.trim()
        if (title.isNullOrEmpty()) {
            title = doc.selectFirst(".title")?.text()?.trim()
        }
        if (title.isNullOrEmpty()) {
            title = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
        }
        title = title ?: ""
        
        var poster = doc.selectFirst("img.rounded-lg")?.attr("src")
        if (poster.isNullOrEmpty()) {
            poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
        }
        if (poster.isNullOrEmpty()) {
            poster = doc.selectFirst("img")?.attr("src")
        }
        poster = fixUrl(poster ?: "")
        
        val isSeries = url.contains("/series/") || url.contains("/anime/") || 
                       doc.select("a[href*=/episodio/]").isNotEmpty()
        
        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            var episodeLinks = doc.select("a[href*=/episodio/]")
            if (episodeLinks.isEmpty()) {
                episodeLinks = doc.select("a[href*=/capitulo/]")
            }
            if (episodeLinks.isEmpty()) {
                episodeLinks = doc.select(".episode-item a")
            }
            
            episodeLinks.forEachIndexed { index, link ->
                val episodeUrl = fixUrl(link.attr("href"))
                val episodeNumber = index + 1
                
                episodes.add(
                    Episode(
                        episodeUrl,
                        "Episodio $episodeNumber",
                        episodeNumber
                    )
                )
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
