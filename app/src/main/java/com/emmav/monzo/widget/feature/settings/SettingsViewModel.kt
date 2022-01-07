package com.emmav.monzo.widget.feature.settings

import com.emmav.monzo.widget.R
import com.emmav.monzo.widget.common.BaseViewModel
import com.emmav.monzo.widget.common.Text
import com.emmav.monzo.widget.common.textRes
import com.emmav.monzo.widget.data.api.toLongAccountType
import com.emmav.monzo.widget.data.auth.LoginRepository
import com.emmav.monzo.widget.data.db.MonzoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign

class SettingsViewModel @AssistedInject constructor(
    @Assisted private val appWidgetId: Int,
    @Assisted private val widgetTypeId: String?,
    loginRepository: LoginRepository,
    private val monzoRepository: MonzoRepository
) : BaseViewModel<SettingsViewModel.State>(initialState = State()) {

    private val accountsObservable = monzoRepository.accountsWithBalance()
        .map {
            it.map { dbAccountWithBalance ->
                val id = dbAccountWithBalance.account.id
                Row.Widget(
                    title = "💳 ${dbAccountWithBalance.account.type.toLongAccountType()}",
                    isSelected = id == widgetTypeId,
                    click = { onAccountClicked(id) })
            }
        }
        .map { listOf(Row.Header(title = textRes(R.string.settings_header_accounts))) + it }
        .replay(1)
        .refCount()

    private val potsObservable: Observable<List<Row>> = monzoRepository.pots()
        .map {
            it.map { pot ->
                Row.Widget(
                    title = "🍯 ${pot.name}",
                    isSelected = pot.id == widgetTypeId,
                    click = { onPotClicked(pot.id) })
            }
        }
        .map { listOf(Row.Header(title = textRes(R.string.settings_header_pots))) + it }
        .replay(1)
        .refCount()

    init {
        if (!loginRepository.hasToken) {
            setState { copy(error = true) }
        }

        disposables += Observable.combineLatest(
            accountsObservable,
            potsObservable
        ) { accounts, pots ->
            accounts + pots
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { setState { copy(loading = false, rows = it) } }
    }

    private fun onAccountClicked(accountId: String) {
        disposables += monzoRepository.saveAccountWidget(accountId = accountId, id = appWidgetId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { setState { copy(complete = true) } }
    }

    private fun onPotClicked(potId: String) {
        disposables += monzoRepository.savePotWidget(potId = potId, id = appWidgetId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { setState { copy(complete = true) } }
    }

    data class State(
        val loading: Boolean = true,
        val rows: List<Row> = emptyList(),
        val complete: Boolean = false,
        val error: Boolean = false
    )

    @AssistedFactory
    interface Factory {
        fun create(appWidgetId: Int, widgetTypeId: String?): SettingsViewModel
    }
}

sealed class Row {
    data class Header(val title: Text) : Row()
    data class Widget(
        val title: String,
        val isSelected: Boolean,
        val click: ((Unit) -> Unit)
    ) : Row()
}
