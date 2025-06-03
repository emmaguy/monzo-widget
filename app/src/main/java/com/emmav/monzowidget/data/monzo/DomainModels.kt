package com.emmav.monzowidget.data.monzo

import java.math.BigDecimal

data class Account(
    val id: String,
    val ownerType: String,
    val productType: String,
    val countryCodeEmoji: String,
    val balance: Balance? = null,
    val pots: List<Pot>
)

data class Balance(val currencyCode: String, val amount: BigDecimal)

data class Pot(
    val id: String,
    val accountId: String,
    val name: String,
    val balance: Balance? = null,
)

internal fun DbBalance.toBalance(): Balance {
    return Balance(
        currencyCode = currency,
        amount = amountToBigDecimal(balance) ?: BigDecimal.ZERO,
    )
}

internal fun DbPot.toPot(): Pot {
    return Pot(
        id = id,
        accountId = accountId,
        name = name,
        balance = amountToBigDecimal(balance)?.let { Balance(currency, it) }
    )
}

internal fun DbAccount.toAccount(pots: List<Pot>, balance: Balance?): Account {
    return Account(
        id = id,
        ownerType = ownerType,
        productType = productType,
        countryCodeEmoji = countryCodeToEmojiFlag(countryCodeAlpha2),
        balance = balance,
        pots = pots,
    )
}

private fun amountToBigDecimal(amount: Long): BigDecimal? {
    return BigDecimal.valueOf(amount, 2)
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