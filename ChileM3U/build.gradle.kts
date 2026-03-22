import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

cloudstream {
    authors = listOf("6nandou")
    language = "es"
    description = "Proveedor Chile M3U"
    status = 1
    tvTypes = listOf(
          "Live"
    )
    iconUrl = "http://www.google.com/s2/favicons?domain=https://m3u.cl/"
}
