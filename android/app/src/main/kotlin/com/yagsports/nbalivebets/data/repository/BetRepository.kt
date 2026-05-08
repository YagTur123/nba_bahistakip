package com.yagsports.nbalivebets.data.repository

import com.yagsports.nbalivebets.data.api.NBAApiService
import com.yagsports.nbalivebets.data.database.BetDao
import com.yagsports.nbalivebets.data.models.BetEntity
import kotlinx.coroutines.flow.Flow

class BetRepository(
    private val betDao: BetDao,
    private val apiService: NBAApiService
) {
    fun getAllBets(): Flow<List<BetEntity>> = betDao.getAllBets()

    fun getBetsByGameId(gameId: String): Flow<List<BetEntity>> = betDao.getBetsByGameId(gameId)

    suspend fun insertBet(bet: BetEntity) = betDao.insertBet(bet)

    suspend fun updateBet(bet: BetEntity) = betDao.updateBet(bet)

    suspend fun deleteBet(bet: BetEntity) = betDao.deleteBet(bet)

    suspend fun getScoreboard() = apiService.getScoreboard()
}
