package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Element

class LaMovieProvider : MainAPI() {
    override var mainUrl = "https://la.movie"
    override var name = "La Movie"
    override var lang = "es"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val categories = listOf(
            Pair("$mainUrl/peliculas", "Películas"),
            Pair("$mainUrl/series", "Series")
        )

        val home = categories.map { (url, title) ->
            val document = app.get(url).document
            val items = document.select(".popular-card").mapNotNull {
                it.toSearchResult()
            }
            HomePageList(title, items)
        }

        return newHomePageResponse(home, false)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val titleElement = this.selectFirst(".popular-card__title h2 a")
        val title = titleElement?.selectFirst("span")?.text() 
            ?: titleElement?.selectFirst("p")?.text() 
            ?: return null
            
        val href = fixUrl(titleElement?.attr("href") ?: return null)
        val posterUrl = fixUrl(this.selectFirst(".popular-card__img img")?.attr("src") ?: "")

        val type = if (href.contains("/series/")) TvType.TvSeries else TvType.Movie

        return if (type == TvType.TvSeries) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search/${query.replace(" ", "+")}"
        val document = app.get(url).document
        return document.select(".popular-card").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        val title = document.selectFirst(".movies-full__inside-title h1, .popular-card__title h1")?.text() 
            ?: document.selectFirst("h1")?.text() 
            ?: "Sin título"
            
        val poster = fixUrl(document.selectFirst(".movies-full__img img, .popular-card__img img")?.attr("src") ?: "")
        val description = document.selectFirst(".description, .storyline, p.text-gray-400")?.text()
        
        val isSerie = url.contains("/series/")

        return if (isSerie) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, emptyList()) {
                this.posterUrl = poster
                this.plot = description
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return false 
    }
}
