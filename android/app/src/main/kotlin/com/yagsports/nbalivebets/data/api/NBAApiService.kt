package com.yagsports.nbalivebets.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface NBAApiService {
    @GET("statsSampleURL")
    suspend fun getScoreboard(): ScoreBoardResponse

    @GET("liveAPI/nba/static/data/json/cms/2024/nba/scoreboard/{date}/games_{date}_{hour}00.json")
    suspend fun getGamesByDate(
        @Path("date") date: String,
        @Path("hour") hour: String
    ): ScoreBoardResponse
}

data class ScoreBoardResponse(
    val scoreboard: Scoreboard
)

data class Scoreboard(
    val games: List<Game>
)

data class Game(
    val gameId: String,
    val gameStatus: Int,
    val gameStatusText: String,
    val awayTeam: Team,
    val homeTeam: Team
)

data class Team(
    val teamId: Int,
    val teamName: String,
    val teamTricode: String,
    val score: Int,
    val players: List<Player>
)

data class Player(
    val personId: Int,
    val name: String,
    val jerseyNum: Int?,
    val statistics: Statistics
)

data class Statistics(
    val points: Int = 0,
    val assists: Int = 0,
    val reboundsTotal: Int = 0,
    val reboundsOffensive: Int = 0,
    val reboundsDefensive: Int = 0,
    val threePointersMade: Int = 0,
    val threePointersAttempted: Int = 0,
    val fieldGoalsMade: Int = 0,
    val fieldGoalsAttempted: Int = 0,
    val freeThrowsMade: Int = 0,
    val freeThrowsAttempted: Int = 0,
    val steals: Int = 0,
    val blocks: Int = 0,
    val turnovers: Int = 0,
    val foulsPersonal: Int = 0
)
