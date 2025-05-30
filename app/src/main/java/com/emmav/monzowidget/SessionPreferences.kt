package com.emmav.monzowidget

import android.content.Context
import androidx.core.content.edit

class SessionPreferences(
    context: Context
) {
    private val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    var stateToken: String?
        get() = prefs.getString("state_token", null)
        set(value) = prefs.edit { putString("state_token", value) }
}
