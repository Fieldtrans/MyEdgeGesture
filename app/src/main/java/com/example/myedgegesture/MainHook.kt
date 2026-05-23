package com.example.myedgegesture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "MyEdgeGesture"
        private const val MAX_HELD_TOUCH_EVENTS = 32
    }

    private var configReceiverRegistered = false
    @Volatile private var inputFilterRegistered = false
    @Volatile private var registeringInputFilter = false
    @Volatile private var imsInstance: Any? = null
    @Volatile private var imsClassLoader: ClassLoader? = null
    @Volatile private var systemContext: Context? = null
    private val heldTouchEvents = ArrayDeque<MotionEvent>()

    private val detector = EdgeGestureDetector(
        object : EdgeGestureDetector.Callbacks {
            override fun onGesture(
                context: Context,
                edge: EdgeGestureDetector.Edge,
                zone: String,
                gesture: String,
                startX: Float,
                startY: Float,
                x: Float,
                y: Float
            ) {
                GestureActionDispatcher.perform(
                    context = context,
                    edge = edge,
                    zone = zone,
                    gesture = gesture,
                    startX = startX,
                    startY = startY,
                    x = x,
                    y = y
                )
            }
        }
    )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.example.myedgegesture") {
            hookModuleHealth(lpparam.classLoader)
            XposedBridge.log("$TAG: app process loaded")
            return
        }

        if (lpparam.packageName == "android") {
            XposedBridge.log("$TAG: loaded")
            hookInputManager(lpparam.classLoader)
        }
    }

    private fun hookModuleHealth(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.example.myedgegesture.HookHealth",
                classLoader,
                "isModuleLoaded",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return true
                    }
                }
            )
            XposedBridge.log("$TAG: hooked module health")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: module health hook failed: ${t.message}")
        }
    }

    private fun hookInputManager(classLoader: ClassLoader) {
        try {
            val ims = XposedHelpers.findClass(
                "com.android.server.input.InputManagerService",
                classLoader
            )

            hookSetInputFilter(ims, classLoader)
            hookNativeInputFilterSwitch(classLoader)
            hookInputManagerStart(ims, classLoader)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: hookInputManager failed: ${t.message}")
        }
    }

    private fun hookSetInputFilter(ims: Class<*>, classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                ims,
                "setInputFilter",
                "android.view.IInputFilter",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (
                            param.args[0] == null &&
                            RuntimeGestureConfig.enabled &&
                            !inputFilterRegistered &&
                            !registeringInputFilter
                        ) {
                            registerInputFilter(param.thisObject, classLoader)
                        }
                    }
                }
            )

            XposedBridge.log("$TAG: hooked setInputFilter")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: setInputFilter hook failed: ${t.message}")
        }
    }

    private fun hookNativeInputFilterSwitch(classLoader: ClassLoader) {
        try {
            val nativeClass = XposedHelpers.findClass(
                "com.android.server.input.NativeInputManagerService\$NativeImpl",
                classLoader
            )

            XposedHelpers.findAndHookMethod(
                nativeClass,
                "setInputFilterEnabled",
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (RuntimeGestureConfig.enabled) {
                            param.args[0] = true
                        }
                    }
                }
            )

            XposedBridge.log("$TAG: hooked setInputFilterEnabled")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: NativeImpl hook failed: ${t.message}")
        }
    }

    private fun hookInputManagerStart(ims: Class<*>, classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                ims,
                "start",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        imsInstance = param.thisObject
                        imsClassLoader = classLoader

                        val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                        systemContext = context

                        loadSavedConfig(context)
                        DebugLog.markStatus(context, GestureConfig.KEY_STATUS_LOADED_AT, "system_server loaded")
                        registerConfigReceiver(context)

                        if (RuntimeGestureConfig.enabled) {
                            enableNativeInputFilter(param.thisObject)
                            registerInputFilter(param.thisObject, classLoader)
                            DebugLog.markStatus(context, GestureConfig.KEY_STATUS_STARTED_AT, "input filter enabled")
                        } else {
                            disableNativeInputFilter(param.thisObject)
                        }
                    }
                }
            )

            XposedBridge.log("$TAG: hooked InputManagerService.start")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: start hook failed: ${t.message}")
        }
    }

    private fun registerInputFilter(imsInstance: Any, classLoader: ClassLoader) {
        if (inputFilterRegistered || registeringInputFilter) return

        try {
            registeringInputFilter = true
            val filterClass = XposedHelpers.findClass("android.view.IInputFilter", classLoader)
            val hostClass = XposedHelpers.findClass("android.view.IInputFilterHost", classLoader)
            val sendInputEvent = hostClass.getMethod(
                "sendInputEvent",
                InputEvent::class.java,
                Int::class.javaPrimitiveType
            )

            var host: Any? = null
            val filter = Proxy.newProxyInstance(
                classLoader,
                arrayOf(filterClass)
            ) { _, method, args ->
                when (method.name) {
                    "install" -> {
                        host = args?.getOrNull(0)
                        null
                    }

                    "uninstall" -> {
                        host = null
                        inputFilterRegistered = false
                        resetInputState()
                        null
                    }

                    "filterInputEvent" -> {
                        val event = args?.getOrNull(0) as? InputEvent
                        val policyFlags = args?.getOrNull(1) as? Int ?: 0
                        if (event != null) {
                            handleFilterEvent(host, sendInputEvent, event, policyFlags)
                        }

                        null
                    }

                    else -> null
                }
            }

            XposedHelpers.callMethod(imsInstance, "setInputFilter", filter)
            inputFilterRegistered = true
            XposedBridge.log("$TAG: input filter registered")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: input filter failed: ${t.message}")
        } finally {
            registeringInputFilter = false
        }
    }

    private fun handleFilterEvent(
        host: Any?,
        sendInputEvent: Method,
        event: InputEvent,
        policyFlags: Int
    ) {
        val decision = handleInputEvent(event)
        val consumed = decision == EdgeGestureDetector.Decision.HOLD ||
            decision == EdgeGestureDetector.Decision.CONSUME ||
            decision == EdgeGestureDetector.Decision.DROP_HELD_AND_CONSUME
        when (decision) {
            EdgeGestureDetector.Decision.FORWARD -> {
                forwardInputEvent(host, sendInputEvent, event, policyFlags)
            }

            EdgeGestureDetector.Decision.HOLD -> {
                holdTouchEvent(event)
            }

            EdgeGestureDetector.Decision.CONSUME -> Unit

            EdgeGestureDetector.Decision.REPLAY_HELD_THEN_FORWARD -> {
                replayHeldTouchEvents(host, sendInputEvent, policyFlags)
                forwardInputEvent(host, sendInputEvent, event, policyFlags)
            }

            EdgeGestureDetector.Decision.DROP_HELD_AND_CONSUME -> {
                recycleHeldTouchEvents()
            }
        }
    }

    private fun forwardInputEvent(
        host: Any?,
        sendInputEvent: Method,
        event: InputEvent,
        policyFlags: Int
    ) {
        if (host == null) return

        try {
            sendInputEvent.invoke(host, event, policyFlags)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: forward input failed: ${t.message}")
        }
    }

    private fun holdTouchEvent(event: InputEvent) {
        if (event !is MotionEvent) return
        if (heldTouchEvents.size >= MAX_HELD_TOUCH_EVENTS) {
            heldTouchEvents.removeFirst().recycle()
        }
        heldTouchEvents.add(MotionEvent.obtain(event))
    }

    private fun replayHeldTouchEvents(
        host: Any?,
        sendInputEvent: Method,
        policyFlags: Int
    ) {
        while (heldTouchEvents.isNotEmpty()) {
            val held = heldTouchEvents.removeFirst()
            forwardInputEvent(host, sendInputEvent, held, policyFlags)
            held.recycle()
        }
    }

    private fun recycleHeldTouchEvents() {
        while (heldTouchEvents.isNotEmpty()) {
            heldTouchEvents.removeFirst().recycle()
        }
    }

    private fun handleInputEvent(event: InputEvent): EdgeGestureDetector.Decision {
        if (InputInjectionGuard.isIgnoring()) return EdgeGestureDetector.Decision.FORWARD
        if (event !is MotionEvent) return EdgeGestureDetector.Decision.FORWARD
        if (event.source and InputDevice.SOURCE_TOUCHSCREEN != InputDevice.SOURCE_TOUCHSCREEN) {
            return EdgeGestureDetector.Decision.FORWARD
        }

        val context = systemContext ?: return EdgeGestureDetector.Decision.FORWARD
        if (!DeviceState.canRunGestures(context)) {
            resetInputState()
            return EdgeGestureDetector.Decision.FORWARD
        }

        return try {
            if (OneHandPointer.isActive() && OneHandPointer.handleMotion(context, event)) {
                EdgeGestureDetector.Decision.CONSUME
            } else if (RuntimeGestureConfig.enabled) {
                detector.handle(context, event)
            } else {
                resetInputState()
                EdgeGestureDetector.Decision.FORWARD
            }
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: input handling failed: ${t.message}")
            resetInputState()
            EdgeGestureDetector.Decision.FORWARD
        }
    }

    private fun enableNativeInputFilter(imsInstance: Any) {
        try {
            val native = XposedHelpers.getObjectField(imsInstance, "mNative")
            XposedHelpers.callMethod(native, "setInputFilterEnabled", true)
            XposedBridge.log("$TAG: input filter enabled")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: enable input filter failed: ${t.message}")
        }
    }

    private fun disableNativeInputFilter(imsInstance: Any) {
        try {
            val native = XposedHelpers.getObjectField(imsInstance, "mNative")
            XposedHelpers.callMethod(native, "setInputFilterEnabled", false)
            XposedBridge.log("$TAG: input filter disabled")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: disable input filter failed: ${t.message}")
        }
    }

    private fun disableInputFilter() {
        val currentIms = imsInstance ?: return

        try {
            inputFilterRegistered = false
            disableNativeInputFilter(currentIms)
            XposedBridge.log("$TAG: input filter removed")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: remove input filter failed: ${t.message}")
        }
    }

    private fun registerConfigReceiver(context: Context) {
        if (configReceiverRegistered) return
        configReceiverRegistered = true

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_USER_UNLOCKED) {
                    loadSavedConfig(receiverContext)
                    applyRuntimeConfig(receiverContext)
                    DebugLog.markStatus(receiverContext, GestureConfig.KEY_STATUS_STARTED_AT, "config reloaded after unlock")
                    return
                }

                if (intent.action == Intent.ACTION_SCREEN_OFF ||
                    intent.action == Intent.ACTION_USER_PRESENT ||
                    intent.action == Intent.ACTION_USER_BACKGROUND
                ) {
                    resetInputState()
                    DebugLog.info("input state reset by ${intent.action}")
                    return
                }

                if (intent.action != GestureConfig.ACTION_CONFIG_CHANGED) return

                RuntimeGestureConfig.updateFromIntent(intent)
                applyRuntimeConfig(receiverContext)

                XposedBridge.log(
                    "$TAG: config updated enabled=${RuntimeGestureConfig.enabled} " +
                        "edge=${RuntimeGestureConfig.edgeWidthDp} " +
                        "distance=${RuntimeGestureConfig.swipeDistanceDp}"
                )
                DebugLog.markStatus(receiverContext, GestureConfig.KEY_STATUS_STARTED_AT, "config updated")
            }
        }

        try {
            val filter = IntentFilter().apply {
                addAction(GestureConfig.ACTION_CONFIG_CHANGED)
                addAction(Intent.ACTION_USER_UNLOCKED)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_USER_BACKGROUND)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            XposedBridge.log("$TAG: config receiver registered")
        } catch (t: Throwable) {
            configReceiverRegistered = false
            XposedBridge.log("$TAG: config receiver failed: ${t.message}")
        }
    }

    private fun loadSavedConfig(context: Context) {
        try {
            val moduleContext = context.createPackageContext(
                "com.example.myedgegesture",
                Context.CONTEXT_IGNORE_SECURITY
            )
            val devicePrefs = moduleContext.createDeviceProtectedStorageContext()
                .getSharedPreferences(GestureConfig.PREFS_NAME, Context.MODE_PRIVATE)
            val normalPrefs = runCatching {
                moduleContext.getSharedPreferences(GestureConfig.PREFS_NAME, Context.MODE_PRIVATE)
            }.getOrNull()
            val prefs = chooseConfigPrefs(devicePrefs, normalPrefs)
            RuntimeGestureConfig.updateFromPreferences(prefs)
            applyLegacyConfigFallback(prefs)
            XposedBridge.log(
                "$TAG: saved config loaded enabled=${RuntimeGestureConfig.enabled} " +
                    "updatedAt=${prefs.getLong(GestureConfig.KEY_CONFIG_UPDATED_AT, 0L)}"
            )
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: saved config load skipped: ${t.message}")
        }
    }

    private fun chooseConfigPrefs(
        devicePrefs: android.content.SharedPreferences,
        normalPrefs: android.content.SharedPreferences?
    ): android.content.SharedPreferences {
        val deviceHasConfig = devicePrefs.contains(GestureConfig.KEY_ENABLED)
        val normalHasConfig = normalPrefs?.contains(GestureConfig.KEY_ENABLED) == true

        if (!deviceHasConfig && normalHasConfig) return normalPrefs
        if (deviceHasConfig && !normalHasConfig) return devicePrefs
        if (normalPrefs == null) return devicePrefs

        val deviceUpdatedAt = devicePrefs.getLong(GestureConfig.KEY_CONFIG_UPDATED_AT, 0L)
        val normalUpdatedAt = normalPrefs.getLong(GestureConfig.KEY_CONFIG_UPDATED_AT, 0L)

        return if (normalUpdatedAt >= deviceUpdatedAt) normalPrefs else devicePrefs
    }

    private fun applyLegacyConfigFallback(prefs: android.content.SharedPreferences) {
        val hasTimestamp = prefs.getLong(GestureConfig.KEY_CONFIG_UPDATED_AT, 0L) > 0L
        if (!hasTimestamp && !RuntimeGestureConfig.enabled) {
            RuntimeGestureConfig.enabled = true
            XposedBridge.log("$TAG: legacy config has no timestamp; enabling default gesture")
        }
    }

    private fun applyRuntimeConfig(context: Context) {
        if (RuntimeGestureConfig.enabled) {
            val currentIms = imsInstance
            val currentLoader = imsClassLoader
            if (currentIms != null && currentLoader != null) {
                enableNativeInputFilter(currentIms)
                registerInputFilter(currentIms, currentLoader)
            }
        } else {
            resetInputState()
            disableInputFilter()
        }
    }

    private fun resetInputState() {
        OneHandPointer.cancel()
        detector.reset()
        recycleHeldTouchEvents()
    }
}
