package com.nandou

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.nodes.Element

class CuevanaGsProvider : MainAPI() {
    override var mainUrl = "https://cuevana.gs"
    override var name = "Cuevana"
    override var lang = "es"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = ArrayList<HomePageList>()
        val urls = listOf(
            Pair("$mainUrl/peliculas", "Películas"),
            Pair("$mainUrl/series", "Series"),
            Pair("$mainUrl/animes", "Animes"),
        )
        
        urls.forEach { (url, name) ->
            val soup = app.get(url).document
            val home = soup.select("div.relative.group").mapNotNull {
                it.toSearchResult()
            }
            items.add(HomePageList(name, home))
        }

        return newHomePageResponse(items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkElement = this.selectFirst("a.relative.block") ?: return null
        val href = fixUrl(linkElement.attr("href"))
        val title = this.selectFirst("h3")?.text()?.trim() ?: return null
        val posterUrl = fixUrl(this.selectFirst("img")?.attr("src") ?: "")

        val type = if (href.contains("/series/") || href.contains("/animes/")) TvType.TvSeries else TvType.Movie

        return if (type == TvType.Movie) {
            newMovieSearchResponse(title, href, type) { this.posterUrl = posterUrl }
        } else {
            newTvSeriesSearchResponse(title, href, type) { this.posterUrl = posterUrl }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=${query.replace(" ", "+")}"
        val document = app.get(url).document
        return document.select("div.relative.group").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val soup = app.get(url).document
        val title = soup.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrl(soup.selectFirst("img.rounded-lg")?.attr("src") ?: "")
        
        return if (url.contains("/series/") || url.contains("/animes/")) {
            val episodes = soup.select("a[href*='/episodio/']").map {
                newEpisode(fixUrl(it.attr("href"))) { this.name = it.text().trim() }
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) { this.posterUrl = poster }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) { this.posterUrl = poster }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("iframe").forEach {
            val src = fixUrl(it.attr("src"))
            loadSourceNameExtractor("Cuevana", src, mainUrl, subtitleCallback, callback)
        }
        return true
    }
}

suspend fun loadSourceNameExtractor(
    source: String,
    url: String,
    referer: String,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
) {
    loadExtractor(url, referer, subtitleCallback) { link ->
        CoroutineScope(Dispatchers.IO).launch {
            callback.invoke(
                newExtractorLink(
                    source,
                    source,
                    link.url,
                    referer,
                    link.quality,
                    link.type,
                    link.headers,
                    link.extractorData
                )
            )
        }
    }
}
