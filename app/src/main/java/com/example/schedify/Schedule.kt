package com.example.schedify

data class Schedule(
    val id: Int,
    val title: String,
    val time: String,
    val day: String // Tambahkan field hari
)