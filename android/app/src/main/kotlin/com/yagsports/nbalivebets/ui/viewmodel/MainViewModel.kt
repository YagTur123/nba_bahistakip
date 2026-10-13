package com.yagsports.nbalivebets.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yagsports.nbalivebets.data.api.Game
import com.yagsports.nbalivebets.data.api.Player
import com.yagsports.nbalivebets.data.database.BetDatabase
import com.yagsports.nbalivebets.data.models.BetEntity
import com.yagsports.nbalivebets.data.repository.BetRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BetRepository

    init {
        val dao = BetDatabase.getInstance(application).betDao()
        repository = BetRepository(dao)
    }

    val allBets: StateFlow<List<BetEntity>> = repository.allBets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val bets: StateFlow<List<BetEntity>> get() = allBets

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _selectedGame = MutableStateFlow<Game?>(null)
    val selectedGame: StateFlow<Game?> = _selectedGame.asStateFlow()

    private val _selectedGameId = MutableStateFlow<String?>(null)
    val selectedGameId: StateFlow<String?> = _selectedGameId.asStateFlow()

    private val _selectedGamePlayers = MutableStateFlow<List<Player>>(emptyList())
    val selectedGamePlayers: StateFlow<List<Player>> = _selectedGamePlayers.asStateFlow()

    private val _delaySeconds = MutableStateFlow(0)
    val delaySeconds: StateFlow<Int> = _delaySeconds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadScoreboard()
        startLivePolling()
    }

    fun setDelay(sec: Int) {
        _delaySeconds.value = sec
    }

    fun loadScoreboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val resp = repository.fetchScoreboard()
                _games.value = resp.scoreboard.games
            } catch (e: Exception) {
                _errorMessage.value = "Maçlar yüklenemedi: ${e.message ?: "Bilinmeyen hata"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshData() = loadScoreboard()
    fun setDelaySeconds(sec: Int) = setDelay(sec)

    fun selectGame(gameId: String) {
        _selectedGameId.value = gameId
        val game = _games.value.find { it.gameId == gameId }
        if (game != null) {
            selectGame(game)
        }
    }

    fun removeBet(betId: Long) {
        viewModelScope.launch {
            val bet = allBets.value.find { it.id == betId }
            if (bet != null) {
                repository.deleteBet(bet)
            }
        }
    }

    fun selectGame(game: Game) {
        _selectedGame.value = game
        _selectedGameId.value = game.gameId
        viewModelScope.launch {
            try {
                val box = repository.fetchBoxScore(game.gameId)
                val allPlayers = box.game.awayTeam.players + box.game.homeTeam.players
                _selectedGamePlayers.value = allPlayers
            } catch (e: Exception) {
                _selectedGamePlayers.value = emptyList()
            }
        }
    }

    fun addBet(
        gameId: String,
        playerId: Int,
        playerName: String,
        playerTeam: String,
        jerseyNumber: String,
        statDisplay: String,
        target: Double,
        betType: String
    ) {
        viewModelScope.launch {
            val entity = BetEntity(
                gameId = gameId,
                playerId = playerId,
                playerName = playerName,
                playerTeam = playerTeam,
                jerseyNumber = jerseyNumber,
                statKey = statDisplay,
                statDisplay = statDisplay,
                target = target,
                betType = betType
            )
            repository.insertBet(entity)
        }
    }

    fun deleteBet(bet: BetEntity) {
        viewModelScope.launch {
            repository.deleteBet(bet)
        }
    }

    private fun startLivePolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                val currentBets = allBets.value
                val gameIds = currentBets.map { it.gameId }.distinct()
                if (gameIds.isNotEmpty()) {
                    for (gId in gameIds) {
                        try {
                            val box = repository.fetchBoxScore(gId)
                            val players = box.game.awayTeam.players + box.game.homeTeam.players
                            for (bet in currentBets.filter { it.gameId == gId }) {
                                val p = players.find { it.personId == bet.playerId }
                                if (p != null) {
                                    val currentVal = BetRepository.computeStatValue(bet.statDisplay, p.statistics)
                                    val won = bet.betType == "ÜST" && currentVal >= bet.target
                                    val lost = bet.betType == "ALT" && currentVal >= bet.target
                                    if (currentVal != bet.currentValue || won != bet.isWon || lost != bet.isLost) {
                                        val updated = bet.copy(
                                            currentValue = currentVal,
                                            isWon = won,
                                            isLost = lost,
                                            lastUpdated = System.currentTimeMillis()
                                        )
                                        repository.updateBet(updated)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Hata durumunda polling devam eder
                        }
                    }
                }
                delay(5000L + (_delaySeconds.value * 1000L))
            }
        }
    }
}
