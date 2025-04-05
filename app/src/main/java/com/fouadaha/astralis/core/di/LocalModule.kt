package com.fouadaha.astralis.core.di

import android.content.Context
import androidx.room.Room
import com.fouadaha.astralis.core.data.local.AstralisDataBase
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan
class LocalModule {

    @Single
    fun provideDatabase(context: Context): AstralisDataBase {
        val db = Room.databaseBuilder(
            context,
            AstralisDataBase::class.java, "astralis-db"
        )
        db.fallbackToDestructiveMigration()
        return db.build()

    }


}