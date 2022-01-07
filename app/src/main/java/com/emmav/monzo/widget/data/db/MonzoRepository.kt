package com.emmav.monzo.widget.data.db

import com.emmav.monzo.widget.data.api.*
import com.emmav.monzo.widget.data.appwidget.WidgetType
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class MonzoRepository @Inject constructor(
    private val monzoApi: MonzoApi,
    private val monzoStorage: MonzoStorage
) {
    fun syncAccounts(): Single<List<ApiAccount>> {
        return monzoApi.accounts()
            .map { it.accounts }
            .doOnSuccess {
                monzoStorage.saveAccounts(it.map { apiAccount -> apiAccount.toDbAccount() })
            }
            .subscribeOn(Schedulers.io())
    }

    fun syncBalance(accountId: String): Completable {
        return monzoApi.balance(accountId = accountId)
            .doOnSuccess { monzoStorage.saveBalance(it.toDbBalance(accountId = accountId)) }
            .ignoreElement()
            .subscribeOn(Schedulers.io())
    }

    fun syncPots(accountId: String): Completable {
        return monzoApi.pots(accountId = accountId)
            .doOnSuccess {
                val pots = it.pots
                    .filter { apiPot -> !apiPot.deleted }
                    .map { apiPot -> apiPot.toDbPot(accountId = accountId) }
                monzoStorage.savePots(pots)
            }
            .ignoreElement()
            .subscribeOn(Schedulers.io())
    }

    fun accountsWithBalance(): Observable<List<DbAccountWithBalance>> {
        return monzoStorage.accountsWithBalance()
            .subscribeOn(Schedulers.io())
    }

    fun pots(): Observable<List<DbPot>> {
        return monzoStorage.pots()
            .subscribeOn(Schedulers.io())
    }
}
