package com.example.schedify

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val time: String,
    val day: String,
    val location: String = "",
    val color: Int = 0xFF2196F3.toInt() // Default Blue
)