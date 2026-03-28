package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
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
            val items = document.select("div.item, div.movie-item").mapNotNull {
                it.toSearchResult()
            }
            HomePageList(title, items)
        }

        return HomePageResponse(home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".title, h3, .name")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrl(this.selectFirst("img")?.attr("src") ?: "")

        val type = if (href.contains("/series/")) TvType.TvSeries else TvType.Movie

        return MovieSearchResponse(
            title,
            href,
            this@LaMovieProvider.name,
            type,
            posterUrl,
            null
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?search=$query").document
        return document.select("div.item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1, .movies-full__inside-title")?.text() ?: "Sin título"
        val poster = fixUrl(document.selectFirst("img")?.attr("src") ?: "")
        val description = document.selectFirst(".description, .storyline")?.text()
        
        val isSerie = url.contains("/series/")

        return if (isSerie) {
            val episodes = document.select(".episode-item, .episodes a").map {
                Episode(
                    fixUrl(it.attr("href")),
                    it.text()
                )
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
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
        val document = app.get(data).document
        
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (!src.isNullOrBlank()) {
                callback.invoke(
                    ExtractorLink(
                        "Reproductor",
                        "HD",
                        fixUrl(src),
                        mainUrl,
                        Qualities.Unknown.value,
                        src.contains(".m3u8")
                    )
                )
            }
        }
        return true
    }
}
