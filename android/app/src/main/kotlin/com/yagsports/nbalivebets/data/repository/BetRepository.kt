package com.yagsports.nbalivebets.data.repository

import com.yagsports.nbalivebets.data.api.NBAApiClient
import com.yagsports.nbalivebets.data.api.Statistics
import com.yagsports.nbalivebets.data.database.BetDao
import com.yagsports.nbalivebets.data.models.BetEntity
import kotlinx.coroutines.flow.Flow

class BetRepository(private val betDao: BetDao) {
    val allBets: Flow<List<BetEntity>> = betDao.getAllBets()

    suspend fun insertBet(bet: BetEntity) = betDao.insertBet(bet)
    suspend fun updateBet(bet: BetEntity) = betDao.updateBet(bet)
    suspend fun deleteBet(bet: BetEntity) = betDao.deleteBet(bet)

    suspend fun fetchScoreboard() = NBAApiClient.api.getScoreboard()
    suspend fun fetchBoxScore(gameId: String) = NBAApiClient.api.getBoxScore(gameId)

    companion object {
        val STAT_OPTIONS = listOf(
            "Sayı (PTS)",
            "Asist (AST)",
            "Ribaund (REB)",
            "Hücum Ribaundu (OREB)",
            "Savunma Ribaundu (DREB)",
            "Üç Sayılık İsabet (3PM)",
            "Üç Sayılık Deneme (3PA)",
            "Saha İçi İsabet (FGM)",
            "Saha İçi Deneme (FGA)",
            "Serbest Atış İsabet (FTM)",
            "Serbest Atış Deneme (FTA)",
            "Top Çalma (STL)",
            "Blok (BLK)",
            "Top Kaybı (TOV)",
            "Faul (PF)",
            "Sayı+Rib+Asist (PRA)",
            "Sayı+Asist (P+A)",
            "Sayı+Ribaund (P+R)",
            "Rib+Asist (R+A)",
            "Üçlük+Asist (3P+A)",
            "Top Çalma+Blok (S+B)"
        )

        fun computeStatValue(statDisplay: String, stats: Statistics?): Int {
            if (stats == null) return 0

            return when (statDisplay) {
                "Sayı (PTS)" -> stats.points
                "Asist (AST)" -> stats.assists
                "Ribaund (REB)" -> stats.reboundsTotal
                "Hücum Ribaundu (OREB)" -> stats.reboundsOffensive
                "Savunma Ribaundu (DREB)" -> stats.reboundsDefensive
                "Üç Sayılık İsabet (3PM)" -> stats.threePointersMade
                "Üç Sayılık Deneme (3PA)" -> stats.threePointersAttempted
                "Saha İçi İsabet (FGM)" -> stats.fieldGoalsMade
                "Saha İçi Deneme (FGA)" -> stats.fieldGoalsAttempted
                "Serbest Atış İsabet (FTM)" -> stats.freeThrowsMade
                "Serbest Atış Deneme (FTA)" -> stats.freeThrowsAttempted
                "Top Çalma (STL)" -> stats.steals
                "Blok (BLK)" -> stats.blocks
                "Top Kaybı (TOV)" -> stats.turnovers
                "Faul (PF)" -> stats.foulsPersonal
                "Sayı+Rib+Asist (PRA)" -> stats.points + stats.reboundsTotal + stats.assists
                "Sayı+Asist (P+A)" -> stats.points + stats.assists
                "Sayı+Ribaund (P+R)" -> stats.points + stats.reboundsTotal
                "Rib+Asist (R+A)" -> stats.reboundsTotal + stats.assists
                "Üçlük+Asist (3P+A)" -> stats.threePointersMade + stats.assists
                "Top Çalma+Blok (S+B)" -> stats.steals + stats.blocks
                else -> stats.points
            }
        }
    }
}
