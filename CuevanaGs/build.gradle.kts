import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

cloudstream {
    authors = listOf("6nandou")
    language = "es"
    description = "Proveedor Cuevana.gs"
    status = 1
    tvTypes = listOf(
        "Movie",
        "TvSeries",
        "Anime"
    )
    iconUrl = "https://www.google.com/s2/favicons?domain=https://cuevana.gs/"
}
