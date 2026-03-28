package com.nandou

import com.lagradost.cloudstream3.*
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

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = mutableListOf<HomePageList>()
        val categories = listOf(
            Pair("peliculas", "Películas"),
            Pair("series", "Series"),
            Pair("animes", "Animes")
        )

        categories.forEach { (slug, title) ->
            try {
                val url = if (page <= 1) "$mainUrl/$slug/" else "$mainUrl/$slug/page/$page/"
                val doc = app.get(url).document
                val res = doc.select("article, .popular-card, .item").mapNotNull {
                    it.toSearchResult()
                }
                if (res.isNotEmpty()) {
                    items.add(HomePageList(title, res))
                }
            } catch (e: Exception) { }
        }

        return newHomePageResponse(items, false)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val link = this.selectFirst("a") ?: return null
        val href = fixUrl(link.attr("href"))
        val title = this.selectFirst("h2, h3, .title, .popular-card__title")?.text() 
            ?: this.selectFirst("img")?.attr("alt")
            ?: return null

        val posterUrl = fixUrl(
            this.selectFirst("img")?.attr("data-src") 
            ?: this.selectFirst("img")?.attr("src") 
            ?: ""
        )

        return when {
            href.contains("/series/") -> {
                newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                    this.posterUrl = posterUrl
                }
            }
            href.contains("/animes/") -> {
                newTvSeriesSearchResponse(title, href, TvType.Anime) {
                    this.posterUrl = posterUrl
                }
            }
            else -> {
                newMovieSearchResponse(title, href, TvType.Movie) {
                    this.posterUrl = posterUrl
                }
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        return document.select("article, .popular-card, .item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: "Sin título"
        val poster = fixUrl(document.selectFirst("img.wp-post-image, .poster img, .popular-card__img img")?.attr("src") ?: "")
        val plot = document.selectFirst(".description, .entry-content p, .storyline, .movies-full__inside-main p")?.text()
        val year = document.selectFirst(".year, a[href*='fecha-de-estreno']")?.text()?.filter { it.isDigit() }?.toIntOrNull()

        return if (url.contains("/series/") || url.contains("/animes/")) {
            val episodes = document.select(".episodios li, .episode-item, .aa-eps-list li").mapNotNull {
                val epLink = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val epName = it.text().trim()
                newEpisode(epLink) {
                    this.name = epName
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
