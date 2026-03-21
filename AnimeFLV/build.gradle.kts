import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = 1

cloudstream {
    authors = listOf("6nandou")
    language = "es"
    description = "Proveedor AnimeFLV"
    status = 1
    tvTypes = listOf(
        "Anime",
        "OVA",
        "AnimeMovie"
    )
    iconUrl = "http://www.google.com/s2/favicons?domain=https://www3.animeflv.net/"
}
