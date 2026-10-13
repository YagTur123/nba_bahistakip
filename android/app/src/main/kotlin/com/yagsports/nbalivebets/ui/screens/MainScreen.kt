package com.yagsports.nbalivebets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yagsports.nbalivebets.data.api.Game
import com.yagsports.nbalivebets.data.api.Player
import com.yagsports.nbalivebets.data.models.BetEntity
import com.yagsports.nbalivebets.data.repository.BetRepository
import com.yagsports.nbalivebets.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val games by viewModel.games.collectAsState()
    val bets by viewModel.bets.collectAsState()
    val selectedGameId by viewModel.selectedGameId.collectAsState()
    val selectedGamePlayers by viewModel.selectedGamePlayers.collectAsState()
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
                containerColor = Color(0xFF121212),
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
                        containerColor = Color(0xFF00B4D8),
                        contentColor = Color.White
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
                        text = "🏀 Canlı & Yaklaşan Maçlar",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (games.isEmpty()) {
                        Text(
                            text = "Maç verileri yükleniyor...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF888888),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Aktif Bahisler
                    Text(
                        text = "📊 Takipteki Kuponlar / Oyuncular",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (bets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Henüz takipte kupon yok.\nSağ alttaki '+' butonundan oyuncu baremi ekleyin.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF888888),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(bets, key = { it.id }) { bet ->
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
    }

    if (showAddBetDialog) {
        AddBetDialog(
            games = games,
            players = selectedGamePlayers,
            onGameSelected = { viewModel.selectGame(it.id) },
            onDismiss = { showAddBetDialog = false },
            onAdd = { gId, pId, pName, pTeam, jersey, stat, target, type ->
                viewModel.addBet(
                    gameId = gId,
                    playerId = pId,
                    playerName = pName,
                    playerTeam = pTeam,
                    jerseyNumber = jersey,
                    statDisplay = stat,
                    target = target,
                    betType = type
                )
                showAddBetDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = "Gecikme:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFAAAAAA))
                )
                IconButton(onClick = { if (delaySeconds > 0) onDelayChange(delaySeconds - 5) }) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Azalt",
                        tint = Color.White
                    )
                }
                Text(
                    text = "${delaySeconds}s",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF00B4D8),
                        fontWeight = FontWeight.Bold
                    )
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
            containerColor = if (isSelected) Color(0xFF0A3A60) else Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = game.awayTeamName,
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@",
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.Gray)
                )
                Text(
                    text = game.homeTeamName,
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${game.awayScore} - ${game.homeScore}",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF00B4D8)),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = game.quarterStatus,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (game.gameStatus == 2) Color(0xFF28A745) else Color(0xFFAAAAAA),
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun BetCard(
    bet: BetEntity,
    onRemove: () -> Unit
) {
    val borderColor = when {
        bet.isWon -> Color(0xFF28A745)
        bet.isLost -> Color(0xFFDC3545)
        else -> Color(0xFF333333)
    }

    val progressColor = when {
        bet.isWon -> Color(0xFF28A745)
        bet.isLost -> Color(0xFFDC3545)
        else -> Color(0xFF00B4D8)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(10.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = bet.playerName,
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "#${bet.jersey}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFAAAAAA))
                    )
                    Text(
                        text = bet.playerTeam,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF00B4D8),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = Color(0xFFDC3545)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${bet.stat} • ${bet.type}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (bet.type == "ÜST") Color(0xFF28A745) else Color(0xFFDC3545),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${bet.current} / ${bet.target} (%${bet.percentage()})",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = (bet.percentage.coerceIn(0.0, 100.0) / 100.0).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = Color(0xFF333333)
            )

            if (bet.isWon || bet.isLost) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (bet.isWon) "🎉 BAREM GELDİ (KAZANDI)" else "❌ BAREM AŞILDI (KAYBETTİ)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = progressColor,
                        fontWeight = FontWeight.Bold
                    )
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

    var expandedGameDropdown by remember { mutableStateOf(false) }
    var expandedPlayerDropdown by remember { mutableStateOf(false) }
    var expandedStatDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "➕ Yeni Bahis Ekle",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. Maç Seçimi
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedGame?.let { "${it.awayTeamName} @ ${it.homeTeamName}" } ?: "Maç Seçiniz",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedGameDropdown = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.White,
                            disabledBorderColor = Color(0xFF444444)
                        )
                    )
                    DropdownMenu(
                        expanded = expandedGameDropdown,
                        onDismissRequest = { expandedGameDropdown = false }
                    ) {
                        games.forEach { game ->
                            DropdownMenuItem(
                                text = { Text("${game.awayTeamName} @ ${game.homeTeamName} (${game.quarterStatus})") },
                                onClick = {
                                    selectedGame = game
                                    selectedPlayer = null
                                    expandedGameDropdown = false
                                    onGameSelected(game)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Oyuncu Seçimi
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedPlayer?.let { "#${it.jersey} ${it.name}" } ?: "Oyuncu Seçiniz",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (selectedGame != null) expandedPlayerDropdown = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.White,
                            disabledBorderColor = Color(0xFF444444)
                        )
                    )
                    DropdownMenu(
                        expanded = expandedPlayerDropdown,
                        onDismissRequest = { expandedPlayerDropdown = false }
                    ) {
                        if (players.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Oyuncu listesi yükleniyor...") },
                                onClick = { expandedPlayerDropdown = false }
                            )
                        } else {
                            players.forEach { player ->
                                DropdownMenuItem(
                                    text = { Text("#${player.jersey} ${player.name}") },
                                    onClick = {
                                        selectedPlayer = player
                                        expandedPlayerDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. İstatistik Türü
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedStat,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedStatDropdown = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color(0xFF00B4D8),
                            disabledBorderColor = Color(0xFF444444)
                        )
                    )
                    DropdownMenu(
                        expanded = expandedStatDropdown,
                        onDismissRequest = { expandedStatDropdown = false }
                    ) {
                        BetRepository.STAT_OPTIONS.forEach { stat ->
                            DropdownMenuItem(
                                text = { Text(stat) },
                                onClick = {
                                    selectedStat = stat
                                    expandedStatDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Hedef Barem & ÜST/ALT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = targetValue,
                        onValueChange = { targetValue = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Barem (örn: 24.5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Button(
                        onClick = { typeValue = if (typeValue == "ÜST") "ALT" else "ÜST" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (typeValue == "ÜST") Color(0xFF28A745) else Color(0xFFDC3545)
                        )
                    ) {
                        Text(typeValue, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Butonlar
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("İptal", color = Color(0xFFAAAAAA))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val target = targetValue.toDoubleOrNull()
                            if (selectedGame != null && selectedPlayer != null && target != null) {
                                onAdd(
                                    selectedGame!!.id,
                                    selectedPlayer!!.id,
                                    selectedPlayer!!.name,
                                    selectedGame!!.awayTeamName,
                                    selectedPlayer!!.jersey,
                                    selectedStat,
                                    target,
                                    typeValue
                                )
                            }
                        },
                        enabled = selectedGame != null && selectedPlayer != null && targetValue.toDoubleOrNull() != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
                    ) {
                        Text("Kupona Ekle", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
