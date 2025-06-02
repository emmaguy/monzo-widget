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
        if (productType == "standard") {
            return ownerType // personal or joint
        }

        return productType
    }
}