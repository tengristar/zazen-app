package com.example.zazentimer.model

enum class GongMode(val label: String) {
    NONE("No sound"),
    BELL("Bell"),
    GONG("Gong"),
    CHIME("Chime")
}

data class SessionPreset(val label: String, val minutes: Int)

data class MeditationRecord(
    val timestamp: Long,
    val minutes: Int,
    val presetLabel: String
)
