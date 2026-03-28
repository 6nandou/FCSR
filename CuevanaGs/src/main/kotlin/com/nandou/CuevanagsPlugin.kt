package com.lagradost.cloudstream3.movieproviders

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class CuevanaGsPlugin: CloudstreamPlugin() {
    override fun load(context: Context) {
        registerMainAPI(CuevanaGsProvider())
    }
}
