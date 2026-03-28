package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class CuevanaProvider : MainAPI() {
    override var mainUrl = "https://cuevana.gs"
    override var name = "Cuevana"
    override var lang = "es"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    override val mainPage = mainPageOf(
        "peliculas" to "Películas",
        "series" to "Series de TV",
        "animes" to "Animes"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) "$mainUrl/${request.data}/" else "$mainUrl/${request.data}/page/$page/"
        val document = app.get(url).document
        val home = document.select("article.item, div.item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home, true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".title h2, .entry-title, h3")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrl(this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src") ?: "")

        return if (href.contains("/series/") || href.contains("/animes/")) {
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
        val url = "$mainUrl/search/$query"
        val document = app.get(url).document
        return document.select("article.item, .result-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: ""
        val poster = fixUrl(document.selectFirst(".poster img")?.attr("src") ?: "")
        val plot = document.selectFirst(".description p, .entry-content p")?.text()
        val year = document.selectFirst(".year")?.text()?.filter { it.isDigit() }?.toIntOrNull()

        return if (url.contains("/series/") || url.contains("/animes/")) {
            val episodes = document.select("ul.episodios li, .list-episodes li").mapNotNull {
                val epHref = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val name = it.selectFirst(".episodiotitle a")?.text() ?: it.text()
                newEpisode(epHref) {
                    this.name = name
                }
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plot
                this.year = year
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = plot
                this.year = year
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
        document.select("iframe, .video-player iframe").forEach {
            val src = fixUrl(it.attr("src").ifEmpty { it.attr("data-src") })
            if (src.isNotEmpty() && !src.contains("google") && !src.contains("youtube")) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }
        return true
    }
}
