package com.yagsports.nbalivebets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yagsports.nbalivebets.data.api.Game
import com.yagsports.nbalivebets.data.api.Player
import com.yagsports.nbalivebets.data.models.BetEntity
import com.yagsports.nbalivebets.data.repository.BetRepository
import com.yagsports.nbalivebets.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val games by viewModel.games.collectAsState()
    val bets by viewModel.bets.collectAsState()
    val selectedGameId by viewModel.selectedGameId.collectAsState()
    val delaySeconds by viewModel.delaySeconds.collectAsState()

    var showAddBetDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)),
            color = Color(0xFF121212)
        ) {
            Scaffold(
                topBar = {
                    TopBar(
                        delaySeconds = delaySeconds,
                        onRefresh = { viewModel.refreshData() },
                        onDelayChange = { viewModel.setDelaySeconds(it) }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddBetDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Bahis Ekle")
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    // Canlı Maçlar Bandı
                    Text(
                        text = "🏀 Canlı Maçlar",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(games) { game ->
                            GameCard(
                                game = game,
                                isSelected = selectedGameId == game.id,
                                onClick = { viewModel.selectGame(game.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Aktif Bahisler
                    Text(
                        text = "📊 Aktif Takipteki Bahisler",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bets) { bet ->
                            BetCard(
                                bet = bet,
                                onRemove = { viewModel.removeBet(bet.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddBetDialog) {
        AddBetDialog(
            games = games,
            players = emptyList(), // ViewModel'den gelecek
            onGameSelected = { /* handled in dialog */ },
            onDismiss = { showAddBetDialog = false },
            onAdd = { gameId, playerId, pName, pTeam, jersey, stat, target, type ->
                viewModel.addBet(
                    gameId = gameId,
                    playerId = playerId,
                    playerName = pName,
                    playerTeam = pTeam,
                    jersey = jersey,
                    stat = stat,
                    target = target,
                    type = type
                )
                showAddBetDialog = false
            }
        )
    }
}

@Composable
fun TopBar(
    delaySeconds: Int,
    onRefresh: () -> Unit,
    onDelayChange: (Int) -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🏀 NBA Canlı Takip",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Yenile",
                    tint = Color.White
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "Gecikme:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                )
                IconButton(onClick = { if (delaySeconds > 0) onDelayChange(delaySeconds - 5) }) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Azalt",
                        tint = Color.White
                    )
                }
                Text(
                    text = "$delaySeconds sn",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Cyan)
                )
                IconButton(onClick = { if (delaySeconds < 60) onDelayChange(delaySeconds + 5) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Artır",
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF1F1F1F),
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
fun GameCard(
    game: Game,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1976D2) else Color(0xFF2C2C2C)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = game.homeTeam,
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = game.awayTeam,
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = game.homeScore.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(color = Color(0xFF00B4D8)),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "-",
                    style = MaterialTheme.typography.headlineSmall.copy(color = Color.Gray)
                )
                Text(
                    text = game.awayScore.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(color = Color(0xFF00B4D8)),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = game.quarterStatus,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB0B0B0))
            )
        }
    }
}

@Composable
fun BetCard(
    bet: BetEntity,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = bet.playerName,
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "#${bet.jersey}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB0B0B0))
                    )
                    Text(
                        text = bet.playerTeam,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF00B4D8)),
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = Color(0xFFDC3545)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${bet.stat} • ${bet.type}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB0B0B0))
                )
                Text(
                    text = "Hedef: ${bet.target}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF00B4D8))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${bet.current} / ${bet.target} (${bet.percentage}%)",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB0B0B0))
                )
                LinearProgressIndicator(
                    progress = bet.percentage.coerceIn(0.0, 100.0) / 100f,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .padding(start = 8.dp),
                    color = when {
                        bet.isWon -> Color(0xFF28A745)
                        bet.isLost -> Color(0xFFDC3545)
                        else -> Color(0xFF00B4D8)
                    },
                    trackColor = Color(0xFF444444)
                )
            }
        }
    }
}

