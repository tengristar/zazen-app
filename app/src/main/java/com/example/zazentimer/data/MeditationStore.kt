package com.example.zazentimer.data

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import com.example.zazentimer.model.GongMode
import com.example.zazentimer.model.MeditationRecord

private const val PREFS_NAME = "zazen_timer_prefs"
private const val PREF_HISTORY = "meditation_history"
private const val PREF_GONG = "selected_gong"

class MeditationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadHistory(): List<MeditationRecord> {
        val raw = prefs.getString(PREF_HISTORY, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()

        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|", limit = 3)
                if (parts.size != 3) return@mapNotNull null
                runCatching {
                    MeditationRecord(
                        timestamp = parts[0].toLong(),
                        minutes = parts[1].toInt(),
                        presetLabel = parts[2]
                    )
                }.getOrNull()
            }
            .toList()
    }

    fun saveHistory(records: List<MeditationRecord>) {
        val encoded = records.joinToString("\n") {
            "${it.timestamp}|${it.minutes}|${it.presetLabel.replace('|', ' ')}"
        }
        prefs.edit().putString(PREF_HISTORY, encoded).apply()
    }

    fun loadGong(): GongMode {
        val saved = prefs.getString(PREF_GONG, GongMode.BELL.name) ?: GongMode.BELL.name
        return runCatching { GongMode.valueOf(saved) }.getOrDefault(GongMode.BELL)
    }

    fun saveGong(mode: GongMode) {
        prefs.edit().putString(PREF_GONG, mode.name).apply()
    }

    fun playSelectedGong(mode: GongMode) {
        if (mode == GongMode.NONE) return

        val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 90)
        try {
            when (mode) {
                GongMode.NONE -> Unit
                GongMode.BELL -> toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
                GongMode.GONG -> toneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE, 800)
                GongMode.CHIME -> toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 350)
            }
        } catch (_: Exception) {
            // Ignore unsupported devices / tones.
        } finally {
            toneGenerator.release()
        }
    }
}
