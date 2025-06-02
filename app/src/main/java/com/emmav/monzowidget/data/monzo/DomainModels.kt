package com.emmav.monzowidget.data.monzo

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

data class Account(
    val id: String,
    val ownerType: String,
    val productType: String,
    val emoji: String,
    val balance: String? = null,
    val pots: List<Pot>
)

data class Balance(val currency: String, val amount: Long)

data class Pot(
    val id: String,
    val accountId: String,
    val name: String,
    val balance: String? = null,
)

internal fun DbBalance.toBalance(): Balance {
    return Balance(
        currency = currency,
        amount = balance,
    )
}

internal fun DbPot.toPot(): Pot {
    return Pot(
        id = id,
        accountId = accountId,
        name = name,
        balance = formatBalance(currency, balance),
    )
}

internal fun DbAccount.toAccount(pots: List<Pot>, balance: Balance?): Account {
    return Account(
        id = id,
        ownerType = ownerType,
        productType = productType,
        emoji = countryCodeToEmojiFlag(countryCodeAlpha2),
        balance = if (balance != null) {
            formatBalance(balance.currency, balance.amount)
        } else {
            null
        },
        pots = pots,
    )
}

private fun formatBalance(curr: String, amount: Long): String {
    return NumberFormat.getCurrencyInstance()
        .apply {
            currency = Currency.getInstance(curr)
        }
        .format(BigDecimal.valueOf(amount, 2))
}


private fun countryCodeToEmojiFlag(countryCode: String): String {
    return countryCode
        .map { char ->
            Character.codePointAt("$char", 0) - 0x41 + 0x1F1E6
        }
        .map { codePoint ->
            Character.toChars(codePoint)
        }
        .joinToString(separator = "") { charArray ->
            String(charArray)
        }
}