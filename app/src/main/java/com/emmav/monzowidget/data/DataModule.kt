package com.emmav.monzowidget.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.emmav.monzo.widget.data.db.MonzoStorage
import com.emmav.monzowidget.data.api.MonzoApi
import com.emmav.monzowidget.data.api.MonzoAuthenticator
import com.emmav.monzowidget.data.api.createMonzoApi
import com.emmav.monzowidget.data.monzo.DbAccount
import com.emmav.monzowidget.data.monzo.DbBalance
import com.emmav.monzowidget.data.monzo.DbPot
import com.emmav.monzowidget.data.session.DbSession
import com.emmav.monzowidget.data.session.SessionStorage
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object DataModule {

    fun create(context: Context, sessionStorage: SessionStorage, baseUrl: String): MonzoApi {
        val baseHttpClient by lazy {
            OkHttpClient.Builder().build()
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-type", "application/json")
                    .also {
                        runBlocking {
                            val session = sessionStorage.getSession()
                            if (session != null) {
                                it.header("Authorization", "Bearer ${session.accessToken}")
                            }
                        }
                    }

                chain.proceed(requestBuilder.build())
            }
            .authenticator(
                MonzoAuthenticator(
                    baseUrl = baseUrl,
                    baseHttpClient = baseHttpClient,
                    sessionStorage = sessionStorage,
                )
            )
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor(ChuckerInterceptor(context))
            .build()
            .createMonzoApi(baseUrl)
    }

    fun createDb(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = AppDatabase::class.java,
            name = "app-database",
        ).fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    @Database(
        entities = [
            // Session
            DbSession::class,
            // Monzo API entities
            DbAccount::class,
            DbBalance::class,
            DbPot::class,
        ], version = 3
    )
    abstract class AppDatabase : RoomDatabase() {
        abstract fun authStorage(): SessionStorage
        abstract fun monzoStorage(): MonzoStorage
    }
}