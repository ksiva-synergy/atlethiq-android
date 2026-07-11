package com.example.atlethiq

import android.app.Application
import com.example.atlethiq.notifications.CallNotifier
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AtlethiqApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CallNotifier.ensureChannel(this)
    }
}
