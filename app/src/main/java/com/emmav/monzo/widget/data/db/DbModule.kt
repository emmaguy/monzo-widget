package com.emmav.monzo.widget.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MonzoStorage {
        return Room.databaseBuilder(context, AppDatabase::class.java, "db")
            .fallbackToDestructiveMigration()
            .build()
            .storage()
    }
}

@Database(
    entities = [
        DbAccount::class,
        DbBalance::class,
        DbPot::class,
        DbWidget::class
    ], version = 4, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storage(): MonzoStorage
}