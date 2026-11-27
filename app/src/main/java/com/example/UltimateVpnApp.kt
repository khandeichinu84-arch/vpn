package com.example

import android.app.Application
import com.example.data.VpnRepository

class UltimateVpnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize installed apps for split tunneling
        VpnRepository.loadInstalledApps(packageManager)
    }
}
