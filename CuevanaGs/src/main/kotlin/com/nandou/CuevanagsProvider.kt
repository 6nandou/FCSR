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
        val categories = listOf(
            Pair("$mainUrl/peliculas", "Películas"),
            Pair("$mainUrl/series", "Series"),
            Pair("$mainUrl/animes", "Animes")
        )
        
        val items = categories.mapNotNull { (url, title) ->
            val doc = app.get(url).document
            val cards = doc.select("div.relative.group, article, .item, .post").mapNotNull {
                it.toSearchResult()
            }
            if (cards.isNotEmpty()) HomePageList(title, cards) else null
        }

        return newHomePageResponse(items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val a = this.selectFirst("a[href*='/pelicula/'], a[href*='/series/'], a[href*='/animes/']") ?: return null
        val href = fixUrl(a.attr("href"))
        
        val title = this.selectFirst("h3, h2, .title")?.text()?.trim() 
            ?: this.selectFirst("img")?.attr("alt")?.trim() 
            ?: return null
            
        val posterUrl = fixUrl(this.selectFirst("img")?.attr("src") ?: "")

        return newMovieSearchResponse(title, href, TvType.Movie) { 
            this.posterUrl = posterUrl 
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=${query.replace(" ", "+")}").document
        return doc.select("div.relative.group, article, .item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text()?.trim() ?: "Sin título"
        val poster = fixUrl(doc.selectFirst("img.rounded-lg, .poster img")?.attr("src") ?: "")
        
        return newMovieLoadResponse(title, url, TvType.Movie, url) { 
            this.posterUrl = poster 
        }
    }
}
