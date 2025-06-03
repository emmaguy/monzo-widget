package com.emmav.monzowidget.ui

import com.emmav.monzowidget.data.monzo.Account
import com.emmav.monzowidget.data.monzo.Balance
import java.text.NumberFormat
import java.util.Currency

object Utils {
    fun Balance.formatBalance(): String {
        return NumberFormat.getCurrencyInstance()
            .apply {
                currency = Currency.getInstance(currencyCode)
            }
            .format(amount)
    }

    fun Account.title(): String {
        val title = if (productType == "standard") {
            ownerType // e.g. personal or joint
        } else {
            productType
        }
        return title.replaceFirstChar { it.uppercaseChar() }
    }
}