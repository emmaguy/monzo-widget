package com.emmav.monzo.widget.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.emmav.monzowidget.data.monzo.DbAccount
import com.emmav.monzowidget.data.monzo.DbBalance
import com.emmav.monzowidget.data.monzo.DbPot
import kotlinx.coroutines.flow.Flow

@Dao
interface MonzoStorage {

    @Transaction
    @Query("SELECT * FROM accounts")
    fun accounts(): Flow<List<DbAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAccounts(accounts: List<DbAccount>)

    @Query("SELECT * FROM balance")
    suspend fun balance(): List<DbBalance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveBalance(dbBalance: DbBalance)

    @Query("SELECT * FROM pots")
    suspend fun pots(): List<DbPot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun savePots(pots: List<DbPot>)
}