package com.example.myedgegesture

import android.content.Context
import android.hardware.input.InputManager
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import de.robv.android.xposed.XposedBridge

object GestureActionDispatcher {

    fun perform(
        context: Context,
        edge: EdgeGestureDetector.Edge,
        zone: String,
        gesture: String,
        startX: Float,
        startY: Float,
        x: Float,
        y: Float
    ) {
        val action = RuntimeGestureConfig.actionFor(edge, gesture)
        DebugLog.info("dispatch edge=$edge zone=$zone gesture=$gesture action=$action")

        if (!DeviceState.canRunGestures(context)) {
            DebugLog.info("gesture ignored because device is locked or screen is off")
            return
        }

        when (action) {
            GestureConfig.ACTION_BACK -> injectSystemKey(context, KeyEvent.KEYCODE_BACK, "back")
            GestureConfig.ACTION_HOME -> injectSystemKey(context, KeyEvent.KEYCODE_HOME, "home")
            GestureConfig.ACTION_RECENTS -> injectSystemKey(context, KeyEvent.KEYCODE_APP_SWITCH, "recents")
            GestureConfig.ACTION_ONE_HAND_TAP -> OneHandPointer.start(context, x, y, x, y)
            else -> DebugLog.info("no action mapped")
        }
    }

    private fun injectSystemKey(context: Context, keyCode: Int, actionName: String) {
        try {
            val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
            val injectMethod = inputManager.javaClass.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.javaPrimitiveType
            )
            val now = SystemClock.uptimeMillis()

            listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
                val event = KeyEvent(
                    now,
                    now,
                    action,
                    keyCode,
                    0,
                    0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_FROM_SYSTEM,
                    InputDevice.SOURCE_KEYBOARD
                )
                InputInjectionGuard.runIgnoring {
                    injectMethod.invoke(inputManager, event, 0)
                }
            }
            XposedBridge.log("MyEdgeGesture: action -> $actionName")
        } catch (t: Throwable) {
            XposedBridge.log("MyEdgeGesture: injectSystemKey failed: ${t.message}")
        }
    }
}
