package com.trail2

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@HiltAndroidApp
class App : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPKIT_KEY)
        MapKitFactory.initialize(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val logging = HttpLoggingInterceptor { msg ->
            android.util.Log.d("CoilOkHttp", msg)
        }.apply { level = HttpLoggingInterceptor.Level.BASIC }

        val coilOkHttp = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "TrailSocial/1.0 (Android; https://github.com/trail2)")
                        .build()
                )
            }
            .build()

        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { coilOkHttp }))
            }
            .build()
    }
}