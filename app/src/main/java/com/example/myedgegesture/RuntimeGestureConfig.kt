package com.example.myedgegesture

import android.content.Intent
import android.content.SharedPreferences

object RuntimeGestureConfig {
    @Volatile var enabled: Boolean = GestureConfig.DEFAULT_ENABLED
    @Volatile var edgeWidthDp: Int = GestureConfig.DEFAULT_EDGE_WIDTH_DP
    @Volatile var swipeDistanceDp: Int = GestureConfig.DEFAULT_SWIPE_DISTANCE_DP
    @Volatile var triggerRegionStartPercent: Int = GestureConfig.DEFAULT_TRIGGER_REGION_START_PERCENT
    @Volatile var triggerRegionEndPercent: Int = GestureConfig.DEFAULT_TRIGGER_REGION_END_PERCENT
    @Volatile var swipeAngleDegrees: Int = GestureConfig.DEFAULT_SWIPE_ANGLE_DEGREES
    @Volatile var pointerRadiusDp: Int = GestureConfig.DEFAULT_POINTER_RADIUS_DP
    @Volatile var pointerControlAlpha: Int = GestureConfig.DEFAULT_POINTER_CONTROL_ALPHA
    @Volatile var pointerSensitivity: Int = GestureConfig.DEFAULT_POINTER_SENSITIVITY
    @Volatile var pointerArrowDp: Int = GestureConfig.DEFAULT_POINTER_ARROW_DP
    @Volatile var pointerTouchAreaDp: Int = GestureConfig.DEFAULT_POINTER_TOUCH_AREA_DP
    @Volatile var pointerLineDp: Int = GestureConfig.DEFAULT_POINTER_LINE_DP
    @Volatile var pointerMarginDp: Int = GestureConfig.DEFAULT_POINTER_MARGIN_DP
    @Volatile var pointerCancelDistanceDp: Int = GestureConfig.DEFAULT_POINTER_CANCEL_DISTANCE_DP
    @Volatile var pointerTimeoutMs: Int = GestureConfig.DEFAULT_POINTER_TIMEOUT_MS
    @Volatile var pointerSmoothing: Int = GestureConfig.DEFAULT_POINTER_SMOOTHING
    @Volatile var pointerMaxSpeed: Int = GestureConfig.DEFAULT_POINTER_MAX_SPEED
    @Volatile var pointerCurve: Int = GestureConfig.DEFAULT_POINTER_CURVE
    @Volatile var pointerMappingMode: String = GestureConfig.DEFAULT_POINTER_MAPPING_MODE
    @Volatile var pointerControlStyle: String = GestureConfig.DEFAULT_POINTER_CONTROL_STYLE
    @Volatile var trackerBallDp: Int = GestureConfig.DEFAULT_TRACKER_BALL_DP
    @Volatile var trackerCursorDp: Int = GestureConfig.DEFAULT_TRACKER_CURSOR_DP
    @Volatile var trackerCancelRadiusDp: Int = GestureConfig.DEFAULT_TRACKER_CANCEL_RADIUS_DP
    @Volatile var trackerSensitivity: Int = GestureConfig.DEFAULT_TRACKER_SENSITIVITY
    @Volatile var trackerMaxSpeed: Int = GestureConfig.DEFAULT_TRACKER_MAX_SPEED
    @Volatile var trackerSmoothing: Int = GestureConfig.DEFAULT_TRACKER_SMOOTHING
    @Volatile var pointerColorRed: Int = GestureConfig.DEFAULT_POINTER_COLOR_RED
    @Volatile var pointerColorGreen: Int = GestureConfig.DEFAULT_POINTER_COLOR_GREEN
    @Volatile var pointerColorBlue: Int = GestureConfig.DEFAULT_POINTER_COLOR_BLUE
    private val actionByKey = mutableMapOf<String, String>().apply {
        GestureConfig.edges.forEach { edge ->
            GestureConfig.gestures.forEach { gesture ->
                val key = GestureConfig.actionKey(edge, gesture)
                put(key, GestureConfig.defaultAction(edge, gesture))
            }
        }
    }

    @Synchronized
    fun actionFor(edge: EdgeGestureDetector.Edge, gesture: String): String {
        val key = GestureConfig.actionKey(edge.name.lowercase(), gesture)
        return actionByKey[key] ?: GestureConfig.ACTION_NONE
    }

