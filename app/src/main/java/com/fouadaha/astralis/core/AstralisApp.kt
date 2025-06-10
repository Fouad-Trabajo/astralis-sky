package com.fouadaha.astralis.core

import android.app.Application
import com.fouadaha.astralis.core.di.AppModule
import com.fouadaha.astralis.core.di.LocalModule
import com.fouadaha.astralis.core.di.RemoteModule
import com.fouadaha.astralis.features.celestialbodies.di.CelestialBodyModule
import com.fouadaha.astralis.features.skyorientation.di.SkyOrientationModule
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

class AstralisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        startKoin {
            androidContext(this@AstralisApp)
            modules(
                AppModule().module,
                RemoteModule().module,
                LocalModule().module,
                CelestialBodyModule().module,
                SkyOrientationModule().module
            )
        }
    }
}
