import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import com.android.build.gradle.BaseExtension

apply(plugin = "com.android.library")
apply(plugin = "kotlin-android")
apply(plugin = "com.lagradost.cloudstream3.gradle")

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

// Configuración simplificada sin 'setMetadata'
configure<CloudstreamExtension> {
    // Al no poner setMetadata, el plugin busca 'provider.json' 
    // automáticamente en la carpeta del proyecto.
    authors = listOf("6nandou")
    language = "en"
    description = "Proveedor para AnimeIdHentai"
}

dependencies {
    val cloudstreamVersion = "pre-release"
    implementation("com.github.recloudstream:cloudstream:$cloudstreamVersion")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
}
