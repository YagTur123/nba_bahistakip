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
    val jerseyNumber: Int,
    val statType: String,
    val statDisplay: String,
    val target: Double,
    val betType: String, // "ÜST" or "ALT"
    val createdAt: Long = System.currentTimeMillis(),
    val posX: Int = 0,
    val posY: Int = 0
)
