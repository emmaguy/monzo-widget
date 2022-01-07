package com.emmav.monzo.widget.feature.sync

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.emmav.monzo.widget.data.appwidget.WidgetRepository
import com.emmav.monzo.widget.data.db.MonzoRepository
import com.emmav.monzo.widget.feature.appwidget.WidgetProvider
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

@HiltWorker class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val monzoRepository: MonzoRepository,
    private val widgetRepository: WidgetRepository,
) : RxWorker(context, workerParams) {

    override fun createWork(): Single<Result> {
        return monzoRepository.syncAccounts()
            .flatMapCompletable { accounts ->
                Completable.merge(
                    accounts.map { account ->
                        monzoRepository.syncBalance(accountId = account.id)
                            .andThen(monzoRepository.syncPots(accountId = account.id))
                    }
                )
            }
            .doOnComplete {
                Timber.d("Successfully refreshed data")
                WidgetProvider.updateAllWidgets(context, AppWidgetManager.getInstance(context), widgetRepository)
            }
            .toSingleDefault(Result.success())
            .subscribeOn(Schedulers.io())
    }
}