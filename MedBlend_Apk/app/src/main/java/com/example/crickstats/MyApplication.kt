package com.example.crickstats

import android.app.Application
import android.media.Ringtone

class MyApplication : Application() {
    private var currentRingtone: Ringtone? = null

    fun stopCurrentAlarm() {
        currentRingtone?.stop()
        currentRingtone = null
    }

    fun setCurrentRingtone(ringtone: Ringtone) {
        currentRingtone = ringtone
    }
}