    @Synchronized
    fun updateFromIntent(intent: Intent) {
        enabled = intent.getBooleanExtra(GestureConfig.KEY_ENABLED, enabled)
        edgeWidthDp = intent.getIntExtra(GestureConfig.KEY_EDGE_WIDTH_DP, edgeWidthDp)
        swipeDistanceDp = intent.getIntExtra(GestureConfig.KEY_SWIPE_DISTANCE_DP, swipeDistanceDp)
        triggerRegionStartPercent = intent.getIntExtra(GestureConfig.KEY_TRIGGER_REGION_START_PERCENT, triggerRegionStartPercent)
        triggerRegionEndPercent = intent.getIntExtra(GestureConfig.KEY_TRIGGER_REGION_END_PERCENT, triggerRegionEndPercent)
        swipeAngleDegrees = intent.getIntExtra(GestureConfig.KEY_SWIPE_ANGLE_DEGREES, swipeAngleDegrees)
        pointerRadiusDp = intent.getIntExtra(GestureConfig.KEY_POINTER_RADIUS_DP, pointerRadiusDp)
        pointerControlAlpha = intent.getIntExtra(GestureConfig.KEY_POINTER_CONTROL_ALPHA, pointerControlAlpha)
        pointerSensitivity = intent.getIntExtra(GestureConfig.KEY_POINTER_SENSITIVITY, pointerSensitivity)
        pointerArrowDp = intent.getIntExtra(GestureConfig.KEY_POINTER_ARROW_DP, pointerArrowDp)
        pointerTouchAreaDp = intent.getIntExtra(GestureConfig.KEY_POINTER_TOUCH_AREA_DP, pointerTouchAreaDp)
        pointerLineDp = intent.getIntExtra(GestureConfig.KEY_POINTER_LINE_DP, pointerLineDp)
        pointerMarginDp = intent.getIntExtra(GestureConfig.KEY_POINTER_MARGIN_DP, pointerMarginDp)
        pointerCancelDistanceDp = intent.getIntExtra(GestureConfig.KEY_POINTER_CANCEL_DISTANCE_DP, pointerCancelDistanceDp)
        pointerTimeoutMs = intent.getIntExtra(GestureConfig.KEY_POINTER_TIMEOUT_MS, pointerTimeoutMs)
        pointerSmoothing = intent.getIntExtra(GestureConfig.KEY_POINTER_SMOOTHING, pointerSmoothing)
        pointerMaxSpeed = intent.getIntExtra(GestureConfig.KEY_POINTER_MAX_SPEED, pointerMaxSpeed)
        pointerCurve = intent.getIntExtra(GestureConfig.KEY_POINTER_CURVE, pointerCurve)
        pointerMappingMode = intent.getStringExtra(GestureConfig.KEY_POINTER_MAPPING_MODE)
            ?.takeIf { it in GestureConfig.pointerMappingValues }
            ?: pointerMappingMode
        pointerControlStyle = intent.getStringExtra(GestureConfig.KEY_POINTER_CONTROL_STYLE)
            ?.takeIf { it in GestureConfig.pointerStyleValues }
            ?: pointerControlStyle
        trackerBallDp = intent.getIntExtra(GestureConfig.KEY_TRACKER_BALL_DP, trackerBallDp)
        trackerCursorDp = intent.getIntExtra(GestureConfig.KEY_TRACKER_CURSOR_DP, trackerCursorDp)
        trackerCancelRadiusDp = intent.getIntExtra(GestureConfig.KEY_TRACKER_CANCEL_RADIUS_DP, trackerCancelRadiusDp)
        trackerSensitivity = intent.getIntExtra(GestureConfig.KEY_TRACKER_SENSITIVITY, trackerSensitivity)
        trackerMaxSpeed = intent.getIntExtra(GestureConfig.KEY_TRACKER_MAX_SPEED, trackerMaxSpeed)
        trackerSmoothing = intent.getIntExtra(GestureConfig.KEY_TRACKER_SMOOTHING, trackerSmoothing)
        pointerColorRed = intent.getIntExtra(GestureConfig.KEY_POINTER_COLOR_RED, pointerColorRed)
        pointerColorGreen = intent.getIntExtra(GestureConfig.KEY_POINTER_COLOR_GREEN, pointerColorGreen)
        pointerColorBlue = intent.getIntExtra(GestureConfig.KEY_POINTER_COLOR_BLUE, pointerColorBlue)

        GestureConfig.edges.forEach { edge ->
            GestureConfig.gestures.forEach { gesture ->
                val key = GestureConfig.actionKey(edge, gesture)
                val action = intent.getStringExtra(key)
                if (action != null) {
                    actionByKey[key] = GestureConfig.sanitizeAction(gesture, action)
                }
            }
        }
    }

