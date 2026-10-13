package com.yagsports.nbalivebets.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
    val gameClock: String? = null,
    val period: Int? = null,
    val awayTeam: Team,
    val homeTeam: Team
) {
    val id: String get() = gameId
    val homeScore: Int get() = homeTeam.score
    val awayScore: Int get() = awayTeam.score
    val homeTeamName: String get() = homeTeam.teamTricode.ifBlank { homeTeam.teamName }
    val awayTeamName: String get() = awayTeam.teamTricode.ifBlank { awayTeam.teamName }
}

data class Team(
    val teamId: Int,
    val teamName: String,
    val teamTricode: String,
    val score: Int,
    val players: List<Player> = emptyList()
)

data class Player(
    val personId: Int,
    val name: String,
    val jerseyNum: String? = "0",
    val position: String? = null,
    val statistics: Statistics? = null,
    val team: String = ""
) {
    val id: Int get() = personId
    val jersey: String get() = jerseyNum ?: "0"
}

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

data class BoxScoreResponse(
    val game: BoxScoreGame
)

data class BoxScoreGame(
    val gameId: String,
    val gameStatus: Int,
    val gameStatusText: String,
    val awayTeam: Team,
    val homeTeam: Team
)

object NBAApiClient {
    private const val BASE_URL = "https://cdn.nba.com/"

    private val gson: Gson = GsonBuilder().create()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val modifiedRequest = originalRequest.newBuilder()
                .header("User-Agent", "Mozilla/5.0")
                .build()
            chain.proceed(modifiedRequest)
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: NBAApi = retrofit.create(NBAApi::class.java)
}

interface NBAApi {
    @GET("static/json/liveData/scoreboard/todaysScoreboard_00.json")
    suspend fun getScoreboard(): ScoreBoardResponse

    @GET("static/json/liveData/boxscore/boxscore_{gameId}.json")
    suspend fun getBoxScore(@Path("gameId") gameId: String): BoxScoreResponse
}
