package com.yagsports.nbalivebets.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bets")
data class BetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String,
    val playerId: Int,
    val playerName: String,
    val playerTeam: String,
    val jerseyNumber: String = "0",
    val statKey: String,
    val statDisplay: String,
    val target: Double,
    val betType: String, // 'ÜST' veya 'ALT'
    val currentValue: Int = 0,
    val isWon: Boolean = false,
    val isLost: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun percentage(): Int = Math.min(((currentValue / target) * 100).toInt(), 100)

    fun statusText(): String = when {
        isWon -> "KAZANDI"
        isLost -> "KAYBETTİ"
        else -> "CANLI"
    }
}
