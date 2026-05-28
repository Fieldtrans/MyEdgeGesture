package com.example.myedgegesture

import android.content.Intent

object GestureConfig {
    const val PREFS_NAME = "gesture_settings"
    const val ACTION_CONFIG_CHANGED = "com.example.myedgegesture.CONFIG_CHANGED"

    const val KEY_ENABLED = "enabled"
    const val KEY_EDGE_WIDTH_DP = "edge_width_dp"
    const val KEY_SWIPE_DISTANCE_DP = "swipe_distance_dp"
    const val KEY_TRIGGER_REGION_START_PERCENT = "trigger_region_start_percent"
    const val KEY_TRIGGER_REGION_END_PERCENT = "trigger_region_end_percent"
    const val KEY_SWIPE_ANGLE_DEGREES = "swipe_angle_degrees"
    const val KEY_POINTER_RADIUS_DP = "pointer_radius_dp"
    const val KEY_POINTER_CONTROL_ALPHA = "pointer_control_alpha"
    const val KEY_POINTER_SENSITIVITY = "pointer_sensitivity"
    const val KEY_POINTER_ARROW_DP = "pointer_arrow_dp"
    const val KEY_POINTER_TOUCH_AREA_DP = "pointer_touch_area_dp"
    const val KEY_POINTER_LINE_DP = "pointer_line_dp"
    const val KEY_POINTER_MARGIN_DP = "pointer_margin_dp"
    const val KEY_POINTER_CANCEL_DISTANCE_DP = "pointer_cancel_distance_dp"
    const val KEY_POINTER_TIMEOUT_MS = "pointer_timeout_ms"
    const val KEY_POINTER_SMOOTHING = "pointer_smoothing"
    const val KEY_POINTER_MAX_SPEED = "pointer_max_speed"
    const val KEY_POINTER_CURVE = "pointer_curve"
    const val KEY_POINTER_MAPPING_MODE = "pointer_mapping_mode"
    const val KEY_POINTER_CONTROL_STYLE = "pointer_control_style"
    const val KEY_TRACKER_BALL_DP = "tracker_ball_dp"
    const val KEY_TRACKER_CURSOR_DP = "tracker_cursor_dp"
    const val KEY_TRACKER_CANCEL_RADIUS_DP = "tracker_cancel_radius_dp"
    const val KEY_TRACKER_SENSITIVITY = "tracker_sensitivity"
    const val KEY_TRACKER_MAX_SPEED = "tracker_max_speed"
    const val KEY_TRACKER_SMOOTHING = "tracker_smoothing"
    const val KEY_POINTER_COLOR_RED = "pointer_color_red"
    const val KEY_POINTER_COLOR_GREEN = "pointer_color_green"
    const val KEY_POINTER_COLOR_BLUE = "pointer_color_blue"
    const val KEY_CONFIG_UPDATED_AT = "config_updated_at"
    const val STATUS_PREFS_NAME = "hook_status"
    const val KEY_STATUS_LOADED_AT = "loaded_at"
    const val KEY_STATUS_STARTED_AT = "started_at"
    const val KEY_STATUS_LAST_MESSAGE = "last_message"

    const val ACTION_NONE = "none"
    const val ACTION_BACK = "back"
    const val ACTION_HOME = "home"
    const val ACTION_RECENTS = "recents"
    const val ACTION_ONE_HAND_TAP = "one_hand_tap"

    const val POINTER_MAPPING_PRECISION = "precision"
    const val POINTER_STYLE_LINE_ARROW = "line_arrow"
    const val POINTER_STYLE_TRACKER_CURSOR = "tracker_cursor"

