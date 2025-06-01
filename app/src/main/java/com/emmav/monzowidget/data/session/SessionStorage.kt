package com.emmav.monzowidget.data.session

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Dao
interface SessionStorage {

    @Query("SELECT * FROM oauth_session LIMIT 1")
    suspend fun getSession(): DbSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: DbSession)

    @Query("DELETE FROM oauth_session")
    suspend fun clearSession()
}

@Entity(tableName = "oauth_session")
data class DbSession(
    @PrimaryKey val id: Int = 0, // Singleton session (id = 0)
    val accessToken: String,
    val refreshToken: String,
)