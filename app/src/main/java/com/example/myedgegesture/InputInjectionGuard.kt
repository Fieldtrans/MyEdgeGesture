package com.example.myedgegesture

import android.os.SystemClock

object InputInjectionGuard {
    private val ignoring = ThreadLocal.withInitial { false }
    @Volatile private var ignoreUntil = 0L

    fun isIgnoring(): Boolean {
        return ignoring.get() == true || SystemClock.uptimeMillis() < ignoreUntil
    }

    fun ignoreFor(durationMs: Long) {
        ignoreUntil = maxOf(ignoreUntil, SystemClock.uptimeMillis() + durationMs)
    }

    fun <T> runIgnoring(block: () -> T): T {
        val oldValue = ignoring.get()
        ignoring.set(true)
        return try {
            block()
        } finally {
            ignoring.set(oldValue)
        }
    }
}
