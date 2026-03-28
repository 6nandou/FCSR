package com.nandou

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class CuevanagsPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CuevanaGsProvider())
    }
}
