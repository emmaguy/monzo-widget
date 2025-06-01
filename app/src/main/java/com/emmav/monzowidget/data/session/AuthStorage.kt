package com.emmav.monzowidget.data.session

import android.content.Context
import androidx.core.content.edit

/**
 * AuthStorage is responsible for storing the temporary state token used in the OAuth 2.0 login flow.
 * It uses SharedPreferences to persist the state token across app sessions.
 */
class AuthStorage(
    context: Context
) {
    private val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    var stateToken: String?
        get() = prefs.getString("state_token", null)
        set(value) = prefs.edit { putString("state_token", value) }
}