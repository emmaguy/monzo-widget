package com.emmav.monzowidget.data.monzo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class DbAccount(
    @PrimaryKey val id: String,
    val ownerType: String,
    val productType: String,
    val countryCodeAlpha2: String,
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