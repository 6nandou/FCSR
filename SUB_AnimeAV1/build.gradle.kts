import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

cloudstream {
    authors = listOf("6nandou")
    language = "es"
    description = "Proveedor AnimeAV1"
    status = 1
    tvTypes = listOf(
        "Anime",
        "OVA",
        "AnimeMovie"
    )
    iconUrl = "http://www.google.com/s2/favicons?domain=https://animeav1.com/"
    version = 1
}
