import com.lagradost.cloudstream3.gradle.CloudstreamExtension

cloudstream {
    // Esto llenará correctamente el plugins.json
    authors = listOf("6nandou")
    language = "en"
    description = "Provider for AnimeIdHentai"
    
    // Forzamos que el nombre en la lista sea el correcto
    setMetadata(
        "name" to "AnimeIdHentai",
        "version" to 190,
        "tvTypes" to listOf("NSFW")
    )
}
