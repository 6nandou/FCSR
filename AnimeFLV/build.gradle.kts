import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(plugin = "com.android.library")
apply(plugin = "kotlin-android")
apply(plugin = "com.lagradost.cloudstream3.gradle")

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

dependencies {
    val cloudstreamVersion = "3.0.0" // O la versión que use tu repositorio base
    implementation("com.lagradost:cloudstream3:$cloudstreamVersion")
    
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
    }
}