package com.emmav.monzo.widget.data.appwidget

sealed class Widget {
    abstract val id: String
    abstract val widgetTypeId: String
    abstract val emoji: String
    abstract val balance: Long
    abstract val currency: String

    data class Account(
        override val id: String,
        override val widgetTypeId: String,
        override val emoji: String = "💳",
        override val balance: Long,
        override val currency: String,
        val name: String
    ) : Widget() {
        override fun toString(): String {
            return "$emoji $name"
        }
    }

    data class Pot(
        override val id: String,
        override val widgetTypeId: String,
        override val emoji: String = "🍯",
        override val balance: Long,
        override val currency: String,
        val name: String
    ) : Widget() {
        override fun toString(): String {
            return "$emoji $name"
        }
    }
}