@Composable
fun AddBetDialog(
    games: List<Game>,
    players: List<Player>,
    onGameSelected: (Game) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (gameId: String, playerId: Int, pName: String, pTeam: String, jersey: String, stat: String, target: Double, type: String) -> Unit
) {
    var selectedGame by remember { mutableStateOf<Game?>(null) }
    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var selectedStat by remember { mutableStateOf(BetRepository.STAT_OPTIONS.first()) }
    var targetValue by remember { mutableStateOf("") }
    var typeValue by remember { mutableStateOf("ÜST") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1F1F1F)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "➕ Yeni Bahis Ekle",
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Maç seçimi
                Text(
                    text = "Maç:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB0B0B0)),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    DropdownMenu(
                        expanded = selectedGame == null,
                        onDismissRequest = { },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        games.forEach { game ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${game.homeTeam} vs ${game.awayTeam}",
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    selectedGame = game
                                    onGameSelected(game)
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = selectedGame?.let { "${it.homeTeam} vs ${it.awayTeam}" } ?: "",
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Seçili Maç", color = Color(0xFFB0B0B0)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00B4D8),
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedLabelColor = Color(0xFF00B4D8),
                            unfocusedLabelColor = Color(0xFFB0B0B0)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Oyuncu seçimi
                Text(
                    text = "Oyuncu:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB0B0B0)),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    DropdownMenu(
                        expanded = selectedPlayer == null,
                        onDismissRequest = { },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        players.forEach { player ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${player.name} (#${player.jersey}) - ${player.team}",
                                        color = Color.White
                                    )
                                },
                                onClick = { selectedPlayer = player }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = selectedPlayer?.let { "${it.name} (#${it.jersey})" } ?: "",
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Seçili Oyuncu", color = Color(0xFFB0B0B0)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00B4D8),
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedLabelColor = Color(0xFF00B4D8),
                            unfocusedLabelColor = Color(0xFFB0B0B0)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // İstatistik türü
                Text(
                    text = "İstatistik Türü:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB0B0B0)),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                DropdownMenu(
                    expanded = false,
                    onDismissRequest = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BetRepository.STAT_OPTIONS.forEach { stat ->
                        DropdownMenuItem(
                            text = { Text(stat, color = Color.White) },
                            onClick = { selectedStat = stat }
                        )
                    }
                }
                OutlinedTextField(
                    value = selectedStat,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Seçili İstatistik", color = Color(0xFFB0B0B0)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00B4D8),
                        unfocusedBorderColor = Color(0xFF444444),
                        focusedLabelColor = Color(0xFF00B4D8),
                        unfocusedLabelColor = Color(0xFFB0B0B0)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hedef
                Text(
                    text = "Hedef Değer:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB0B0B0)),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("\\d*\\.?\\d*"))) targetValue = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Hedef", color = Color(0xFFB0B0B0)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00B4D8),
                        unfocusedBorderColor = Color(0xFF444444),
                        focusedLabelColor = Color(0xFF00B4D8),
                        unfocusedLabelColor = Color(0xFFB0B0B0)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ÜST / ALT
                Text(
                    text = "Üst/Alt:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB0B0B0)),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { typeValue = "ÜST" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (typeValue == "ÜST") Color(0xFF00B4D8) else Color(0xFF444444)
                        )
                    ) {
                        Text("ÜST", color = Color.White)
                    }
                    Button(
                        onClick = { typeValue = "ALT" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (typeValue == "ALT") Color(0xFF00B4D8) else Color(0xFF444444)
                        )
                    ) {
                        Text("ALT", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Butonlar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İptal", color = Color(0xFFB0B0B0))
                    }
                    Button(
                        onClick = {
                            if (selectedGame != null && selectedPlayer != null && targetValue.isNotEmpty()) {
                                onAdd(
                                    gameId = selectedGame!!.id,
                                    playerId = selectedPlayer!!.id,
                                    pName = selectedPlayer!!.name,
                                    pTeam = selectedPlayer!!.team,
                                    jersey = selectedPlayer!!.jersey,
                                    stat = selectedStat,
                                    target = targetValue.toDoubleOrNull() ?: 0.0,
                                    type = typeValue
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedGame != null && selectedPlayer != null && targetValue.isNotEmpty()
                    ) {
                        Text("Kaydet", color = Color.White)
                    }
                }
            }
        }
    }
}
