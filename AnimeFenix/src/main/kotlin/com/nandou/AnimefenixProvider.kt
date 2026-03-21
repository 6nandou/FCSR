package com.nandou

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink

class Animefenix : MainAPI() {
    override var mainUrl = "https://animefenix2.tv"
    override var name = "AnimeFenix"
    override val hasQuickSearch = true
    override val hasMainPage = true
    override val supportedTypes = setOf(
        TvType.AnimeMovie,
        TvType.OVA,
        TvType.Anime
    )
    
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse? {
        val list = mutableListOf<AnimeSearchResponse>()
        val url = if (page <= 1) "$mainUrl/${request.data}" else "$mainUrl/${request.data}&page=$page"
        val res = app.get(url).document
        
        res.select("article.anime").forEach { article ->
            val title = article.selectFirst("h3.title a")?.text() 
                ?: article.selectFirst("div.ttl")?.text() ?: ""
            val poster = article.selectFirst("img")?.attr("src")
            val href = article.selectFirst("a")?.attr("href") ?: ""
            if (href.isNotBlank()) {
                list.add(newAnimeSearchResponse(title, href) {
                    this.posterUrl = poster
                })
            }
        }
        
        return newHomePageResponse(
            listOf(HomePageList(
                name = request.name,
                list = list,
                isHorizontalImages = true
            )),
            hasNext = true
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/buscar?q=$query"
        return app.get(url).document.select("article.anime").mapNotNull { article ->
            val title = article.selectFirst("h3.title a")?.text() 
                ?: article.selectFirst("div.ttl")?.text() ?: ""
            val poster = article.selectFirst("img")?.attr("src")
            val href = article.selectFirst("a")?.attr("href") ?: ""
            newAnimeSearchResponse(title, href) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val result = app.get(url).document
        val background = result.selectFirst("div.backdrop")?.attr("style")
            ?.substringAfter("url('")?.substringBefore("')")
        val description = result.selectFirst("div.description")?.text()
        val title = result.selectFirst("h1.title")?.text() ?: result.selectFirst("h1.ttl")?.text() ?: ""

        val episodes = result.select("ul.episode-list li").mapNotNull {
            val epHref = it.selectFirst("a")?.attr("href") ?: ""
            val epNum = it.selectFirst("a")?.text()?.filter { char -> char.isDigit() }?.toIntOrNull() ?: 0
            Episode(epHref, "Episodio $epNum")
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.backgroundPosterUrl = background
            this.plot = description
            this.episodes = episodes
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val res = app.get(data).document
        
        val scripts = res.select("script")
        scripts.forEach { script ->
            val content = script.data()
            if (content.contains("var streams =")) {
            }
        }

        res.select("div.player-container iframe, div.embed iframe").forEach { iframe ->
            val src = iframe.attr("src")
            loadExtractor(src, data, subtitleCallback, callback)
        }

        return true
    }
}
