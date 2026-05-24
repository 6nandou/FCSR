package com.nandou

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class IronHPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(IronHProvider())
    }
}
