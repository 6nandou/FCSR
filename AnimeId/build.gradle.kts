import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import com.android.build.gradle.BaseExtension

// Aplicamos los plugins necesarios
apply(plugin = "com.android.library")
apply(plugin = "kotlin-android")
apply(plugin = "com.lagradost.cloudstream3.gradle")

// Configuración de Android para que sepa dónde está el código
configure<BaseExtension> {
    compileSdkVersion(33)

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }
}

// Tu configuración de Cloudstream mejorada
configure<CloudstreamExtension> {
    // Vincula el provider.json que ya tienes
    setMetadata(file("provider.json"))
    
    // Estos valores pueden ir aquí o en el provider.json
    authors = listOf("6nandou")
    language = "es"
    description = "Proveedor para AnimeIdHentai"
    status = 1
    tvTypes = listOf("NSFW")
}

dependencies {
    // Versión de la API de Cloudstream
    val cloudstreamVersion = "pre-release"
    implementation("com.github.recloudstream:cloudstream:$cloudstreamVersion")
    
    // Librerías base para que funcione el scraping (Jsoup)
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
}
