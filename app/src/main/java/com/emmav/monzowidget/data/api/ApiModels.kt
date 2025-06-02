package com.emmav.monzowidget.data.api

import com.emmav.monzowidget.data.monzo.DbAccount
import com.emmav.monzowidget.data.monzo.DbBalance
import com.emmav.monzowidget.data.monzo.DbPot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("scope") val scope: String?,
)

@Serializable
data class AccountsResponse(val accounts: List<ApiAccount>)

@Serializable
data class ApiAccount(
    val id: String,
    val closed: Boolean,
    @SerialName("product_type") val productType: String,
    @SerialName("owner_type") val ownerType: String,
    @SerialName("country_code") val countryCode: String,
)

@Serializable
data class ApiBalance(
    val balance: Long,
    val currency: String
)

@Serializable
data class PotsResponse(val pots: List<ApiPot> = emptyList())

@Serializable
data class ApiPot(
    val id: String,
    val name: String,
    val balance: Long,
    val currency: String,
    val deleted: Boolean
)

internal fun ApiAccount.toDbAccount(): DbAccount {
    return DbAccount(
        id = id,
        ownerType = ownerType,
        productType = productType,
        countryCodeAlpha2 = countryCode,
    )
}

internal fun ApiBalance.toDbBalance(accountId: String): DbBalance {
    return DbBalance(accountId = accountId, currency = currency, balance = balance)
}

internal fun ApiPot.toDbPot(accountId: String): DbPot {
    return DbPot(
        id = id,
        accountId = accountId,
        name = name,
        balance = balance,
        currency = currency
    )
}