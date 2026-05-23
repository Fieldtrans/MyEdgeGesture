package com.example.myedgegesture

import android.content.Context
import android.view.MotionEvent
import android.view.WindowManager
import kotlin.math.abs

class EdgeGestureDetector(
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onGesture(
            context: Context,
            edge: Edge,
            zone: String,
            gesture: String,
            startX: Float,
            startY: Float,
            x: Float,
            y: Float
        )
    }

    enum class Edge {
        LEFT, RIGHT, TOP, BOTTOM
    }

    enum class Decision {
        FORWARD,
        HOLD,
        CONSUME,
        REPLAY_HELD_THEN_FORWARD,
        DROP_HELD_AND_CONSUME
    }

    private data class Session(
        val edge: Edge,
        val zone: String,
        val downX: Float,
        val downY: Float,
        val thresholdPx: Float,
        val createdAt: Long,
        var claimed: Boolean = false,
        var yielded: Boolean = false
    )

    private var session: Session? = null

    fun handle(context: Context, event: MotionEvent): Decision {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDown(context, event)
            MotionEvent.ACTION_POINTER_DOWN -> {
                reset()
                Decision.REPLAY_HELD_THEN_FORWARD
            }
            MotionEvent.ACTION_MOVE -> handleMove(context, event)
            MotionEvent.ACTION_UP -> handleUp()
            MotionEvent.ACTION_CANCEL -> handleCancel()
            else -> if (session?.claimed == true) Decision.CONSUME else Decision.FORWARD
        }
    }

    fun reset() {
        session = null
    }

    private fun handleDown(context: Context, event: MotionEvent): Decision {
        if (!RuntimeGestureConfig.enabled) {
            session = null
            return Decision.FORWARD
        }

        if (!DeviceState.canRunGestures(context)) {
            session = null
            return Decision.FORWARD
        }

        val bounds = screenBounds(context)
        val density = context.resources.displayMetrics.density
        val edgeWidth = RuntimeGestureConfig.edgeWidthDp * density
        val edge = resolveEdge(bounds.first, bounds.second, edgeWidth, event.rawX, event.rawY)
        if (edge == null || !isInsideTriggerRegion(bounds.first, bounds.second, edge, event.rawX, event.rawY)) {
            session = null
            return Decision.FORWARD
        }

        if (!hasOneHandSwipeUpActionForEdge(edge)) {
            session = null
            return Decision.FORWARD
        }

        session = Session(
            edge = edge,
            zone = resolveZone(bounds.first, bounds.second, edge, event.rawX, event.rawY),
            downX = event.rawX,
            downY = event.rawY,
            thresholdPx = RuntimeGestureConfig.swipeDistanceDp * density,
            createdAt = event.eventTime
        )

        DebugLog.info("hold down edge=$edge x=${event.rawX} y=${event.rawY}")
        return Decision.HOLD
    }

    private fun handleMove(context: Context, event: MotionEvent): Decision {
        val current = session ?: return Decision.FORWARD
        if (current.claimed) return Decision.CONSUME
        if (current.yielded) return Decision.FORWARD

        val density = context.resources.displayMetrics.density
        val dx = event.rawX - current.downX
        val dy = event.rawY - current.downY

        if (isNativeBackIntent(current.edge, dx, dy, density) ||
            isClearlyHorizontalFromSideEdge(current.edge, dx, dy, density)
        ) {
            current.yielded = true
            DebugLog.info("yield to system edge=${current.edge} dx=$dx dy=$dy")
            return Decision.REPLAY_HELD_THEN_FORWARD
        }

        if (isConfirmedSwipeUp(dx, dy, current.thresholdPx)) {
            current.claimed = true
            DebugLog.info("claim pointer swipe edge=${current.edge} dx=$dx dy=$dy")
            callbacks.onGesture(
                context = context,
                edge = current.edge,
                zone = current.zone,
                gesture = "swipe_up",
                startX = current.downX,
                startY = current.downY,
                x = event.rawX,
                y = event.rawY
            )
            return Decision.DROP_HELD_AND_CONSUME
        }

        if (shouldStopHolding(current, event, dx, dy, density)) {
            current.yielded = true
            DebugLog.info("hold timeout yield edge=${current.edge}")
            return Decision.REPLAY_HELD_THEN_FORWARD
        }

        return Decision.HOLD
    }

    private fun handleUp(): Decision {
        val current = session ?: return Decision.FORWARD
        session = null
        return if (current.claimed) Decision.CONSUME else Decision.REPLAY_HELD_THEN_FORWARD
    }

    private fun handleCancel(): Decision {
        val current = session ?: return Decision.FORWARD
        session = null
        return if (current.claimed) Decision.CONSUME else Decision.REPLAY_HELD_THEN_FORWARD
    }

    private fun shouldStopHolding(
        current: Session,
        event: MotionEvent,
        dx: Float,
        dy: Float,
        density: Float
    ): Boolean {
        val holdTime = event.eventTime - current.createdAt
        if (isPossibleSwipeUp(dx, dy, density) && holdTime < MAX_UP_HOLD_MS) {
            return false
        }

        return holdTime >= MAX_HOLD_MS
    }

    private fun resolveEdge(width: Float, height: Float, edgeWidth: Float, x: Float, y: Float): Edge? {
        return when {
            x <= edgeWidth -> Edge.LEFT
            x >= width - edgeWidth -> Edge.RIGHT
            y <= edgeWidth -> Edge.TOP
            y >= height - edgeWidth -> Edge.BOTTOM
            else -> null
        }
    }

    private fun resolveZone(width: Float, height: Float, edge: Edge, x: Float, y: Float): String {
        return when (edge) {
            Edge.LEFT,
            Edge.RIGHT -> verticalZone(y, height)
            Edge.TOP,
            Edge.BOTTOM -> horizontalZone(x, width)
        }
    }

    private fun verticalZone(y: Float, height: Float): String {
        return when {
            y < height * 0.33f -> "top"
            y < height * 0.66f -> "mid"
            else -> "bottom"
        }
    }

    private fun horizontalZone(x: Float, width: Float): String {
        return when {
            x < width * 0.33f -> "left"
            x < width * 0.66f -> "mid"
            else -> "right"
        }
    }

    private fun hasOneHandSwipeUpActionForEdge(edge: Edge): Boolean {
        return RuntimeGestureConfig.actionFor(edge, "swipe_up") == GestureConfig.ACTION_ONE_HAND_TAP
    }

    private fun isInsideTriggerRegion(width: Float, height: Float, edge: Edge, x: Float, y: Float): Boolean {
        val startPercent = RuntimeGestureConfig.triggerRegionStartPercent.coerceIn(0, 100)
        val endPercent = RuntimeGestureConfig.triggerRegionEndPercent.coerceIn(0, 100)
        val start = minOf(startPercent, endPercent) / 100f
        val end = maxOf(startPercent, endPercent) / 100f

        val position = when (edge) {
            Edge.LEFT,
            Edge.RIGHT -> y / height
            Edge.TOP,
            Edge.BOTTOM -> x / width
        }

        return position in start..end
    }

    private fun isNativeBackIntent(edge: Edge, dx: Float, dy: Float, density: Float): Boolean {
        if (edge != Edge.LEFT && edge != Edge.RIGHT) return false

        val inward = when (edge) {
            Edge.LEFT -> dx > 0f
            Edge.RIGHT -> dx < 0f
            else -> false
        }
        if (!inward) return false

        val horizontal = abs(dx)
        val vertical = abs(dy)
        val slop = NATIVE_BACK_YIELD_SLOP_DP * density

        return horizontal >= slop && horizontal > vertical * 0.55f
    }

    private fun isClearlyHorizontalFromSideEdge(edge: Edge, dx: Float, dy: Float, density: Float): Boolean {
        if (edge != Edge.LEFT && edge != Edge.RIGHT) return false
        return abs(dx) >= HORIZONTAL_YIELD_DISTANCE_DP * density && abs(dx) > abs(dy) * 0.75f
    }

    private fun isConfirmedSwipeUp(dx: Float, dy: Float, thresholdPx: Float): Boolean {
        if (dy >= 0f) return false
        val vertical = abs(dy)
        val horizontal = abs(dx)
        val tolerance = RuntimeGestureConfig.swipeAngleDegrees.coerceIn(5, 85)
        val verticalRatio = (1.65f - (tolerance - 5f) / 80f * 0.75f).coerceIn(0.9f, 1.65f)
        return vertical >= thresholdPx && vertical > horizontal * verticalRatio
    }

    private fun isPossibleSwipeUp(dx: Float, dy: Float, density: Float): Boolean {
        if (dy >= 0f) return false
        val vertical = abs(dy)
        val horizontal = abs(dx)
        return vertical >= POSSIBLE_UP_DISTANCE_DP * density && vertical > horizontal * 0.9f
    }

    private fun screenBounds(context: Context): Pair<Float, Float> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bounds = windowManager.currentWindowMetrics.bounds
        return bounds.width().toFloat() to bounds.height().toFloat()
    }

    private companion object {
        const val MAX_HOLD_MS = 110L
        const val MAX_UP_HOLD_MS = 260L
        const val NATIVE_BACK_YIELD_SLOP_DP = 8f
        const val HORIZONTAL_YIELD_DISTANCE_DP = 6f
        const val POSSIBLE_UP_DISTANCE_DP = 10f
    }
}
