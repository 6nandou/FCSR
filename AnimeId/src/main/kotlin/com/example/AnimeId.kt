package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.base64ToText

class AnimeId : MainAPI() {
    override var mainUrl = "https://animeidhentai.com"
    override var name = "AnimeIdHentai"
    override val hasQuickSearch = false
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "trending" to "Trending Hentai",
        "genre/hentai-uncensored" to "Uncensored Hentai",
        "genre/censored" to "Censored Hentai",
        "genre/incest" to "Incest Hentai",
        "genre/hd" to "HD Hentai",
        "genre/maid" to "Maid Hentai",
        "genre/school-girl" to "Schoolgirl Hentai",
        "genre/virgin" to "Virgin Hentai",
        "genre/anal" to "Anal Hentai",
        "genre/nudity" to "Nudity Hentai",
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
            listOf(HomePageList(request.name, list, isHorizontalImages = true)),
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
        
        // 1. Decodificar el servidor propio (NHPlayer) mediante Base64
        // Este método salta anuncios y obtiene el link directo con token
        res.select("li[data-id*=/player.php?vid=]").forEach { element ->
            val rawId = element.attr("data-id")
            val base64Part = rawId.substringAfter("vid=").substringBefore("&")
            
            try {
                // Decodificamos y limpiamos la URL (quitamos el timestamp tras el '|')
                val decodedUrl = base64ToText(base64Part).substringBefore("|")
                
                if (decodedUrl.startsWith("http")) {
                    callback.invoke(
                        newExtractorLink(
                            this.name,
                            "Directo (NH)",
                            decodedUrl
                        ).apply {
                            this.referer = "https://nhplayer.com/"
                        }
                    )
                }
            } catch (e: Exception) {
                // En caso de error en decodificación, no rompemos el proceso
            }
        }

        // 2. Extraer servidores externos (Doodstream, Voe, etc.)
        res.select("div.embed iframe, div.servers iframe").forEach { element ->
            val src = element.attr("src")
            if (src.isNotEmpty() && !src.contains("nhplayer")) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
