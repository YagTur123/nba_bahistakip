package com.yagsports.nbalivebets.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.yagsports.nbalivebets.data.models.BetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BetDao {
    @Insert
    suspend fun insertBet(bet: BetEntity)

    @Update
    suspend fun updateBet(bet: BetEntity)

    @Delete
    suspend fun deleteBet(bet: BetEntity)

    @Query("SELECT * FROM bets WHERE gameId = :gameId")
    fun getBetsByGameId(gameId: String): Flow<List<BetEntity>>

    @Query("SELECT * FROM bets")
    fun getAllBets(): Flow<List<BetEntity>>

    @Query("DELETE FROM bets WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: String)
}