    @Synchronized
    fun updateFromPreferences(prefs: SharedPreferences) {
        enabled = prefs.getBoolean(GestureConfig.KEY_ENABLED, enabled)
        edgeWidthDp = prefs.getInt(GestureConfig.KEY_EDGE_WIDTH_DP, edgeWidthDp)
        swipeDistanceDp = prefs.getInt(GestureConfig.KEY_SWIPE_DISTANCE_DP, swipeDistanceDp)
        triggerRegionStartPercent = prefs.getInt(GestureConfig.KEY_TRIGGER_REGION_START_PERCENT, triggerRegionStartPercent)
        triggerRegionEndPercent = prefs.getInt(GestureConfig.KEY_TRIGGER_REGION_END_PERCENT, triggerRegionEndPercent)
        swipeAngleDegrees = prefs.getInt(GestureConfig.KEY_SWIPE_ANGLE_DEGREES, swipeAngleDegrees)
        pointerRadiusDp = prefs.getInt(GestureConfig.KEY_POINTER_RADIUS_DP, pointerRadiusDp)
        pointerControlAlpha = prefs.getInt(GestureConfig.KEY_POINTER_CONTROL_ALPHA, pointerControlAlpha)
        pointerSensitivity = prefs.getInt(GestureConfig.KEY_POINTER_SENSITIVITY, pointerSensitivity)
        pointerArrowDp = prefs.getInt(GestureConfig.KEY_POINTER_ARROW_DP, pointerArrowDp)
        pointerTouchAreaDp = prefs.getInt(GestureConfig.KEY_POINTER_TOUCH_AREA_DP, pointerTouchAreaDp)
        pointerLineDp = prefs.getInt(GestureConfig.KEY_POINTER_LINE_DP, pointerLineDp)
        pointerMarginDp = prefs.getInt(GestureConfig.KEY_POINTER_MARGIN_DP, pointerMarginDp)
        pointerCancelDistanceDp = prefs.getInt(GestureConfig.KEY_POINTER_CANCEL_DISTANCE_DP, pointerCancelDistanceDp)
        pointerTimeoutMs = prefs.getInt(GestureConfig.KEY_POINTER_TIMEOUT_MS, pointerTimeoutMs)
        pointerSmoothing = prefs.getInt(GestureConfig.KEY_POINTER_SMOOTHING, pointerSmoothing)
        pointerMaxSpeed = prefs.getInt(GestureConfig.KEY_POINTER_MAX_SPEED, pointerMaxSpeed)
        pointerCurve = prefs.getInt(GestureConfig.KEY_POINTER_CURVE, pointerCurve)
        pointerMappingMode = prefs.getString(GestureConfig.KEY_POINTER_MAPPING_MODE, pointerMappingMode)
            ?.takeIf { it in GestureConfig.pointerMappingValues }
            ?: GestureConfig.DEFAULT_POINTER_MAPPING_MODE
        pointerControlStyle = prefs.getString(GestureConfig.KEY_POINTER_CONTROL_STYLE, pointerControlStyle)
            ?.takeIf { it in GestureConfig.pointerStyleValues }
            ?: GestureConfig.DEFAULT_POINTER_CONTROL_STYLE
        trackerBallDp = prefs.getInt(GestureConfig.KEY_TRACKER_BALL_DP, trackerBallDp)
        trackerCursorDp = prefs.getInt(GestureConfig.KEY_TRACKER_CURSOR_DP, trackerCursorDp)
        trackerCancelRadiusDp = prefs.getInt(GestureConfig.KEY_TRACKER_CANCEL_RADIUS_DP, trackerCancelRadiusDp)
        trackerSensitivity = prefs.getInt(GestureConfig.KEY_TRACKER_SENSITIVITY, trackerSensitivity)
        trackerMaxSpeed = prefs.getInt(GestureConfig.KEY_TRACKER_MAX_SPEED, trackerMaxSpeed)
        trackerSmoothing = prefs.getInt(GestureConfig.KEY_TRACKER_SMOOTHING, trackerSmoothing)
        pointerColorRed = prefs.getInt(GestureConfig.KEY_POINTER_COLOR_RED, pointerColorRed)
        pointerColorGreen = prefs.getInt(GestureConfig.KEY_POINTER_COLOR_GREEN, pointerColorGreen)
        pointerColorBlue = prefs.getInt(GestureConfig.KEY_POINTER_COLOR_BLUE, pointerColorBlue)

        GestureConfig.edges.forEach { edge ->
            GestureConfig.gestures.forEach { gesture ->
                val key = GestureConfig.actionKey(edge, gesture)
                val savedAction = prefs.getString(key, actionByKey[key])
                    ?: GestureConfig.defaultAction(edge, gesture)
                actionByKey[key] = GestureConfig.sanitizeAction(gesture, savedAction)
            }
        }
    }
}
