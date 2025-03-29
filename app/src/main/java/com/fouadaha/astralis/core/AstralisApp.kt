package com.fouadaha.astralis.core

import android.app.Application
import com.fouadaha.astralis.core.di.AppModule
import com.fouadaha.astralis.core.di.LocalModule
import com.fouadaha.astralis.core.di.RemoteModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AstralisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AstralisApp)
            //modules(AppModule().module, RemoteModule().module, LocalModule().module)
        }
    }
}
