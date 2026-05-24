package com.nandou

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class Sub_Animeav1Plugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Animeav1Provider())
    }
}
