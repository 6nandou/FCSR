package com.nandou

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.ExtractorLink
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

    override val mainPage =
        mainPageOf(
            "directorio/anime?estado=2" to "En Emision",
            "directorio/anime?genero=1" to "Accion",
            "directorio/anime?genero=3" to "Romance",
        )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse? {
        val list = mutableListOf<AnimeSearchResponse>()
        val url = if (page <= 1) "$mainUrl/${request.data}" else "$mainUrl/${request.data}&page=$page"
        val res = app.get(url).document
        
        res.select("article.anime").forEach { article ->
            val title = article.selectFirst("header > div.ttl")?.text() ?: ""
            val poster = article.selectFirst("img")?.attr("src")
            val href = article.selectFirst("a.lnk-blk")?.attr("href") ?: ""
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
        val url = "$mainUrl/?s=$query"
        return app.get(url).document.select("article.anime").mapNotNull { article ->
            val title = article.selectFirst("header > div.ttl")?.text() ?: ""
            val poster = article.selectFirst("img")?.attr("src")
            val href = article.selectFirst("a.lnk-blk")?.attr("href") ?: ""
            newAnimeSearchResponse(title, href) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val result = app.get(url).document
        val background = result.selectFirst("div.backdrop")?.attr("style")
            ?.substringAfter("url('")?.substringBefore("')")
        val description = result.selectFirst("div.description > p")?.text()
        val title = result.selectFirst("header.anime-hd h1.ttl")?.text() ?: ""

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.backgroundPosterUrl = background ?: result.selectFirst("meta[property=og:image]")?.attr("content")
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        try {
            val res = app.get(data).document
            val iframe = res.selectFirst("div.embed > iframe")?.attr("src") ?: ""
            val playerurl = extractplayer(iframe) ?: return false
            val sourceurl = extractsource(playerurl) ?: ""
            val subtitle = extractsubtitles(playerurl) ?: ""
            
            if (sourceurl.isNotBlank()) {
                callback.invoke(
                    newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = sourceurl,
                    )
                )
            }
            if (subtitle.isNotBlank()) {
                subtitleCallback.invoke(SubtitleFile("spa", subtitle))
            }
        } catch (e: Exception) {
            logError(e)
        }
        return true
    }

    suspend fun extractplayer(url: String): String? {
        return app.get(url).document.selectFirst("div.servers li")?.attr("data-id")
    }

    suspend fun extractsource(url: String): String? {
        val script = app.get("https://nhplayer.com/$url").document.select("script:containsData(sources)").toString()
        return script.substringAfter("file: \"").substringBefore("\",")
    }

    suspend fun extractsubtitles(url: String): String? {
        val script = app.get("https://nhplayer.com/$url").document.select("script:containsData(sources)").toString()
        val pattern = "\"file\":.\"(.*)\",".toRegex()
        return pattern.find(script)?.groups?.get(1)?.value
    }
}
