rootProject.name = "CloudstreamPlugins"

val disabled = listOf(".github", ".git", "gradle", "build", "builds")

File(rootDir, ".").eachDir { dir ->
    // Si la carpeta no está en la lista negra y tiene un build.gradle.kts, inclúyela
    if (!disabled.contains(dir.name) && File(dir, "build.gradle.kts").exists()) {
        include(dir.name)
        // Esto le dice a Gradle: "El proyecto :AnimeId está en la carpeta /AnimeId"
        project(":${dir.name}").projectDir = dir
    }
}

fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}
