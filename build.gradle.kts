import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        // Versión de Gradle compatible con el entorno de GitHub Actions
        classpath("com.android.tools.build:gradle:8.7.3")
        // Plugin de Cloudstream para generar los archivos .cs3/.arw
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        // Plugin de Kotlin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10") 
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// Funciones de extensión para facilitar la configuración
fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) = 
    extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = 
    extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        // Configura el repositorio automáticamente para el despliegue
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "6nandou/FCSR")
    }

    android {
        // Namespace base para todos los plugins
        namespace = "com.example" 

        defaultConfig {
            minSdk = 21
            compileSdkVersion(35)
            targetSdk = 35
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        // Configuración del compilador Kotlin para JVM 17
        tasks.withType<KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.addAll(
                    "-Xno-call-assertions",
                    "-Xno-param-assertions",
                    "-Xno-receiver-assertions",
                    "-Xskip-metadata-version-check"
                )
            }
        }
    }

    dependencies {
        val cloudstream by configurations
        val implementation by configurations

        // CORRECCIÓN CLAVE: Usamos el grupo correcto para JitPack
        cloudstream("com.github.recloudstream:cloudstream:pre-release")

        implementation(kotlin("stdlib"))
        implementation("com.github.Blatzar:NiceHttp:0.4.11")
        implementation("org.jsoup:jsoup:1.18.3")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    }
}

// Tarea para limpiar el proyecto
task<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
