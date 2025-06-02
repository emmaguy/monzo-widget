package com.emmav.monzowidget.data.monzo

import com.emmav.monzo.widget.data.db.MonzoStorage
import com.emmav.monzowidget.data.api.MonzoApi
import com.emmav.monzowidget.data.api.toDbAccount
import com.emmav.monzowidget.data.api.toDbBalance
import com.emmav.monzowidget.data.api.toDbPot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MonzoRepository(
    private val api: MonzoApi,
    private val storage: MonzoStorage
) {
    suspend fun refreshAccounts(): Result<List<String>> {
        val result = api.accounts()
        return if (result.isSuccessful) {
            val accounts = (result.body()?.accounts ?: emptyList())
                .filter { account -> !account.closed }
            withContext(Dispatchers.IO) {
                storage.saveAccounts(accounts.map { apiAccount -> apiAccount.toDbAccount() })
            }

            Result.success(accounts.map { it.id })
        } else {
            Result.failure(
                Exception("Failed to refresh accounts: ${result.errorBody()?.string()}")
            )
        }
    }

    fun accountsWithPots(): Flow<List<Account>> {
        return storage.accounts()
            .map {
                val balances = storage.balance()

                val pots = storage.pots()
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
        val result = api.balance(accountId = accountId)
        return if (result.isSuccessful) {
            val balance = result.body()
            balance?.let {
                withContext(Dispatchers.IO) {
                    storage.saveBalance(it.toDbBalance(accountId = accountId))
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
        val result = api.pots(accountId = accountId)

        return if (result.isSuccessful) {
            val pots = result.body()?.pots
                ?.filter { apiPot -> !apiPot.deleted }
                ?.map { apiPot -> apiPot.toDbPot(accountId = accountId) }

            pots?.let {
                withContext(Dispatchers.IO) {
                    storage.savePots(it)
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