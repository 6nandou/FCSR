package com.nandou

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class AnimefenixProviderPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(AnimefenixProvider())
    }
}
