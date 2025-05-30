package com.emmav.monzowidget

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.emmav.monzowidget.api.MonzoApi
import com.emmav.monzowidget.db.OAuthSessionEntity
import com.emmav.monzowidget.db.SessionStorage
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object DataModule {
    fun create(baseUrl: String): MonzoApi {
        val json = Json {
            ignoreUnknownKeys = true
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .build()
            )
            .build()
            .create(MonzoApi::class.java)
    }

    fun createDb(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = AppDatabase::class.java,
            name = "app-database",
        ).build()
    }

    @Database(entities = [OAuthSessionEntity::class], version = 1)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun authStorage(): SessionStorage
    }
}