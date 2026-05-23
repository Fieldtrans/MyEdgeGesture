package com.example.myedgegesture

import android.content.Context
import android.os.SystemClock
import de.robv.android.xposed.XposedBridge

object DebugLog {
    private const val TAG = "MyEdgeGesture"

    fun info(message: String) {
        XposedBridge.log("$TAG: $message")
    }

    fun always(message: String) {
        XposedBridge.log("$TAG: $message")
    }

    fun markStatus(context: Context, key: String, message: String) {
        try {
            val moduleContext = context.createPackageContext(
                "com.example.myedgegesture",
                Context.CONTEXT_IGNORE_SECURITY
            )
            moduleContext.createDeviceProtectedStorageContext()
                .getSharedPreferences(GestureConfig.STATUS_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(key, SystemClock.elapsedRealtime())
                .putString(GestureConfig.KEY_STATUS_LAST_MESSAGE, message)
                .commit()
        } catch (t: Throwable) {
            always("status write failed: ${t.message}")
        }
    }
}
