package com.emmav.monzo.widget.data.appwidget

import com.emmav.monzo.widget.data.api.toLongAccountType
import com.emmav.monzo.widget.data.db.DbWidget
import com.emmav.monzo.widget.data.db.DbWidgetWithRelations
import com.emmav.monzo.widget.data.db.MonzoStorage
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

class WidgetRepository @Inject constructor(private val monzoStorage: MonzoStorage) {

    fun saveAccountWidget(accountId: String, id: String?): Completable {
        return Single.fromCallable {
            // TODO: Update widget using id too
            if (id == null) {
                monzoStorage.saveWidget(
                    DbWidget(
                        id = UUID.randomUUID().toString(),
                        type = WidgetType.ACCOUNT_BALANCE.key,
                        accountId = accountId,
                        potId = null,
                    )
                )
            }
        }
            .ignoreElement()
            .subscribeOn(Schedulers.io())
    }

    fun savePotWidget(potId: String, id: String?): Completable {
        return Single.fromCallable {
            if (id == null) {
                monzoStorage.saveWidget(
                    DbWidget(
                        id = UUID.randomUUID().toString(),
                        type = WidgetType.POT_BALANCE.key,
                        accountId = null,
                        potId = potId,
                    )
                )
            }
        }
            .ignoreElement()
            .subscribeOn(Schedulers.io())
    }

    fun allWidgets(): Observable<List<Widget>> {
        return monzoStorage.widgets()
            .map { dbWidgets -> dbWidgets.map { it.toWidget() } }
            .subscribeOn(Schedulers.io())
    }
}

private fun DbWidgetWithRelations.toWidget(): Widget {
    return if (pot != null) {
        Widget.Pot(
            id = widget.id,
            widgetTypeId = pot.id,
            name = pot.name,
            balance = pot.balance,
            currency = pot.currency
        )
    } else if (account != null && balance != null) {
        Widget.Account(
            id = widget.id,
            widgetTypeId = account.id,
            name = account.type.toLongAccountType(),
            balance = balance.balance,
            currency = balance.currency
        )
    } else {
        throw IllegalArgumentException("Invalid type of widget")
    }
}
