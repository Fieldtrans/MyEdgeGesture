package com.example.myedgegesture

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.view.Gravity
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import de.robv.android.xposed.XposedBridge
import kotlin.math.max
import kotlin.math.sqrt

object OneHandPointer {
    private var mainHandler: Handler? = null
    private var session: Session? = null

    private data class Session(
        val overlay: PointerOverlay,
        val width: Float,
        val height: Float,
        val margin: Float,
        var anchorX: Float,
        var anchorY: Float,
        val controlRadius: Float,
        val controlAlpha: Int,
        val sensitivity: Float,
        val arrowSize: Float,
        val lineWidth: Float,
        val trackerBallSize: Float,
        val cancelDistance: Float,
        val touchArea: Float,
        val color: Int,
        val startupLineLength: Float,
        val startupInset: Float,
        val maxFingerStep: Float,
        val maxPointerSpeed: Float,
        val smoothing: Float,
        val controlStyle: String,
        val cursorStartX: Float,
        val cursorStartY: Float,
        var controlOriginX: Float,
        var controlOriginY: Float,
        var lastFingerX: Float,
        var lastFingerY: Float,
        var basePointerX: Float,
        var basePointerY: Float,
        var pointerX: Float,
        var pointerY: Float,
        var targetX: Float,
        var targetY: Float,
        var waitingForControlOrigin: Boolean = true,
        var settleMoveCount: Int = CONTROL_START_SKIP_MOVES,
        var framePosted: Boolean = false,
        var lastFrameTimeNanos: Long = 0L,
        var timeoutRunnable: Runnable? = null
    )

    fun isActive(): Boolean {
        return session != null
    }

    fun cancel() {
        session?.let { current ->
            cancelTimeout(current)
            current.overlay.dismiss()
        }
        session = null
    }

    fun start(
        context: Context,
        anchorX: Float,
        anchorY: Float,
        fingerX: Float,
        fingerY: Float
    ): Boolean {
        if (session != null) return true

        val bounds = screenBounds(context)
        val overlay = PointerOverlay(context)
        val margin = dp(context, RuntimeGestureConfig.pointerMarginDp.toFloat())
        val controlRadius = dp(context, RuntimeGestureConfig.pointerRadiusDp.toFloat())
        val controlAlpha = RuntimeGestureConfig.pointerControlAlpha.coerceIn(0, 255)
        val sensitivity = RuntimeGestureConfig.pointerSensitivity / 100f
        val arrowSize = dp(context, RuntimeGestureConfig.pointerArrowDp.toFloat())
        val lineWidth = dp(context, RuntimeGestureConfig.pointerLineDp.toFloat())
        val trackerBallSize = dp(context, RuntimeGestureConfig.trackerBallDp.toFloat())
        val cancelDistance = dp(context, RuntimeGestureConfig.pointerCancelDistanceDp.toFloat())
        val controlStyle = RuntimeGestureConfig.pointerControlStyle
        val touchArea = if (controlStyle == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
            dp(context, RuntimeGestureConfig.trackerCursorDp.toFloat())
        } else {
            dp(context, RuntimeGestureConfig.pointerTouchAreaDp.toFloat())
        }
        val smoothingPercent = if (controlStyle == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
            RuntimeGestureConfig.trackerSmoothing
        } else {
            RuntimeGestureConfig.pointerSmoothing
        }
        val smoothing = (smoothingPercent.coerceIn(5, 90) / 100f)
        val maxSpeedPercent = if (controlStyle == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
            RuntimeGestureConfig.trackerMaxSpeed
        } else {
            RuntimeGestureConfig.pointerMaxSpeed
        }
        val speedScale = maxSpeedPercent.coerceIn(40, 500) / 100f
        val maxFingerStep = dp(context, MAX_FINGER_STEP_DP) * speedScale
        val maxPointerSpeed = bounds.second * speedScale
        val pointerColor = Color.rgb(
            RuntimeGestureConfig.pointerColorRed.coerceIn(0, 255),
            RuntimeGestureConfig.pointerColorGreen.coerceIn(0, 255),
            RuntimeGestureConfig.pointerColorBlue.coerceIn(0, 255)
        )
        val startX = anchorX.coerceIn(margin, bounds.first - margin)
        val startY = anchorY.coerceIn(margin, bounds.second - margin)
        val startupLineLength = maxOf(dp(context, STARTUP_LINE_LENGTH_DP), arrowSize * 4.2f, touchArea * 2.1f)
        val startupInset = maxOf(dp(context, STARTUP_INSET_DP), arrowSize * 1.4f)
        val initialPointer = if (controlStyle == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
            trackerCursorStart(bounds.first, bounds.second, startX, margin)
        } else {
            startupPointerFor(
                width = bounds.first,
                height = bounds.second,
                margin = margin,
                anchorX = startX,
                anchorY = startY,
                lineLength = startupLineLength,
                inset = startupInset
            )
        }

        val newSession = Session(
            overlay = overlay,
            width = bounds.first,
            height = bounds.second,
            margin = margin,
            anchorX = startX,
            anchorY = startY,
            controlRadius = controlRadius,
            controlAlpha = controlAlpha,
            sensitivity = sensitivity,
            arrowSize = arrowSize,
            lineWidth = lineWidth,
            trackerBallSize = trackerBallSize,
            cancelDistance = cancelDistance,
            touchArea = touchArea,
            color = pointerColor,
            startupLineLength = startupLineLength,
            startupInset = startupInset,
            maxFingerStep = maxFingerStep,
            maxPointerSpeed = maxPointerSpeed,
            smoothing = smoothing,
            controlStyle = controlStyle,
            cursorStartX = initialPointer.first,
            cursorStartY = initialPointer.second,
            controlOriginX = fingerX,
            controlOriginY = fingerY,
            lastFingerX = fingerX,
            lastFingerY = fingerY,
            basePointerX = initialPointer.first,
            basePointerY = initialPointer.second,
            pointerX = initialPointer.first,
            pointerY = initialPointer.second,
            targetX = initialPointer.first,
            targetY = initialPointer.second
        )
        session = newSession

        overlay.show(
            newSession.anchorX,
            newSession.anchorY,
            controlRadius,
            controlAlpha,
            arrowSize,
            lineWidth,
            newSession.trackerBallSize,
            touchArea,
            pointerColor,
            newSession.pointerX,
            newSession.pointerY,
            newSession.controlStyle
        )
        scheduleTimeout(newSession)
        DebugLog.info("one hand pointer started")
        return true
    }

