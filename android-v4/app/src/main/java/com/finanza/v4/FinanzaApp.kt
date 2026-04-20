package com.finanza.v4

import android.app.Application
import com.finanza.v4.core.AppContainer

class FinanzaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
