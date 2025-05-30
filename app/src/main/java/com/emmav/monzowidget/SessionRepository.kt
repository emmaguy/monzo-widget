package com.emmav.monzowidget

import com.emmav.monzowidget.api.MonzoApi
import com.emmav.monzowidget.api.TokenResponse
import com.emmav.monzowidget.db.OAuthSessionEntity
import com.emmav.monzowidget.db.SessionStorage

class SessionRepository(
    private val api: MonzoApi,
    private val db: SessionStorage,
) {

    suspend fun exchangeCodeForToken(
        clientId: String,
        clientSecret: String,
        authorizationCode: String,
        redirectUri: String
    ): Result<TokenResponse> {
        return try {
            val response = api.getAccessToken(
                clientId = clientId,
                clientSecret = clientSecret,
                authorizationCode = authorizationCode,
                redirectUri = redirectUri
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    db.saveSession(
                        session = OAuthSessionEntity(
                            accessToken = it.accessToken,
                            refreshToken = it.refreshToken,
                        )
                    )
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSession(): OAuthSessionEntity? {
        return db.getSession()
    }

    suspend fun refreshToken(
        clientId: String,
        clientSecret: String,
        refreshToken: String
    ): Result<TokenResponse> {
        return try {
            val response = api.refreshAccessToken(
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = refreshToken
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
