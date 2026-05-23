package com.example.myedgegesture

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager

object DeviceState {
    fun canRunGestures(context: Context): Boolean {
        if (!isScreenInteractive(context)) return false

        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            ?: return true

        if (keyguard.isKeyguardLocked) return false
        return runCatching { keyguard.isDeviceLocked }.getOrDefault(false).not()
    }

    private fun isScreenInteractive(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return true
        return power.isInteractive
    }
}
