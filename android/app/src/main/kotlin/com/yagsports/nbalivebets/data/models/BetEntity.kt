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
    val statKey: String = "",
    val statDisplay: String,
    val target: Double,
    val betType: String, // "ÜST" veya "ALT"
    val currentValue: Int = 0,
    val isWon: Boolean = false,
    val isLost: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val jersey: String get() = jerseyNumber
    val stat: String get() = statDisplay
    val type: String get() = betType
    val current: Int get() = currentValue
    val percentage: Double get() = if (target > 0) Math.min((currentValue.toDouble() / target) * 100.0, 100.0) else 0.0
    fun percentage(): Int = percentage.toInt()
    fun statusText(): String = when {
        isWon -> "KAZANDI"
        isLost -> "KAYBETTİ"
        else -> "CANLI"
    }
}
