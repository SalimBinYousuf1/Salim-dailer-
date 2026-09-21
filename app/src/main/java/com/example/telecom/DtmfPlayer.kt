package com.example.telecom

import android.media.AudioManager
import android.media.ToneGenerator

object DtmfPlayer {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playTone(char: Char, durationMs: Int = 120) {
        val toneType = when (char) {
            '0' -> ToneGenerator.TONE_DTMF_0
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> null
        }
        toneType?.let {
            try {
                toneGenerator?.startTone(it, durationMs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopTone() {
        try {
            toneGenerator?.stopTone()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
