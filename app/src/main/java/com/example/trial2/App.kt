package com.example.trial2

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("BuildConfig.YANDEX_MAPKIT_KEY")
        MapKitFactory.initialize(this)
    }
}