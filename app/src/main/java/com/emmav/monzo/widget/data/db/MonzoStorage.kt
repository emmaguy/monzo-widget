package com.emmav.monzo.widget.data.db

import androidx.room.*
import io.reactivex.Observable

@Dao
interface MonzoStorage {

    @Transaction
    @Query("SELECT * FROM accounts")
    fun accountsWithBalance(): Observable<List<DbAccountWithBalance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAccounts(accounts: List<DbAccount>)

    @Query("SELECT * FROM balance")
    fun balance(): Observable<List<DbBalance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveBalance(dbBalance: DbBalance)

    @Query("SELECT * FROM pots")
    fun pots(): Observable<List<DbPot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun savePots(pots: List<DbPot>)

    @Transaction
    @Query("SELECT * FROM widgets")
    fun widgets(): Observable<List<DbWidgetWithRelations>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveWidget(dbWidget: DbWidget)
}