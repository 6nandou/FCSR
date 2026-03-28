import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

cloudstream {
    authors = listOf("6nandou")
    language = "es"
    description = "Proveedor La Movie"
    status = 1
    tvTypes = listOf(
        "Movie",
        "TvSeries",
        "Anime"
    )
    iconUrl = "http://www.google.com/s2/favicons?domain=https://la.movie/"
}
