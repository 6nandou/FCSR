package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink

class AnimeId : MainAPI() {
    override var mainUrl = "https://animeidhentai.com"
    override var name = "AnimeIdHentai"
    override val hasQuickSearch = false
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "trending" to "Trending Hentai",
        "genre/censored" to "Censored Hentai",
        "genre/hentai-uncensored" to "Uncensored Hentai",
        "genre/incest" to "Incest Hentai",
        "genre/hd" to "HD Hentai",
        "genre/maid" to "Maid Hentai",
        "genre/monster" to "Monster Hentai",
        "genre/female-student" to "Female Student Hentai",
        "genre/tentacle" to "Tentacle Hentai",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse? {
        val list = mutableListOf<AnimeSearchResponse>()
        val pagedUrl = if (page <= 1) "$mainUrl/${request.data}/" else "$mainUrl/${request.data}/page/$page/"
        val res = app.get(pagedUrl).document
        
        res.select("article.anime.poster.por").mapNotNull { article ->
            val title = article.selectFirst("header > div.ttl")?.text() ?: ""
            val poster = article.selectFirst("img")?.attr("src")
            val url = article.selectFirst("a.lnk-blk")?.attr("href") ?: ""
            list.add(newAnimeSearchResponse(title, url) {
                this.posterUrl = poster
            })
        }
        
        return newHomePageResponse(
            listOf(
                HomePageList(
                    request.name,
                    list,
                    isHorizontalImages = true
                )
            ),
            hasNext = true
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=${query}"
        return app.get(url).document.select("article.anime.poster.por").mapNotNull { article ->
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
        val description = result.selectFirst("div.description > p")?.text()
        val title = result.selectFirst("header.anime-hd h1.ttl")?.text() ?: ""

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.plot = description
            this.posterUrl = result.selectFirst("meta[property=og:image]")?.attr("content")
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val res = app.get(data).document
        
        // 1. Extraer de servidores externos conocidos
        res.select("div.embed iframe, div.servers iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotEmpty()) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }

        // 2. Extraer link directo del reproductor de la web
        res.select("video source").forEach { source ->
            val videoUrl = source.attr("src")
            if (videoUrl.isNotEmpty()) {
                // Usamos newExtractorLink con un bloque de inicialización
                // Esto evita el warning de 'deprecated' y el error de 'val reassigned'
                callback.invoke(
                    newExtractorLink(
                        source = this.name,
                        name = "Directo",
                        url = videoUrl
                    )
                )
            }
        }

        return true
    }
}