    fun handleMotion(context: Context, event: MotionEvent): Boolean {
        return handleRawMotion(context, event.actionMasked, event.rawX, event.rawY, event.eventTime)
    }

    fun handleRawMotion(
        context: Context,
        actionMasked: Int,
        rawX: Float,
        rawY: Float,
        eventTime: Long
    ): Boolean {
        val current = session ?: return false

        return when (actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                movePointer(current, rawX, rawY, eventTime)
                true
            }
            MotionEvent.ACTION_UP -> {
                finish(context, current, click = true)
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                finish(context, current, click = false)
                true
            }
            else -> true
        }
    }

    private fun movePointer(
        current: Session,
        fingerX: Float,
        fingerY: Float,
        eventTime: Long
    ) {
        movePointerByJoystick(current, fingerX, fingerY, eventTime)
        scheduleAnimationFrame(current)
    }

    private fun movePointerByJoystick(
        current: Session,
        fingerX: Float,
        fingerY: Float,
        eventTime: Long
    ) {
        if (current.waitingForControlOrigin) {
            current.controlOriginX = fingerX
            current.controlOriginY = fingerY
            current.lastFingerX = fingerX
            current.lastFingerY = fingerY
            current.basePointerX = current.pointerX
            current.basePointerY = current.pointerY
            current.targetX = current.pointerX
            current.targetY = current.pointerY
            current.waitingForControlOrigin = false
            current.settleMoveCount = CONTROL_START_SKIP_MOVES
            updateTrackerAnchor(current, fingerX, fingerY)
            current.overlay.moveTo(
                current.anchorX,
                current.anchorY,
                current.controlRadius,
                current.controlAlpha,
                current.arrowSize,
                current.lineWidth,
                current.trackerBallSize,
                current.touchArea,
                current.color,
                current.pointerX,
                current.pointerY,
                current.controlStyle
            )
            DebugLog.info("pointer control origin set x=$fingerX y=$fingerY")
            return
        }

        val rawDx = fingerX - current.lastFingerX
        val rawDy = fingerY - current.lastFingerY
        current.lastFingerX = fingerX
        current.lastFingerY = fingerY
        updateTrackerAnchor(current, fingerX, fingerY)

        if (current.settleMoveCount > 0) {
            current.settleMoveCount -= 1
            return
        }

        val (dx, dy) = limitedDelta(rawDx, rawDy, current.maxFingerStep)
        val sensitivity = effectiveSensitivity(current)
        current.targetX = (current.targetX + dx * sensitivity)
            .coerceIn(current.margin, current.width - current.margin)
        current.targetY = (current.targetY + dy * sensitivity)
            .coerceIn(current.margin, current.height - current.margin)
    }

    private fun updateTrackerAnchor(current: Session, fingerX: Float, fingerY: Float) {
        if (current.controlStyle != GestureConfig.POINTER_STYLE_TRACKER_CURSOR) return

        current.anchorX = fingerX.coerceIn(0f, current.width)
        current.anchorY = fingerY.coerceIn(0f, current.height)
    }

    private fun effectiveSensitivity(current: Session): Float {
        val base = if (current.controlStyle == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
            RuntimeGestureConfig.trackerSensitivity / 100f
        } else {
            current.sensitivity
        }.coerceAtLeast(0.1f)
        val speedGain = (current.maxPointerSpeed / current.height).coerceIn(0.4f, 5f)
        return if (current.controlStyle == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
            base * TRACKER_CURSOR_GAIN * speedGain
        } else {
            base * LINE_ARROW_GAIN * speedGain
        }
    }

    private fun limitedDelta(dx: Float, dy: Float, maxStep: Float): Pair<Float, Float> {
        val distance = sqrt(dx * dx + dy * dy)
        if (distance <= maxStep || distance <= 0f) return dx to dy

        val ratio = maxStep / distance
        return dx * ratio to dy * ratio
    }

    private fun trackerCursorStart(
        width: Float,
        height: Float,
        anchorX: Float,
        margin: Float
    ): Pair<Float, Float> {
        val x = if (anchorX >= width / 2f) {
            width * TRACKER_CURSOR_START_X_FROM_RIGHT
        } else {
            width * TRACKER_CURSOR_START_X_FROM_LEFT
        }
        val y = height * TRACKER_CURSOR_START_Y

        return x.coerceIn(margin, width - margin) to y.coerceIn(margin, height - margin)
    }

    private fun startupPointerFor(
        width: Float,
        height: Float,
        margin: Float,
        anchorX: Float,
        anchorY: Float,
        lineLength: Float,
        inset: Float
    ): Pair<Float, Float> {
        val safeMargin = max(margin, lineLength * STARTUP_SAFE_MARGIN_FACTOR)
        val minX = safeMargin.coerceAtMost(width / 2f)
        val maxX = (width - safeMargin).coerceAtLeast(width / 2f)
        val minY = safeMargin.coerceAtMost(height / 2f)
        val maxY = (height - safeMargin).coerceAtLeast(height / 2f)
        val left = anchorX
        val right = width - anchorX
        val top = anchorY
        val bottom = height - anchorY
        val minDistance = minOf(left, right, top, bottom)
        val canGoUp = anchorY - lineLength >= minY
        val canGoDown = anchorY + lineLength <= maxY
        val verticalDirection = when {
            canGoUp -> -1f
            canGoDown -> 1f
            top > bottom -> -1f
            else -> 1f
        }

        val rawX: Float
        val rawY: Float
        when (minDistance) {
            left -> {
                rawX = anchorX + inset
                rawY = anchorY + verticalDirection * lineLength
            }
            right -> {
                rawX = anchorX - inset
                rawY = anchorY + verticalDirection * lineLength
            }
            top -> {
                rawX = anchorX
                rawY = anchorY + lineLength
            }
            else -> {
                rawX = anchorX
                rawY = anchorY - lineLength
            }
        }

        return rawX.coerceIn(minX, maxX) to rawY.coerceIn(minY, maxY)
    }

    private fun scheduleAnimationFrame(current: Session) {
        if (current.framePosted) return
        current.framePosted = true
        postToMain {
            if (session !== current) {
                current.framePosted = false
                return@postToMain
            }
            try {
                Choreographer.getInstance().postFrameCallback { frameTimeNanos ->
                    current.framePosted = false
                    if (session !== current) return@postFrameCallback
                    val keepGoing = advanceFrame(current, frameTimeNanos)
                    if (keepGoing && session === current) {
                        scheduleAnimationFrame(current)
                    }
                }
            } catch (_: Throwable) {
                current.framePosted = false
                mainHandler?.postDelayed({
                    if (session !== current) return@postDelayed
                    val keepGoing = advanceFrame(current, System.nanoTime())
                    if (keepGoing && session === current) {
                        scheduleAnimationFrame(current)
                    }
                }, FRAME_FALLBACK_DELAY_MS)
            }
        }
    }

    private fun advanceFrame(current: Session, frameTimeNanos: Long): Boolean {
        val previousFrameTime = current.lastFrameTimeNanos
        current.lastFrameTimeNanos = frameTimeNanos

        val dx = current.targetX - current.pointerX
        val dy = current.targetY - current.pointerY
        val distance = sqrt(dx * dx + dy * dy)
        if (distance <= 0.5f) {
            current.pointerX = current.targetX
            current.pointerY = current.targetY
            current.overlay.moveTo(
                current.anchorX,
                current.anchorY,
                current.controlRadius,
                current.controlAlpha,
                current.arrowSize,
                current.lineWidth,
                current.trackerBallSize,
                current.touchArea,
                current.color,
                current.pointerX,
                current.pointerY,
                current.controlStyle
            )
            return false
        }

        val smoothStep = current.smoothing.coerceIn(0.05f, 1f)
        val frameSeconds = if (previousFrameTime > 0L) {
            ((frameTimeNanos - previousFrameTime).coerceAtLeast(1L) / 1_000_000_000f)
                .coerceIn(1f / 240f, 1f / 30f)
        } else {
            1f / 120f
        }
        val maxFrameStep = max(current.maxPointerSpeed * frameSeconds, 1.5f)
        val desiredStep = minOf((distance * smoothStep).coerceAtLeast(0.5f), maxFrameStep)
        val ratio = (desiredStep / distance).coerceIn(0f, 1f)

        current.pointerX = (current.pointerX + dx * ratio).coerceIn(current.margin, current.width - current.margin)
        current.pointerY = (current.pointerY + dy * ratio).coerceIn(current.margin, current.height - current.margin)
        current.overlay.moveTo(
            current.anchorX,
            current.anchorY,
            current.controlRadius,
            current.controlAlpha,
            current.arrowSize,
            current.lineWidth,
            current.trackerBallSize,
            current.touchArea,
            current.color,
            current.pointerX,
            current.pointerY,
            current.controlStyle
        )
        return true
    }

    private fun finish(context: Context, current: Session, click: Boolean) {
        cancelTimeout(current)
        val arrowTipX = current.pointerX
        val arrowTipY = current.pointerY
        val tapTouchArea = current.touchArea
        val shouldClick = click && !shouldCancelClick(current)

        current.overlay.dismiss()
        session = null

        if (shouldClick) {
            postToMain {
                ClickFeedbackOverlay(context, arrowTipX, arrowTipY, tapTouchArea, current.color).show()
                mainHandler?.postDelayed({
                    injectTap(context.applicationContext, arrowTipX, arrowTipY, tapTouchArea)
                    DebugLog.info("tap requested at arrow tip $arrowTipX,$arrowTipY")
                }, TAP_AFTER_OVERLAY_DISMISS_MS)
            }
        } else if (click) {
            DebugLog.info("tap canceled length=${lineLength(current)}")
        }
    }

    private fun scheduleTimeout(current: Session) {
        val runnable = Runnable {
            if (session !== current) return@Runnable
            current.overlay.dismiss()
            session = null
            DebugLog.info("pointer auto canceled by timeout")
        }
        current.timeoutRunnable = runnable
        postToMain {
            mainHandler?.postDelayed(
                runnable,
                RuntimeGestureConfig.pointerTimeoutMs.coerceIn(2_000, 10_000).toLong()
            )
        }
    }

    private fun cancelTimeout(current: Session) {
        current.timeoutRunnable?.let { runnable ->
            mainHandler?.removeCallbacks(runnable)
        }
        current.timeoutRunnable = null
    }

    private fun shouldCancelClick(current: Session): Boolean {
        if (current.controlStyle == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
            return lineLength(current) <= trackerCancelRadius(current)
        }

        val length = lineLength(current)
        return length <= current.controlRadius || length < current.cancelDistance
    }

    private fun trackerCancelRadius(current: Session): Float {
        return max(current.controlRadius, current.trackerBallSize + current.touchArea)
    }

    private fun lineLength(current: Session): Float {
        val dx = current.pointerX - current.anchorX
        val dy = current.pointerY - current.anchorY
        return sqrt(dx * dx + dy * dy)
    }

    private fun cursorMoveLength(current: Session): Float {
        val dx = current.pointerX - current.cursorStartX
        val dy = current.pointerY - current.cursorStartY
        return sqrt(dx * dx + dy * dy)
    }

    private fun injectTap(context: Context, x: Float, y: Float, touchArea: Float) {
        val downTime = SystemClock.uptimeMillis()
        val moveTime = downTime + TAP_MOVE_DELAY_MS
        val upTime = downTime + TAP_DURATION_MS
        val downEvent = createTapEvent(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, touchArea)
        val moveEvent = createTapEvent(downTime, moveTime, MotionEvent.ACTION_MOVE, x, y, touchArea)
        val upEvent = createTapEvent(downTime, upTime, MotionEvent.ACTION_UP, x, y, touchArea)

        try {
            InputInjectionGuard.ignoreFor(IGNORE_INJECTED_EVENTS_MS)
            InputInjectionGuard.runIgnoring {
                val downInjected = injectInputEvent(context, downEvent)
                val moveInjected = injectInputEvent(context, moveEvent)
                val upInjected = injectInputEvent(context, upEvent)
                DebugLog.info("tap injected down=$downInjected move=$moveInjected up=$upInjected x=$x y=$y")
            }
        } catch (t: Throwable) {
            XposedBridge.log("MyEdgeGesture: injectTap failed: ${t.message}")
        } finally {
            downEvent.recycle()
            moveEvent.recycle()
            upEvent.recycle()
        }
    }

    private fun createTapEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: Float,
        y: Float,
        touchArea: Float
    ): MotionEvent {
        val pointerProperties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
        val pointerCoords = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = if (action == MotionEvent.ACTION_UP) 0f else 0.7f
            size = (touchArea / 100f).coerceIn(0.01f, 1f)
            touchMajor = touchArea
            touchMinor = touchArea
            toolMajor = touchArea
            toolMinor = touchArea
        }

        return MotionEvent.obtain(
            downTime,
            eventTime,
            action,
            1,
            arrayOf(pointerProperties),
            arrayOf(pointerCoords),
            0,
            0,
            1f,
            1f,
            1,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        ).also { event ->
            event.setLocation(x, y)
            runCatching {
                event.javaClass.getMethod("setDisplayId", Int::class.javaPrimitiveType)
                    .invoke(event, 0)
            }
        }
    }

    private fun injectInputEvent(context: Context, event: InputEvent): Boolean {
        injectWithInputManager(context, event)?.let { return it }
        injectWithInputManagerGlobal(event)?.let { return it }
        return false
    }

    private fun injectWithInputManager(context: Context, event: InputEvent): Boolean? {
        return try {
            val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
            val injectMethod = inputManager.javaClass.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.javaPrimitiveType
            )
            (injectMethod.invoke(inputManager, event, INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH) as? Boolean)
                ?: true
        } catch (t: Throwable) {
            DebugLog.info("tap InputManager inject failed: ${t.message}")
            null
        }
    }

    private fun injectWithInputManagerGlobal(event: InputEvent): Boolean? {
        return try {
            val globalClass = Class.forName("android.hardware.input.InputManagerGlobal")
            val instance = globalClass.getMethod("getInstance").invoke(null)
            val injectMethod = globalClass.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.javaPrimitiveType
            )
            (injectMethod.invoke(instance, event, INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH) as? Boolean)
                ?: true
        } catch (t: Throwable) {
            DebugLog.info("tap InputManagerGlobal inject failed: ${t.message}")
            null
        }
    }

    private fun screenBounds(context: Context): Pair<Float, Float> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bounds = windowManager.currentWindowMetrics.bounds
        return bounds.width().toFloat() to bounds.height().toFloat()
    }

    private fun dp(context: Context, value: Float): Float {
        return value * context.resources.displayMetrics.density
    }

    private fun postToMain(block: () -> Unit) {
        val handler = mainHandler ?: Handler(Looper.getMainLooper()).also {
            mainHandler = it
        }
        if (Looper.myLooper() == handler.looper) {
            block()
        } else {
            handler.post(block)
        }
    }

    private class PointerOverlay(private val context: Context) {
        private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        private var shown = false
        private var pendingAnchorX = 0f
        private var pendingAnchorY = 0f
        private var pendingRadius = 0f
        private var pendingControlAlpha = 0
        private var pendingArrowSize = 0f
        private var pendingLineWidth = 0f
        private var pendingTrackerBallSize = 0f
        private var pendingTouchArea = 0f
        private var pendingColor = 0
        private var pendingX = 0f
        private var pendingY = 0f
        private var pendingStyle = GestureConfig.POINTER_STYLE_LINE_ARROW
        private var pendingUpdatePosted = false

        private val screenBounds = run {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        }
        private val view = PointerView(context, screenBounds.first, screenBounds.second)

        private val params = WindowManager.LayoutParams(
            screenBounds.first,
            screenBounds.second,
            2027,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        fun show(
            anchorX: Float,
            anchorY: Float,
            radius: Float,
            controlAlpha: Int,
            arrowSize: Float,
            lineWidth: Float,
            trackerBallSize: Float,
            touchArea: Float,
            color: Int,
            x: Float,
            y: Float,
            style: String
        ) {
            postToMain {
                try {
                    params.x = 0
                    params.y = 0
                    windowManager.addView(view, params)
                    shown = true
                    view.update(anchorX, anchorY, radius, controlAlpha, arrowSize, lineWidth, trackerBallSize, touchArea, color, x, y, style)
                    DebugLog.info("overlay added")
                } catch (t: Throwable) {
                    XposedBridge.log("MyEdgeGesture: show overlay failed: ${t.message}")
                }
            }
        }

        fun moveTo(
            anchorX: Float,
            anchorY: Float,
            radius: Float,
            controlAlpha: Int,
            arrowSize: Float,
            lineWidth: Float,
            trackerBallSize: Float,
            touchArea: Float,
            color: Int,
            x: Float,
            y: Float,
            style: String
        ) {
            pendingAnchorX = anchorX
            pendingAnchorY = anchorY
            pendingRadius = radius
            pendingControlAlpha = controlAlpha
            pendingArrowSize = arrowSize
            pendingLineWidth = lineWidth
            pendingTrackerBallSize = trackerBallSize
            pendingTouchArea = touchArea
            pendingColor = color
            pendingX = x
            pendingY = y
            pendingStyle = style
            runOnMainForFrame { applyPendingUpdate() }
        }

        private fun runOnMainForFrame(action: () -> Unit) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                action()
                return
            }

            if (pendingUpdatePosted) return
            pendingUpdatePosted = true
            postToMain {
                pendingUpdatePosted = false
                action()
            }
        }

        private fun applyPendingUpdate() {
            if (!shown) return

            try {
                view.update(
                    pendingAnchorX,
                    pendingAnchorY,
                    pendingRadius,
                    pendingControlAlpha,
                    pendingArrowSize,
                    pendingLineWidth,
                    pendingTrackerBallSize,
                    pendingTouchArea,
                    pendingColor,
                    pendingX,
                    pendingY,
                    pendingStyle
                )
            } catch (t: Throwable) {
                DebugLog.info("move overlay failed: ${t.message}")
            }
        }

        fun dismiss() {
            postToMain {
                if (!shown) return@postToMain

                try {
                    windowManager.removeView(view)
                } catch (_: Throwable) {
                }

                shown = false
            }
        }

        private fun dp(value: Float): Float {
            return value * context.resources.displayMetrics.density
        }

        private class PointerView(
            context: Context,
            private val screenWidth: Int,
            private val screenHeight: Int
        ) : View(context) {
            private var anchorX = 0f
            private var anchorY = 0f
            private var controlRadius = 0f
            private var controlAlpha = 0
            private var arrowSize = 0f
            private var trackerBallSize = 0f
            private var touchArea = 0f
            private var pointerX = 0f
            private var pointerY = 0f
            private var style = GestureConfig.POINTER_STYLE_LINE_ARROW
            private var visualStartX = 0f
            private var visualStartY = 0f
            private var hasDirtyBounds = false
            private var dirtyLeft = 0
            private var dirtyTop = 0
            private var dirtyRight = 0
            private var dirtyBottom = 0
            private var newDirtyLeft = 0
            private var newDirtyTop = 0
            private var newDirtyRight = 0
            private var newDirtyBottom = 0
            private val clipBounds = Rect()

            private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0, 220, 80)
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            private val controlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(85, 0, 220, 80)
                style = Paint.Style.STROKE
                strokeWidth = dp(1.5f)
            }
            private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0, 220, 80)
                style = Paint.Style.FILL
            }
            private val cursorStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = dp(2f)
            }
            fun update(
                anchorX: Float,
                anchorY: Float,
                radius: Float,
                controlAlpha: Int,
                arrowSize: Float,
                lineWidth: Float,
                trackerBallSize: Float,
                touchArea: Float,
                color: Int,
                pointerX: Float,
                pointerY: Float,
                style: String
            ) {
                val newControlAlpha = controlAlpha.coerceIn(0, 255)
                val pointerMoveX = pointerX - this.pointerX
                val pointerMoveY = pointerY - this.pointerY
                val anchorMoveX = anchorX - this.anchorX
                val anchorMoveY = anchorY - this.anchorY
                val movementEpsilon = dp(0.45f).coerceAtLeast(1f)
                val movementEpsilonSquared = movementEpsilon * movementEpsilon
                val oldLeft = dirtyLeft
                val oldTop = dirtyTop
                val oldRight = dirtyRight
                val oldBottom = dirtyBottom
                val hadOldBounds = hasDirtyBounds
                val controlChanged = !hadOldBounds ||
                        this.anchorX != anchorX ||
                        this.anchorY != anchorY ||
                        this.controlRadius != radius ||
                        this.controlAlpha != newControlAlpha ||
                        this.arrowSize != arrowSize ||
                        this.trackerBallSize != trackerBallSize ||
                        this.style != style ||
                        linePaint.strokeWidth != lineWidth ||
                        linePaint.color != color

                if (hadOldBounds &&
                    !controlChanged &&
                    pointerMoveX * pointerMoveX + pointerMoveY * pointerMoveY < movementEpsilonSquared &&
                    anchorMoveX * anchorMoveX + anchorMoveY * anchorMoveY < movementEpsilonSquared
                ) {
                    return
                }

                this.anchorX = anchorX
                this.anchorY = anchorY
                this.controlRadius = radius
                this.controlAlpha = newControlAlpha
                this.arrowSize = arrowSize
                this.trackerBallSize = trackerBallSize
                this.touchArea = touchArea
                this.style = style
                linePaint.strokeWidth = lineWidth
                linePaint.color = color
                cursorPaint.color = color
                controlPaint.color = Color.argb(
                    this.controlAlpha,
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
                )
                this.pointerX = pointerX
                this.pointerY = pointerY
                updateVisualStart(anchorX, anchorY)
                computeDirtyBounds(anchorX, anchorY, pointerX, pointerY, radius, arrowSize, lineWidth, style, controlChanged)
                dirtyLeft = newDirtyLeft
                dirtyTop = newDirtyTop
                dirtyRight = newDirtyRight
                dirtyBottom = newDirtyBottom
                hasDirtyBounds = true

                if (!hadOldBounds) {
                    invalidateDirty(newDirtyLeft, newDirtyTop, newDirtyRight, newDirtyBottom)
                } else {
                    invalidateDirty(
                        minOf(oldLeft, newDirtyLeft),
                        minOf(oldTop, newDirtyTop),
                        maxOf(oldRight, newDirtyRight),
                        maxOf(oldBottom, newDirtyBottom)
                    )
                }
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)

                if (controlAlpha > 0) {
                    if (style == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
                        drawTracker(canvas)
                    } else if (circleIntersectsClip(canvas, anchorX, anchorY, controlRadius)) {
                        canvas.drawCircle(anchorX, anchorY, controlRadius, controlPaint)
                    }
                }
                if (style == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
                    drawCursor(canvas)
                } else {
                    drawArrowLine(canvas, visualStartX, visualStartY, pointerX, pointerY)
                }
            }

            private fun drawTracker(canvas: Canvas) {
                if (circleIntersectsClip(canvas, anchorX, anchorY, controlRadius)) {
                    canvas.drawCircle(anchorX, anchorY, controlRadius, controlPaint)
                    canvas.drawCircle(anchorX, anchorY, max(trackerBallSize / 2f, dp(3f)), linePaint)
                }
            }

            private fun drawCursor(canvas: Canvas) {
                val radius = max(touchArea / 2f, dp(9f))
                if (circleIntersectsClip(canvas, pointerX, pointerY, radius)) {
                    canvas.drawCircle(pointerX, pointerY, radius, cursorPaint)
                    canvas.drawCircle(pointerX, pointerY, radius, cursorStrokePaint)
                }
            }

            private fun updateVisualStart(x: Float, y: Float) {
                val viewWidth = drawingWidth()
                val viewHeight = drawingHeight()
                val left = x
                val right = viewWidth - x
                val top = y
                val bottom = viewHeight - y
                val minDistance = minOf(left, right, top, bottom)

                when (minDistance) {
                    left -> {
                        visualStartX = 0f
                        visualStartY = y
                    }
                    right -> {
                        visualStartX = viewWidth.toFloat()
                        visualStartY = y
                    }
                    top -> {
                        visualStartX = x
                        visualStartY = 0f
                    }
                    else -> {
                        visualStartX = x
                        visualStartY = viewHeight.toFloat()
                    }
                }
            }

            private fun drawArrowLine(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float) {
                val dx = endX - startX
                val dy = endY - startY
                val length = sqrt(dx * dx + dy * dy)
                if (length < dp(12f)) return

                val ux = dx / length
                val uy = dy / length
                val arrowSize = max(this.arrowSize, dp(6f))
                val wing = arrowSize * 0.62f
                val baseX = endX - ux * arrowSize
                val baseY = endY - uy * arrowSize
                val leftX = baseX + (-uy) * wing
                val leftY = baseY + ux * wing
                val rightX = baseX - (-uy) * wing
                val rightY = baseY - ux * wing

                canvas.drawLine(startX, startY, endX, endY, linePaint)
                canvas.drawLine(endX, endY, leftX, leftY, linePaint)
                canvas.drawLine(endX, endY, rightX, rightY, linePaint)
            }

            private fun circleIntersectsClip(canvas: Canvas, cx: Float, cy: Float, radius: Float): Boolean {
                canvas.getClipBounds(clipBounds)
                val pad = max(radius, dp(2f))
                return clipBounds.intersects(
                    (cx - pad).toInt(),
                    (cy - pad).toInt(),
                    (cx + pad).toInt(),
                    (cy + pad).toInt()
                )
            }

            private fun computeDirtyBounds(
                anchorX: Float,
                anchorY: Float,
                pointerX: Float,
                pointerY: Float,
                radius: Float,
                arrowSize: Float,
                lineWidth: Float,
                style: String,
                includeControl: Boolean
            ) {
                val viewWidth = drawingWidth()
                val viewHeight = drawingHeight()
                val pad = max(max(arrowSize, lineWidth) * 2.4f, dp(18f))

                var left: Float
                var top: Float
                var right: Float
                var bottom: Float

                if (style == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
                    val cursorRadius = max(touchArea / 2f, dp(9f))
                    left = minOf(anchorX - radius, pointerX - cursorRadius) - pad
                    top = minOf(anchorY - radius, pointerY - cursorRadius) - pad
                    right = maxOf(anchorX + radius, pointerX + cursorRadius) + pad
                    bottom = maxOf(anchorY + radius, pointerY + cursorRadius) + pad
                } else {
                    left = minOf(visualStartX, pointerX) - pad
                    top = minOf(visualStartY, pointerY) - pad
                    right = maxOf(visualStartX, pointerX) + pad
                    bottom = maxOf(visualStartY, pointerY) + pad
                }

                if (includeControl && controlAlpha > 0) {
                    val controlPad = dp(4f)
                    left = minOf(left, anchorX - radius - controlPad)
                    top = minOf(top, anchorY - radius - controlPad)
                    right = maxOf(right, anchorX + radius + controlPad)
                    bottom = maxOf(bottom, anchorY + radius + controlPad)
                }

                newDirtyLeft = left.toInt().coerceIn(0, viewWidth)
                newDirtyTop = top.toInt().coerceIn(0, viewHeight)
                newDirtyRight = right.toInt().coerceIn(0, viewWidth)
                newDirtyBottom = bottom.toInt().coerceIn(0, viewHeight)
            }

            private fun drawingWidth(): Int {
                return if (width > 0) width else screenWidth
            }

            private fun drawingHeight(): Int {
                return if (height > 0) height else screenHeight
            }

            private fun invalidateDirty(left: Int, top: Int, right: Int, bottom: Int) {
                if (right <= left || bottom <= top) {
                    return
                }
                postInvalidateOnAnimation(left, top, right, bottom)
            }

            private fun dp(value: Float): Float {
                return value * resources.displayMetrics.density
            }
        }
    }

    private class ClickFeedbackOverlay(
        private val context: Context,
        private val centerX: Float,
        private val centerY: Float,
        touchArea: Float,
        private val color: Int
    ) {
        private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        private val sizePx = max(touchArea * 3.6f, dp(72f)).toInt()
        private val view = ClickFeedbackView(context, color)
        private var shown = false
        private var animator: ValueAnimator? = null

        private val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            2027,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (centerX - sizePx / 2f).toInt()
            y = (centerY - sizePx / 2f).toInt()
        }

        fun show() {
            try {
                windowManager.addView(view, params)
                shown = true
            } catch (t: Throwable) {
                DebugLog.info("click feedback show failed: ${t.message}")
                return
            }

            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = CLICK_FEEDBACK_DURATION_MS
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    view.progress = animation.animatedValue as Float
                    view.postInvalidateOnAnimation()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        dismiss()
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        dismiss()
                    }
                })
                start()
            }
        }

        private fun dismiss() {
            if (!shown) return
            try {
                windowManager.removeView(view)
            } catch (_: Throwable) {
            }
            shown = false
            animator = null
        }

        private fun dp(value: Float): Float {
            return value * context.resources.displayMetrics.density
        }

        private class ClickFeedbackView(context: Context, private val baseColor: Int) : View(context) {
            var progress = 0f

            private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)

                val cx = width / 2f
                val cy = height / 2f
                val eased = progress.coerceIn(0f, 1f)
                val alpha = ((1f - eased) * 210).toInt().coerceIn(0, 210)
                val ringRadius = dp(8f) + (minOf(width, height) / 2f - dp(8f)) * eased
                val dotRadius = dp(4f) * (1f - eased * 0.45f)

                ringPaint.color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                ringPaint.strokeWidth = dp(2.2f)
                dotPaint.color = Color.argb((alpha * 0.55f).toInt(), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

                canvas.drawCircle(cx, cy, ringRadius, ringPaint)
                canvas.drawCircle(cx, cy, dotRadius, dotPaint)
            }

            private fun dp(value: Float): Float {
                return value * resources.displayMetrics.density
            }
        }
    }

    private const val TAP_AFTER_OVERLAY_DISMISS_MS = 70L
    private const val CLICK_FEEDBACK_DURATION_MS = 190L
    private const val TAP_MOVE_DELAY_MS = 28L
    private const val TAP_DURATION_MS = 90L
    private const val IGNORE_INJECTED_EVENTS_MS = 260L
    private const val INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2
    private const val STARTUP_LINE_LENGTH_DP = 84f
    private const val STARTUP_INSET_DP = 24f
    private const val STARTUP_SAFE_MARGIN_FACTOR = 0.32f
    private const val MAX_FINGER_STEP_DP = 42f
    private const val CONTROL_START_SKIP_MOVES = 1
    private const val LINE_ARROW_GAIN = 1.65f
    private const val TRACKER_CURSOR_GAIN = 2.2f
    private const val TRACKER_CURSOR_START_X_FROM_RIGHT = 0.44f
    private const val TRACKER_CURSOR_START_X_FROM_LEFT = 0.56f
    private const val TRACKER_CURSOR_START_Y = 0.38f
    private const val FRAME_FALLBACK_DELAY_MS = 8L
}
