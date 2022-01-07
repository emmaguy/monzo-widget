package com.emmav.monzo.widget.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.*

@Entity(tableName = "accounts")
data class DbAccount(
    @PrimaryKey val id: String,
    val type: String
)

@Entity(tableName = "pots")
data class DbPot(
    @PrimaryKey val id: String,
    val accountId: String,
    val name: String,
    val balance: Long,
    val currency: String
)

@Entity(tableName = "balance")
data class DbBalance(
    @PrimaryKey val accountId: String,
    val currency: String,
    val balance: Long
)

@Entity(tableName = "widgets")
data class DbWidget(
    @PrimaryKey val id: String,
    val type: String,
    val accountId: String?,
    val potId: String?
)

data class DbWidgetWithRelations(
    @Embedded val widget: DbWidget,

    // Either Account & Balance
    @Relation(entity = DbAccount::class, entityColumn = "id", parentColumn = "accountId")
    val account: DbAccount?,

    @Relation(entity = DbBalance::class, entityColumn = "accountId", parentColumn = "accountId")
    val balance: DbBalance?,

    // OR Pot
    @Relation(entity = DbPot::class, entityColumn = "id", parentColumn = "potId")
    val pot: DbPot?
)

data class DbAccountWithBalance(
    @Embedded val account: DbAccount,

    @Relation(entity = DbBalance::class, entityColumn = "accountId", parentColumn = "id")
    val balance: DbBalance
)