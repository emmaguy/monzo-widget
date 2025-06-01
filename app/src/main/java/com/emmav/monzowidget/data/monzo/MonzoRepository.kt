package com.emmav.monzowidget.data.monzo

import com.emmav.monzo.widget.data.db.MonzoStorage
import com.emmav.monzowidget.api.ApiAccount
import com.emmav.monzowidget.api.ApiBalance
import com.emmav.monzowidget.api.ApiPot
import com.emmav.monzowidget.api.MonzoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

class MonzoRepository(
    private val monzoApi: MonzoApi,
    private val monzoStorage: MonzoStorage
) {
    suspend fun refreshAccounts(): Result<List<String>> {
        val result = monzoApi.accounts()
        return if (result.isSuccessful) {
            val accounts = (result.body()?.accounts ?: emptyList())
                .filter { account -> !account.closed }
            withContext(Dispatchers.IO) {
                monzoStorage.saveAccounts(accounts.map { apiAccount -> apiAccount.toDbAccount() })
            }

            Result.success(accounts.map { it.id })
        } else {
            Result.failure(
                Exception("Failed to refresh accounts: ${result.errorBody()?.string()}")
            )
        }
    }

    fun accounts(): Flow<List<Account>> {
        return monzoStorage.accounts()
            .map {
                val balances = monzoStorage.balance()

                val pots = monzoStorage.pots()
                    .map { it.toPot() }
                    .groupBy { it.accountId }

                it.map { apiAccount ->
                    apiAccount.toAccount(
                        pots = pots[apiAccount.id] ?: emptyList(),
                        balance = balances
                            .firstOrNull { dbBalance -> dbBalance.accountId == apiAccount.id }
                            ?.toBalance()
                    )
                }
            }
    }

    suspend fun refreshBalance(accountId: String): Result<Unit> {
        val result = monzoApi.balance(accountId = accountId)
        return if (result.isSuccessful) {
            val balance = result.body()
            balance?.let {
                withContext(Dispatchers.IO) {
                    monzoStorage.saveBalance(it.toDbBalance(accountId = accountId))
                }
            }
            Result.success(Unit)
        } else {
            Result.failure(
                Exception(
                    "Failed to refresh balance for account $accountId: ${
                        result.errorBody()?.string()
                    }"
                )
            )
        }
    }

    suspend fun refreshPots(accountId: String): Result<Unit> {
        val result = monzoApi.pots(accountId = accountId)

        return if (result.isSuccessful) {
            val pots = result.body()?.pots
                ?.filter { apiPot -> !apiPot.deleted }
                ?.map { apiPot -> apiPot.toDbPot(accountId = accountId) }

            pots?.let {
                withContext(Dispatchers.IO) {
                    monzoStorage.savePots(it)
                }
            }
            Result.success(Unit)
        } else {
            Result.failure(
                Exception(
                    "Failed to refresh pots for account $accountId: ${
                        result.errorBody()?.string()
                    }"
                )
            )
        }
    }
}

private fun DbBalance.toBalance(): Balance {
    return Balance(
        currency = currency,
        amount = balance,
    )
}

private fun DbPot.toPot(): Pot {
    return Pot(
        id = id,
        accountId = accountId,
        name = name,
        balance = formatBalance(currency, balance),
    )
}

private fun DbAccount.toAccount(pots: List<Pot>, balance: Balance?): Account {
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

private fun formatBalance(curr: String, amount: Long): String {
    return NumberFormat.getCurrencyInstance()
        .apply {
            currency = Currency.getInstance(curr)
        }
        .format(BigDecimal.valueOf(amount, 2))
}

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