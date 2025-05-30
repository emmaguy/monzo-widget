package com.emmav.monzowidget

import android.content.Context
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class LoginViewModel(
    private val sessionRepository: SessionRepository,
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String,
) : ViewModel() {
    private val stateToken = "state_token" // TODO random

    fun onStartAuth(context: Context) {
        // 1. Redirect the user to Monzo to authorise your app
        //  https://docs.monzo.com/#acquire-an-access-token
        val url =
            "https://auth.monzo.com/?client_id=$clientId&redirect_uri=$redirectUri&response_type=code&state=$stateToken"
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, url.toUri())
    }

    fun exchangeCodeForToken(code: String, state: String?) {
        // 2. Monzo redirects the user back to your app with an authorization code (via [MainActivity.onCreate()])
        if (state != stateToken) error("state mismatch")

        viewModelScope.launch {
            // 3. Exchange the authorization code for an access token.
            sessionRepository.exchangeCodeForToken(
                clientId = clientId,
                clientSecret = clientSecret,
                authorizationCode = code,
                redirectUri = redirectUri,
            ).onSuccess {
                Log.e("ejv", "on success")
            }.onFailure {
                Log.e("ejv", "on failure $it")
            }
        }
    }
}
