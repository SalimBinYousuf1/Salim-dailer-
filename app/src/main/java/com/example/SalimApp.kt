package com.example

import android.app.Application
import com.example.data.local.SalimDatabase
import com.example.telecom.CallManager
import com.example.telecom.CallNotificationHelper

class SalimApp : Application() {

    lateinit var database: SalimDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = SalimDatabase.getDatabase(this)
        CallManager.init(this)
        CallNotificationHelper.createNotificationChannels(this)
    }
}
