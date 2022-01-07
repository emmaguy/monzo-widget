package com.emmav.monzo.widget.common

import androidx.annotation.StringRes

sealed class Text {
    data class String(val value: CharSequence) : Text()
    data class ResString(@StringRes val resId: Int, val args: List<Any> = emptyList()) : Text()

    companion object {
        val Empty = String("")
    }
}

fun text(value: CharSequence) = Text.String(value)

fun textRes(@StringRes resId: Int, vararg args: Any): Text {
    return Text.ResString(resId, args.toList())
}