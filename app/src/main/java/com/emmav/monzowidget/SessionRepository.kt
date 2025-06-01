package com.emmav.monzowidget

import com.emmav.monzowidget.api.MonzoApi
import com.emmav.monzowidget.api.TokenResponse
import com.emmav.monzowidget.db.OAuthSessionEntity
import com.emmav.monzowidget.db.SessionStorage

class SessionRepository(
    private val api: MonzoApi,
    private val db: SessionStorage,
) {

    /**
     * In order to test the user has done Strong Customer Authentication (aka 2FA, and accepted an
     * in-app push notification sent to the official Monzo app). If they have not, we'll get a 403.
     *
     * SCA is used to protect sensitive information. We call /accounts as it's an endpoint that will
     * always require SCA. If we call something like /ping/whoami we'll get a 200 regardless of whether SCA
     * has been done or not, because that API doesn't need it.
     */
    suspend fun testAuthentication(): Result<Boolean> {
        return try {
            val response = api.accounts()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
}
