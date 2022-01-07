package com.emmav.monzo.widget.feature.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.emmav.monzo.widget.data.db.MonzoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

@HiltWorker class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val monzoRepository: MonzoRepository,
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
            } // TODO: Implement refresh all
//            .andThen { BalanceWidget().updateAll(context) }
            .doOnComplete { Timber.d("Successfully refreshed data") }
            .toSingleDefault(Result.success())
            .subscribeOn(Schedulers.io())
    }
}