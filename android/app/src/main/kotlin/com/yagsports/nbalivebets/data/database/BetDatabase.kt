package com.yagsports.nbalivebets.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yagsports.nbalivebets.data.models.BetEntity

@Database(entities = [BetEntity::class], version = 1)
abstract class BetDatabase : RoomDatabase() {
    abstract fun betDao(): BetDao

    companion object {
        @Volatile
        private var instance: BetDatabase? = null

        fun getInstance(context: Context): BetDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    BetDatabase::class.java,
                    "nba_bets.db"
                ).build().also { instance = it }
            }
        }
    }
}
