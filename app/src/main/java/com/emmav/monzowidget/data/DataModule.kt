package com.emmav.monzowidget.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.emmav.monzo.widget.data.db.MonzoStorage
import com.emmav.monzowidget.BuildConfig
import com.emmav.monzowidget.api.MonzoApi
import com.emmav.monzowidget.data.monzo.DbAccount
import com.emmav.monzowidget.data.monzo.DbBalance
import com.emmav.monzowidget.data.monzo.DbPot
import com.emmav.monzowidget.data.session.DbSession
import com.emmav.monzowidget.data.session.SessionStorage
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.IOException

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

    private fun OkHttpClient.createMonzoApi(baseUrl: String): MonzoApi {
        val json = Json {
            ignoreUnknownKeys = true
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .client(this)
            .build()
            .create(MonzoApi::class.java)
    }

    class MonzoAuthenticator(
        private val clientId: String = BuildConfig.MONZO_CLIENT_ID,
        private val clientSecret: String = BuildConfig.MONZO_CLIENT_SECRET,
        private val baseUrl: String,
        private val baseHttpClient: OkHttpClient,
        private val sessionStorage: SessionStorage,
    ) : Authenticator {

        override fun authenticate(route: Route?, response: Response): Request? {
            return synchronized(this) {
                // Make a new instance to avoid making another call with our expired access token
                runBlocking {
                    val monzoApi = baseHttpClient.createMonzoApi(baseUrl)
                    val session = sessionStorage.getSession()
                        ?: error("No session found for authentication")

                    val result = monzoApi.refreshAccessToken(
                        clientId = clientId,
                        clientSecret = clientSecret,
                        refreshToken = session.refreshToken,
                    )
                    try {
                        if (result.code() == 200) {
                            val tokenResponse = result.body()!!
                            sessionStorage.saveSession(
                                session = DbSession(
                                    accessToken = tokenResponse.accessToken,
                                    refreshToken = tokenResponse.refreshToken,
                                )
                            )

                            response.request.newBuilder()
                                .header("Authorization", "Bearer ${tokenResponse.accessToken}")
                                .build()
                        }
                    } catch (e: IOException) {
                        Log.e("ejv", "Exception whilst trying to refresh token", e)
                    }
                    null
                }
            }
        }
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