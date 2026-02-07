package com.fouadaha.astralis.core.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@ComponentScan
class RemoteModule {

    private val url = "https://api.le-systeme-solaire.net/rest/"

    @Single
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("dd/MM/yyyy")
            .create()
    }

    @Single
    fun provideLoggingInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    @Single
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        val okHttpClient = OkHttpClient
            .Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        return okHttpClient
    }

    @Single
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        val retrofit = Retrofit
            .Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        return retrofit
    }

    @Single
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}