    const val DEFAULT_ENABLED = false
    const val DEFAULT_EDGE_WIDTH_DP = 18
    const val DEFAULT_SWIPE_DISTANCE_DP = 72
    const val DEFAULT_TRIGGER_REGION_START_PERCENT = 0
    const val DEFAULT_TRIGGER_REGION_END_PERCENT = 100
    const val DEFAULT_SWIPE_ANGLE_DEGREES = 30
    const val DEFAULT_POINTER_RADIUS_DP = 120
    const val DEFAULT_POINTER_CONTROL_ALPHA = 48
    const val DEFAULT_POINTER_SENSITIVITY = 100
    const val DEFAULT_POINTER_ARROW_DP = 18
    const val DEFAULT_POINTER_TOUCH_AREA_DP = DEFAULT_POINTER_ARROW_DP
    const val DEFAULT_POINTER_LINE_DP = 3
    const val DEFAULT_POINTER_MARGIN_DP = 4
    const val DEFAULT_POINTER_CANCEL_DISTANCE_DP = 240
    const val DEFAULT_POINTER_TIMEOUT_MS = 6500
    const val DEFAULT_POINTER_SMOOTHING = 30
    const val DEFAULT_POINTER_MAX_SPEED = 200
    const val DEFAULT_POINTER_CURVE = 130
    const val DEFAULT_POINTER_MAPPING_MODE = POINTER_MAPPING_PRECISION
    const val DEFAULT_POINTER_CONTROL_STYLE = POINTER_STYLE_LINE_ARROW
    const val DEFAULT_TRACKER_BALL_DP = 10
    const val DEFAULT_TRACKER_CURSOR_DP = 20
    const val DEFAULT_TRACKER_CANCEL_RADIUS_DP = 120
    const val DEFAULT_TRACKER_SENSITIVITY = 100
    const val DEFAULT_TRACKER_MAX_SPEED = 200
    const val DEFAULT_TRACKER_SMOOTHING = 30
    const val DEFAULT_POINTER_COLOR_RED = 0
    const val DEFAULT_POINTER_COLOR_GREEN = 220
    const val DEFAULT_POINTER_COLOR_BLUE = 80

    val pointerMappingValues = listOf(
        POINTER_MAPPING_PRECISION
    )
    val pointerMappingLabels = listOf(
        "精确稳定"
    )

    val pointerStyleValues = listOf(
        POINTER_STYLE_LINE_ARROW,
        POINTER_STYLE_TRACKER_CURSOR
    )
    val pointerStyleLabels = listOf(
        "直线箭头",
        "Tracker + Cursor"
    )

    val edges = listOf("left", "right")
    val gestures = listOf(
        "double_click",
        "swipe_up"
    )
    val actionValues = listOf(
        ACTION_NONE,
        ACTION_ONE_HAND_TAP,
        ACTION_BACK,
        ACTION_HOME,
        ACTION_RECENTS,
    )
    val actionLabels = listOf("无动作", "单手点击屏幕", "返回", "主页", "最近任务")

    fun actionValuesForGesture(gesture: String): List<String> {
        return when (gesture) {
            "swipe_up" -> actionValues
            "double_click" -> listOf(ACTION_NONE, ACTION_RECENTS)
            else -> listOf(ACTION_NONE)
        }
    }

    fun actionKey(edge: String, gesture: String): String {
        return "action_${edge}_$gesture"
    }

    fun defaultAction(edge: String, gesture: String): String {
        return if (edge == "right" && gesture == "swipe_up") {
            ACTION_ONE_HAND_TAP
        } else {
            ACTION_NONE
        }
    }

    fun sanitizeAction(gesture: String, action: String): String {
        return action.takeIf { it in actionValuesForGesture(gesture) } ?: ACTION_NONE
    }

    fun sanitizeActionKey(key: String, action: String): String {
        val gesture = gestures.firstOrNull { key.endsWith("_$it") } ?: return sanitizeAction("", action)
        return sanitizeAction(gesture, action)
    }

