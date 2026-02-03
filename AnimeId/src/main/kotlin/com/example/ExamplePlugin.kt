package com.example

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class ExamplePlugin: Plugin() {
    override fun load(context: Context) {
        // Todos los providers (como AnimeId) deben registrarse aquí
        // para que la aplicación los pueda ver.
        registerMainAPI(AnimeId())
    }
}