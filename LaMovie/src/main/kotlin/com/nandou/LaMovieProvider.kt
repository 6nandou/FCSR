package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class LaMovieProvider : MainAPI() {
    override var mainUrl = "https://la.movie"
    override var name = "La Movie"
    override var lang = "es"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    private val apiPrefix = "$mainUrl/wp-json/wpf/v1"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = mutableListOf<HomePageList>()
        val categories = listOf(
            Pair("movies", "Películas"),
            Pair("tvshows", "Series"),
            Pair("animes", "Animes")
        )

        categories.forEach { (slug, title) ->
            try {
                val response = app.get("$apiPrefix/posts?type=$slug&page=$page&limit=20", headers = mapOf("User-Agent" to userAgent)).text
                val data = tryParseJson<List<LaMovieItem>>(response)
                
                val searchResults = data?.map { item ->
                    if (slug == "movies") {
                        newMovieSearchResponse(item.title ?: "", item.link ?: "", TvType.Movie) {
                            this.posterUrl = item.poster
                        }
                    } else {
                        newTvSeriesSearchResponse(item.title ?: "", item.link ?: "", TvType.TvSeries) {
                            this.posterUrl = item.poster
                        }
                    }
                } ?: emptyList()
                
                if (searchResults.isNotEmpty()) {
                    items.add(HomePageList(title, searchResults))
                }
            } catch (e: Exception) { }
        }

        if (items.isEmpty()) {
            val doc = app.get(mainUrl, headers = mapOf("User-Agent" to userAgent)).document
            val fallback = doc.select(".popular-card, article").mapNotNull { it.toSearchResult() }
            if (fallback.isNotEmpty()) items.add(HomePageList("Tendencias", fallback))
        }

        return newHomePageResponse(items, false)
    }

    data class LaMovieItem(
        val title: String? = null,
        val link: String? = null,
        val poster: String? = null
    )

    private fun Element.toSearchResult(): SearchResponse? {
        val link = this.selectFirst("a") ?: return null
        val href = fixUrl(link.attr("href"))
        val title = this.selectFirst("h2, h3, .title, .popular-card__title")?.text() ?: return null
        val posterUrl = fixUrl(this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src") ?: "")

        return if (href.contains("/series/") || href.contains("/animes/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return try {
            val response = app.get("$apiPrefix/posts?search=$query", headers = mapOf("User-Agent" to userAgent)).text
            val data = tryParseJson<List<LaMovieItem>>(response)
            data?.map { item ->
                val isSerie = item.link?.contains("/series/") == true || item.link?.contains("/animes/") == true
                if (isSerie) {
                    newTvSeriesSearchResponse(item.title ?: "", item.link ?: "", TvType.TvSeries) { this.posterUrl = item.poster }
                } else {
                    newMovieSearchResponse(item.title ?: "", item.link ?: "", TvType.Movie) { this.posterUrl = item.poster }
                }
            } ?: emptyList()
        } catch (e: Exception) {
            val cleanQuery = query.trim().replace(" ", "+")
            val doc = app.get("$mainUrl/search/$cleanQuery", headers = mapOf("User-Agent" to userAgent)).document
            doc.select(".popular-card, article").mapNotNull { it.toSearchResult() }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, headers = mapOf("User-Agent" to userAgent)).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: "Sin título"
        val poster = fixUrl(document.selectFirst("img.wp-post-image, .poster img, .popular-card__img img")?.attr("src") ?: "")
        val plot = document.selectFirst(".description, .entry-content p, .storyline")?.text()
        val year = document.selectFirst(".year, a[href*='fecha-de-estreno']")?.text()?.filter { it.isDigit() }?.toIntOrNull()

        return if (url.contains("/series/") || url.contains("/animes/")) {
            val episodes = document.select(".episodios li, .episode-item, .aa-eps-list li").mapNotNull {
                val epLink = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val epName = it.text().trim()
                newEpisode(epLink) { this.name = epName }
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
        val document = app.get(data, headers = mapOf("User-Agent" to userAgent)).document
        document.select("iframe, .video-player iframe, source").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
            val finalSrc = fixUrl(src)
            if (finalSrc.isNotEmpty() && !finalSrc.contains("google") && !finalSrc.contains("youtube")) {
                loadExtractor(finalSrc, data, subtitleCallback, callback)
            }
        }
        return true
    }
}