    fun putConfigExtras(
        intent: Intent,
        enabled: Boolean,
        edgeWidthDp: Int,
        swipeDistanceDp: Int,
        triggerRegionStartPercent: Int,
        triggerRegionEndPercent: Int,
        swipeAngleDegrees: Int,
        pointerRadiusDp: Int,
        pointerControlAlpha: Int,
        pointerSensitivity: Int,
        pointerArrowDp: Int,
        pointerTouchAreaDp: Int,
        pointerLineDp: Int,
        pointerMarginDp: Int,
        pointerCancelDistanceDp: Int,
        pointerTimeoutMs: Int,
        pointerSmoothing: Int,
        pointerMaxSpeed: Int,
        pointerCurve: Int,
        pointerMappingMode: String,
        pointerControlStyle: String,
        trackerBallDp: Int,
        trackerCursorDp: Int,
        trackerCancelRadiusDp: Int,
        trackerSensitivity: Int,
        trackerMaxSpeed: Int,
        trackerSmoothing: Int,
        pointerColorRed: Int,
        pointerColorGreen: Int,
        pointerColorBlue: Int,
        actionByKey: Map<String, String>,
    ): Intent {
        intent.putExtra(KEY_ENABLED, enabled)
        intent.putExtra(KEY_EDGE_WIDTH_DP, edgeWidthDp)
        intent.putExtra(KEY_SWIPE_DISTANCE_DP, swipeDistanceDp)
        intent.putExtra(KEY_TRIGGER_REGION_START_PERCENT, triggerRegionStartPercent)
        intent.putExtra(KEY_TRIGGER_REGION_END_PERCENT, triggerRegionEndPercent)
        intent.putExtra(KEY_SWIPE_ANGLE_DEGREES, swipeAngleDegrees)
        intent.putExtra(KEY_POINTER_RADIUS_DP, pointerRadiusDp)
        intent.putExtra(KEY_POINTER_CONTROL_ALPHA, pointerControlAlpha)
        intent.putExtra(KEY_POINTER_SENSITIVITY, pointerSensitivity)
        intent.putExtra(KEY_POINTER_ARROW_DP, pointerArrowDp)
        intent.putExtra(KEY_POINTER_TOUCH_AREA_DP, pointerTouchAreaDp)
        intent.putExtra(KEY_POINTER_LINE_DP, pointerLineDp)
        intent.putExtra(KEY_POINTER_MARGIN_DP, pointerMarginDp)
        intent.putExtra(KEY_POINTER_CANCEL_DISTANCE_DP, pointerCancelDistanceDp)
        intent.putExtra(KEY_POINTER_TIMEOUT_MS, pointerTimeoutMs)
        intent.putExtra(KEY_POINTER_SMOOTHING, pointerSmoothing)
        intent.putExtra(KEY_POINTER_MAX_SPEED, pointerMaxSpeed)
        intent.putExtra(KEY_POINTER_CURVE, pointerCurve)
        intent.putExtra(KEY_POINTER_MAPPING_MODE, pointerMappingMode)
        intent.putExtra(KEY_POINTER_CONTROL_STYLE, pointerControlStyle)
        intent.putExtra(KEY_TRACKER_BALL_DP, trackerBallDp)
        intent.putExtra(KEY_TRACKER_CURSOR_DP, trackerCursorDp)
        intent.putExtra(KEY_TRACKER_CANCEL_RADIUS_DP, trackerCancelRadiusDp)
        intent.putExtra(KEY_TRACKER_SENSITIVITY, trackerSensitivity)
        intent.putExtra(KEY_TRACKER_MAX_SPEED, trackerMaxSpeed)
        intent.putExtra(KEY_TRACKER_SMOOTHING, trackerSmoothing)
        intent.putExtra(KEY_POINTER_COLOR_RED, pointerColorRed)
        intent.putExtra(KEY_POINTER_COLOR_GREEN, pointerColorGreen)
        intent.putExtra(KEY_POINTER_COLOR_BLUE, pointerColorBlue)
        actionByKey.forEach { (key, value) ->
            intent.putExtra(key, sanitizeActionKey(key, value))
        }
        return intent
    }
}
