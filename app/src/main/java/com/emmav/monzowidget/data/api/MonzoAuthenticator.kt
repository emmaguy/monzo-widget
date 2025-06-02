package com.emmav.monzowidget.data.api

import android.util.Log
import com.emmav.monzowidget.BuildConfig
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
import retrofit2.Retrofit
import java.io.IOException

class MonzoAuthenticator(
    private val clientId: String = BuildConfig.MONZO_CLIENT_ID,
    private val clientSecret: String = BuildConfig.MONZO_CLIENT_SECRET,
    private val baseUrl: String,
    private val baseHttpClient: OkHttpClient,
    private val sessionStorage: SessionStorage,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        return synchronized(this) {
            runBlocking {
                // Make a new instance of monzoApi to avoid making another call with our expired access token
                val monzoApi = baseHttpClient.createMonzoApi(baseUrl)
                sessionStorage.getSession()?.let { session ->
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
                }
                null
            }
        }
    }
}

internal fun OkHttpClient.createMonzoApi(baseUrl: String): MonzoApi {
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