package com.nandou

import com.lagradost.cloudstream3.*
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
            Pair("$mainUrl", "Estrenos"),
            Pair("$mainUrl/peliculas", "Películas"),
            Pair("$mainUrl/series", "Series")
        )

        sections.forEach { (url, title) ->
            try {
                val doc = app.get(url).document
                val res = doc.select("div.group").mapNotNull {
                    it.toSearchResult()
                }
                if (res.isNotEmpty()) {
                    items.add(HomePageList(title, res))
                }
            } catch (e: Exception) { }
        }

        return newHomePageResponse(items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val a = this.selectFirst("a.relative.block") ?: return null
        val href = fixUrl(a.attr("href"))
        val title = this.selectFirst("h3")?.text()?.trim() ?: return null
        val posterUrl = fixUrl(this.selectFirst("img")?.attr("src") ?: "")

        return newMovieSearchResponse(title, href, TvType.Movie) { 
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
        val title = doc.selectFirst("h1")?.text()?.trim() ?: "Cuevana"
        val poster = fixUrl(doc.selectFirst("img.rounded-lg")?.attr("src") ?: "")
        
        return newMovieLoadResponse(title, url, TvType.Movie, url) { 
            this.posterUrl = poster 
        }
    }
}
