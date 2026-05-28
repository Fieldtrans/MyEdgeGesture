package com.example.myedgegesture

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingAutoSave: Runnable? = null
    private var latestState: SettingsState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialState = loadState()
        latestState = initialState

        setContent {
            var settings by remember { mutableStateOf(initialState) }

            LaunchedEffect(Unit) {
                saveConfig(settings, showToast = false)
            }

            EdgeGestureTheme {
                SettingsScreen(
                    settings = settings,
                    onSettingsChange = { next ->
                        settings = next
                        scheduleAutoSave(next)
                    },
                    onSave = { saveConfig(settings, showToast = true) },
                    onReset = {
                        settings = settings.withRecommendedValues()
                        saveConfig(settings, showToast = false)
                        Toast.makeText(this, t("已恢复推荐值", "Recommended values restored"), Toast.LENGTH_SHORT).show()
                    },
                    onExport = { uri ->
                        exportConfig(uri, settings)
                    },
                    onImport = { uri ->
                        importConfig(uri)?.let { imported ->
                            settings = imported
                            saveConfig(imported, showToast = false)
                            Toast.makeText(this, t("配置已导入", "Config imported"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    hookStatus = readHookStatus()
                )
            }
        }
    }

    override fun onDestroy() {
        pendingAutoSave?.let { mainHandler.removeCallbacks(it) }
        pendingAutoSave = null
        super.onDestroy()
    }

    private fun scheduleAutoSave(state: SettingsState) {
        latestState = state
        pendingAutoSave?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            latestState?.let { saveConfig(it, showToast = false) }
        }
        pendingAutoSave = runnable
        mainHandler.postDelayed(runnable, 120L)
    }

    private fun loadState(): SettingsState {
        val prefs = getSharedPreferences(GestureConfig.PREFS_NAME, MODE_PRIVATE)
        val actionByKey = buildMap {
            GestureConfig.edges.forEach { edge ->
                GestureConfig.gestures.forEach { gesture ->
                    val key = GestureConfig.actionKey(edge, gesture)
                    val savedAction = prefs.getString(key, GestureConfig.defaultAction(edge, gesture))
                        ?: GestureConfig.defaultAction(edge, gesture)
                    put(key, GestureConfig.sanitizeAction(gesture, savedAction))
                }
            }
        }

        return SettingsState(
            enabled = prefs.getBoolean(GestureConfig.KEY_ENABLED, GestureConfig.DEFAULT_ENABLED),
            edgeWidthDp = prefs.getInt(GestureConfig.KEY_EDGE_WIDTH_DP, GestureConfig.DEFAULT_EDGE_WIDTH_DP),
            swipeDistanceDp = prefs.getInt(GestureConfig.KEY_SWIPE_DISTANCE_DP, GestureConfig.DEFAULT_SWIPE_DISTANCE_DP),
            triggerRegionStartPercent = prefs.getInt(
                GestureConfig.KEY_TRIGGER_REGION_START_PERCENT,
                GestureConfig.DEFAULT_TRIGGER_REGION_START_PERCENT
            ),
            triggerRegionEndPercent = prefs.getInt(
                GestureConfig.KEY_TRIGGER_REGION_END_PERCENT,
                GestureConfig.DEFAULT_TRIGGER_REGION_END_PERCENT
            ),
            swipeAngleDegrees = prefs.getInt(GestureConfig.KEY_SWIPE_ANGLE_DEGREES, GestureConfig.DEFAULT_SWIPE_ANGLE_DEGREES),
            pointerRadiusDp = prefs.getInt(GestureConfig.KEY_POINTER_RADIUS_DP, GestureConfig.DEFAULT_POINTER_RADIUS_DP),
            pointerControlAlpha = prefs.getInt(
                GestureConfig.KEY_POINTER_CONTROL_ALPHA,
                GestureConfig.DEFAULT_POINTER_CONTROL_ALPHA
            ),
            pointerSensitivity = prefs.getInt(GestureConfig.KEY_POINTER_SENSITIVITY, GestureConfig.DEFAULT_POINTER_SENSITIVITY),
            pointerArrowDp = prefs.getInt(GestureConfig.KEY_POINTER_ARROW_DP, GestureConfig.DEFAULT_POINTER_ARROW_DP),
            pointerTouchAreaDp = prefs.getInt(
                GestureConfig.KEY_POINTER_TOUCH_AREA_DP,
                GestureConfig.DEFAULT_POINTER_TOUCH_AREA_DP
            ),
            pointerLineDp = prefs.getInt(GestureConfig.KEY_POINTER_LINE_DP, GestureConfig.DEFAULT_POINTER_LINE_DP),
            pointerMarginDp = prefs.getInt(GestureConfig.KEY_POINTER_MARGIN_DP, GestureConfig.DEFAULT_POINTER_MARGIN_DP),
            pointerCancelDistanceDp = prefs.getInt(
                GestureConfig.KEY_POINTER_CANCEL_DISTANCE_DP,
                GestureConfig.DEFAULT_POINTER_CANCEL_DISTANCE_DP
            ),
            pointerTimeoutMs = prefs.getInt(GestureConfig.KEY_POINTER_TIMEOUT_MS, GestureConfig.DEFAULT_POINTER_TIMEOUT_MS),
            pointerSmoothing = prefs.getInt(GestureConfig.KEY_POINTER_SMOOTHING, GestureConfig.DEFAULT_POINTER_SMOOTHING),
            pointerMaxSpeed = prefs.getInt(GestureConfig.KEY_POINTER_MAX_SPEED, GestureConfig.DEFAULT_POINTER_MAX_SPEED),
            pointerCurve = prefs.getInt(GestureConfig.KEY_POINTER_CURVE, GestureConfig.DEFAULT_POINTER_CURVE),
            pointerControlStyle = prefs.getString(
                GestureConfig.KEY_POINTER_CONTROL_STYLE,
                GestureConfig.DEFAULT_POINTER_CONTROL_STYLE
            ) ?: GestureConfig.DEFAULT_POINTER_CONTROL_STYLE,
            trackerBallDp = prefs.getInt(GestureConfig.KEY_TRACKER_BALL_DP, GestureConfig.DEFAULT_TRACKER_BALL_DP),
            trackerCursorDp = prefs.getInt(GestureConfig.KEY_TRACKER_CURSOR_DP, GestureConfig.DEFAULT_TRACKER_CURSOR_DP),
            trackerCancelRadiusDp = prefs.getInt(
                GestureConfig.KEY_TRACKER_CANCEL_RADIUS_DP,
                GestureConfig.DEFAULT_TRACKER_CANCEL_RADIUS_DP
            ),
            trackerSensitivity = prefs.getInt(GestureConfig.KEY_TRACKER_SENSITIVITY, GestureConfig.DEFAULT_TRACKER_SENSITIVITY),
            trackerMaxSpeed = prefs.getInt(GestureConfig.KEY_TRACKER_MAX_SPEED, GestureConfig.DEFAULT_TRACKER_MAX_SPEED),
            trackerSmoothing = prefs.getInt(GestureConfig.KEY_TRACKER_SMOOTHING, GestureConfig.DEFAULT_TRACKER_SMOOTHING),
            pointerColorRed = prefs.getInt(GestureConfig.KEY_POINTER_COLOR_RED, GestureConfig.DEFAULT_POINTER_COLOR_RED),
            pointerColorGreen = prefs.getInt(GestureConfig.KEY_POINTER_COLOR_GREEN, GestureConfig.DEFAULT_POINTER_COLOR_GREEN),
            pointerColorBlue = prefs.getInt(GestureConfig.KEY_POINTER_COLOR_BLUE, GestureConfig.DEFAULT_POINTER_COLOR_BLUE),
            actionByKey = actionByKey
        )
    }

    private fun saveConfig(state: SettingsState, showToast: Boolean) {
        pendingAutoSave?.let { mainHandler.removeCallbacks(it) }
        pendingAutoSave = null
        latestState = state

        val savedAt = System.currentTimeMillis()
        val editor = getSharedPreferences(GestureConfig.PREFS_NAME, MODE_PRIVATE).edit()
        putCurrentConfig(editor, state, savedAt)
        editor.commit()

        val deviceEditor = createDeviceProtectedStorageContext()
            .getSharedPreferences(GestureConfig.PREFS_NAME, MODE_PRIVATE)
            .edit()
        putCurrentConfig(deviceEditor, state, savedAt)
        deviceEditor.commit()

        val intent = GestureConfig.putConfigExtras(
            Intent(GestureConfig.ACTION_CONFIG_CHANGED),
            state.enabled,
            state.edgeWidthDp,
            state.swipeDistanceDp,
            state.triggerRegionStartPercent,
            state.triggerRegionEndPercent,
            state.swipeAngleDegrees,
            state.pointerRadiusDp,
            state.pointerControlAlpha,
            state.pointerSensitivity,
            state.pointerArrowDp,
            state.pointerTouchAreaDp,
            state.pointerLineDp,
            state.pointerMarginDp,
            state.pointerCancelDistanceDp,
            state.pointerTimeoutMs,
            state.pointerSmoothing,
            state.pointerMaxSpeed,
            state.pointerCurve,
            GestureConfig.DEFAULT_POINTER_MAPPING_MODE,
            state.pointerControlStyle,
            state.trackerBallDp,
            state.trackerCursorDp,
            state.trackerCancelRadiusDp,
            state.trackerSensitivity,
            state.trackerMaxSpeed,
            state.trackerSmoothing,
            state.pointerColorRed,
            state.pointerColorGreen,
            state.pointerColorBlue,
            state.actionByKey
        )
        sendBroadcast(intent)

        if (showToast) {
            Toast.makeText(this, t("设置已保存", "Settings saved"), Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportConfig(uri: Uri, state: SettingsState) {
        try {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(state.toJsonString().toByteArray(Charsets.UTF_8))
            } ?: error("openOutputStream returned null")
            Toast.makeText(this, t("配置已导出", "Config exported"), Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            Toast.makeText(this, t("导出失败: ${t.message}", "Export failed: ${t.message}"), Toast.LENGTH_LONG).show()
        }
    }

    private fun importConfig(uri: Uri): SettingsState? {
        return try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("openInputStream returned null")
            SettingsState.fromJsonString(text)
        } catch (t: Throwable) {
            Toast.makeText(this, t("导入失败: ${t.message}", "Import failed: ${t.message}"), Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun putCurrentConfig(
        editor: SharedPreferences.Editor,
        state: SettingsState,
        savedAt: Long
    ) {
        editor
            .putLong(GestureConfig.KEY_CONFIG_UPDATED_AT, savedAt)
            .putBoolean(GestureConfig.KEY_ENABLED, state.enabled)
            .putInt(GestureConfig.KEY_EDGE_WIDTH_DP, state.edgeWidthDp)
            .putInt(GestureConfig.KEY_SWIPE_DISTANCE_DP, state.swipeDistanceDp)
            .putInt(GestureConfig.KEY_TRIGGER_REGION_START_PERCENT, state.triggerRegionStartPercent)
            .putInt(GestureConfig.KEY_TRIGGER_REGION_END_PERCENT, state.triggerRegionEndPercent)
            .putInt(GestureConfig.KEY_SWIPE_ANGLE_DEGREES, state.swipeAngleDegrees)
            .putInt(GestureConfig.KEY_POINTER_RADIUS_DP, state.pointerRadiusDp)
            .putInt(GestureConfig.KEY_POINTER_CONTROL_ALPHA, state.pointerControlAlpha)
            .putInt(GestureConfig.KEY_POINTER_SENSITIVITY, state.pointerSensitivity)
            .putInt(GestureConfig.KEY_POINTER_ARROW_DP, state.pointerArrowDp)
            .putInt(GestureConfig.KEY_POINTER_TOUCH_AREA_DP, state.pointerTouchAreaDp)
            .putInt(GestureConfig.KEY_POINTER_LINE_DP, state.pointerLineDp)
            .putInt(GestureConfig.KEY_POINTER_MARGIN_DP, state.pointerMarginDp)
            .putInt(GestureConfig.KEY_POINTER_CANCEL_DISTANCE_DP, state.pointerCancelDistanceDp)
            .putInt(GestureConfig.KEY_POINTER_TIMEOUT_MS, state.pointerTimeoutMs)
            .putInt(GestureConfig.KEY_POINTER_SMOOTHING, state.pointerSmoothing)
            .putInt(GestureConfig.KEY_POINTER_MAX_SPEED, state.pointerMaxSpeed)
            .putInt(GestureConfig.KEY_POINTER_CURVE, state.pointerCurve)
            .putString(GestureConfig.KEY_POINTER_MAPPING_MODE, GestureConfig.DEFAULT_POINTER_MAPPING_MODE)
            .putString(GestureConfig.KEY_POINTER_CONTROL_STYLE, state.pointerControlStyle)
            .putInt(GestureConfig.KEY_TRACKER_BALL_DP, state.trackerBallDp)
            .putInt(GestureConfig.KEY_TRACKER_CURSOR_DP, state.trackerCursorDp)
            .putInt(GestureConfig.KEY_TRACKER_CANCEL_RADIUS_DP, state.trackerCancelRadiusDp)
            .putInt(GestureConfig.KEY_TRACKER_SENSITIVITY, state.trackerSensitivity)
            .putInt(GestureConfig.KEY_TRACKER_MAX_SPEED, state.trackerMaxSpeed)
            .putInt(GestureConfig.KEY_TRACKER_SMOOTHING, state.trackerSmoothing)
            .putInt(GestureConfig.KEY_POINTER_COLOR_RED, state.pointerColorRed)
            .putInt(GestureConfig.KEY_POINTER_COLOR_GREEN, state.pointerColorGreen)
            .putInt(GestureConfig.KEY_POINTER_COLOR_BLUE, state.pointerColorBlue)

        state.actionByKey.forEach { (key, value) ->
            editor.putString(key, GestureConfig.sanitizeActionKey(key, value))
        }
    }

    private fun readHookStatus(): HookStatus {
        val moduleLoadedInApp = HookHealth.isModuleLoaded()
        val normalPrefs = getSharedPreferences(GestureConfig.STATUS_PREFS_NAME, MODE_PRIVATE)
        val devicePrefs = createDeviceProtectedStorageContext()
            .getSharedPreferences(GestureConfig.STATUS_PREFS_NAME, MODE_PRIVATE)
        val prefs = if (devicePrefs.contains(GestureConfig.KEY_STATUS_LOADED_AT) ||
            devicePrefs.contains(GestureConfig.KEY_STATUS_STARTED_AT)
        ) {
            devicePrefs
        } else {
            normalPrefs
        }
        val loadedAt = prefs.getLong(GestureConfig.KEY_STATUS_LOADED_AT, 0L)
        val startedAt = prefs.getLong(GestureConfig.KEY_STATUS_STARTED_AT, 0L)
        val message = prefs.getString(GestureConfig.KEY_STATUS_LAST_MESSAGE, "") ?: ""
        return when {
            startedAt > 0L -> HookStatus(t("输入过滤器已启动 / $message", "Input filter started / $message"), true)
            loadedAt > 0L -> HookStatus(t("system_server 已加载，等待输入过滤器", "system_server loaded, waiting for input filter"), true)
            moduleLoadedInApp -> HookStatus(t("LSPosed 已加载模块", "LSPosed module loaded"), true)
            else -> HookStatus(t("未检测到加载；启用 LSPosed 后需重启", "Module not detected; reboot after enabling it in LSPosed"), false)
        }
    }
}

private fun t(zh: String, en: String): String {
    return if (Locale.getDefault().language.lowercase(Locale.ROOT) == "zh") zh else en
}

private data class SettingsState(
    val enabled: Boolean,
    val edgeWidthDp: Int,
    val swipeDistanceDp: Int,
    val triggerRegionStartPercent: Int,
    val triggerRegionEndPercent: Int,
    val swipeAngleDegrees: Int,
    val pointerRadiusDp: Int,
    val pointerControlAlpha: Int,
    val pointerSensitivity: Int,
    val pointerArrowDp: Int,
    val pointerTouchAreaDp: Int,
    val pointerLineDp: Int,
    val pointerMarginDp: Int,
    val pointerCancelDistanceDp: Int,
    val pointerTimeoutMs: Int,
    val pointerSmoothing: Int,
    val pointerMaxSpeed: Int,
    val pointerCurve: Int,
    val pointerControlStyle: String,
    val trackerBallDp: Int,
    val trackerCursorDp: Int,
    val trackerCancelRadiusDp: Int,
    val trackerSensitivity: Int,
    val trackerMaxSpeed: Int,
    val trackerSmoothing: Int,
    val pointerColorRed: Int,
    val pointerColorGreen: Int,
    val pointerColorBlue: Int,
    val actionByKey: Map<String, String>
) {
    val pointerColor: Color
        get() = Color(
            red = pointerColorRed.coerceIn(0, 255) / 255f,
            green = pointerColorGreen.coerceIn(0, 255) / 255f,
            blue = pointerColorBlue.coerceIn(0, 255) / 255f
        )

    val timeoutSeconds: Int
        get() = pointerTimeoutMs / 1000

    fun toJsonString(): String {
        val actions = JSONObject()
        actionByKey.toSortedMap().forEach { (key, value) ->
            actions.put(key, value)
        }

        return JSONObject()
            .put("schemaVersion", 1)
            .put("app", "EdgeGesture")
            .put("enabled", enabled)
            .put("edgeWidthDp", edgeWidthDp)
            .put("swipeDistanceDp", swipeDistanceDp)
            .put("triggerRegionStartPercent", triggerRegionStartPercent)
            .put("triggerRegionEndPercent", triggerRegionEndPercent)
            .put("swipeAngleDegrees", swipeAngleDegrees)
            .put("pointerRadiusDp", pointerRadiusDp)
            .put("pointerControlAlpha", pointerControlAlpha)
            .put("pointerSensitivity", pointerSensitivity)
            .put("pointerArrowDp", pointerArrowDp)
            .put("pointerTouchAreaDp", pointerTouchAreaDp)
            .put("pointerLineDp", pointerLineDp)
            .put("pointerMarginDp", pointerMarginDp)
            .put("pointerCancelDistanceDp", pointerCancelDistanceDp)
            .put("pointerTimeoutMs", pointerTimeoutMs)
            .put("pointerSmoothing", pointerSmoothing)
            .put("pointerMaxSpeed", pointerMaxSpeed)
            .put("pointerCurve", pointerCurve)
            .put("pointerControlStyle", pointerControlStyle)
            .put("trackerBallDp", trackerBallDp)
            .put("trackerCursorDp", trackerCursorDp)
            .put("trackerCancelRadiusDp", trackerCancelRadiusDp)
            .put("trackerSensitivity", trackerSensitivity)
            .put("trackerMaxSpeed", trackerMaxSpeed)
            .put("trackerSmoothing", trackerSmoothing)
            .put("pointerColorRed", pointerColorRed)
            .put("pointerColorGreen", pointerColorGreen)
            .put("pointerColorBlue", pointerColorBlue)
            .put("actionByKey", actions)
            .toString(2)
    }

    fun withRecommendedValues(): SettingsState {
        return copy(
            edgeWidthDp = GestureConfig.DEFAULT_EDGE_WIDTH_DP,
            swipeDistanceDp = GestureConfig.DEFAULT_SWIPE_DISTANCE_DP,
            triggerRegionStartPercent = GestureConfig.DEFAULT_TRIGGER_REGION_START_PERCENT,
            triggerRegionEndPercent = GestureConfig.DEFAULT_TRIGGER_REGION_END_PERCENT,
            swipeAngleDegrees = GestureConfig.DEFAULT_SWIPE_ANGLE_DEGREES,
            pointerRadiusDp = GestureConfig.DEFAULT_POINTER_RADIUS_DP,
            pointerControlAlpha = GestureConfig.DEFAULT_POINTER_CONTROL_ALPHA,
            pointerSensitivity = GestureConfig.DEFAULT_POINTER_SENSITIVITY,
            pointerArrowDp = GestureConfig.DEFAULT_POINTER_ARROW_DP,
            pointerTouchAreaDp = GestureConfig.DEFAULT_POINTER_TOUCH_AREA_DP,
            pointerLineDp = GestureConfig.DEFAULT_POINTER_LINE_DP,
            pointerMarginDp = GestureConfig.DEFAULT_POINTER_MARGIN_DP,
            pointerCancelDistanceDp = GestureConfig.DEFAULT_POINTER_CANCEL_DISTANCE_DP,
            pointerTimeoutMs = GestureConfig.DEFAULT_POINTER_TIMEOUT_MS,
            pointerSmoothing = GestureConfig.DEFAULT_POINTER_SMOOTHING,
            pointerMaxSpeed = GestureConfig.DEFAULT_POINTER_MAX_SPEED,
            pointerCurve = GestureConfig.DEFAULT_POINTER_CURVE,
            pointerControlStyle = GestureConfig.DEFAULT_POINTER_CONTROL_STYLE,
            trackerBallDp = GestureConfig.DEFAULT_TRACKER_BALL_DP,
            trackerCursorDp = GestureConfig.DEFAULT_TRACKER_CURSOR_DP,
            trackerCancelRadiusDp = GestureConfig.DEFAULT_TRACKER_CANCEL_RADIUS_DP,
            trackerSensitivity = GestureConfig.DEFAULT_TRACKER_SENSITIVITY,
            trackerMaxSpeed = GestureConfig.DEFAULT_TRACKER_MAX_SPEED,
            trackerSmoothing = GestureConfig.DEFAULT_TRACKER_SMOOTHING,
            pointerColorRed = GestureConfig.DEFAULT_POINTER_COLOR_RED,
            pointerColorGreen = GestureConfig.DEFAULT_POINTER_COLOR_GREEN,
            pointerColorBlue = GestureConfig.DEFAULT_POINTER_COLOR_BLUE
        )
    }

    companion object {
        fun fromJsonString(text: String): SettingsState {
            val json = JSONObject(text)
            val actionsJson = json.optJSONObject("actionByKey") ?: json.optJSONObject("actions")
            val actionByKey = buildMap {
                GestureConfig.edges.forEach { edge ->
                    GestureConfig.gestures.forEach { gesture ->
                        val key = GestureConfig.actionKey(edge, gesture)
                        val imported = actionsJson?.optString(key).orEmpty()
                        put(
                            key,
                            GestureConfig.sanitizeAction(
                                gesture,
                                imported.takeIf { it in GestureConfig.actionValues }
                                    ?: GestureConfig.defaultAction(edge, gesture)
                            )
                        )
                    }
                }
            }
            val pointerStyle = json.optString(
                "pointerControlStyle",
                GestureConfig.DEFAULT_POINTER_CONTROL_STYLE
            ).takeIf { it in GestureConfig.pointerStyleValues } ?: GestureConfig.DEFAULT_POINTER_CONTROL_STYLE

            return SettingsState(
                enabled = json.optBoolean("enabled", GestureConfig.DEFAULT_ENABLED),
                edgeWidthDp = json.optInt("edgeWidthDp", GestureConfig.DEFAULT_EDGE_WIDTH_DP),
                swipeDistanceDp = json.optInt("swipeDistanceDp", GestureConfig.DEFAULT_SWIPE_DISTANCE_DP),
                triggerRegionStartPercent = json.optInt(
                    "triggerRegionStartPercent",
                    GestureConfig.DEFAULT_TRIGGER_REGION_START_PERCENT
                ),
                triggerRegionEndPercent = json.optInt(
                    "triggerRegionEndPercent",
                    GestureConfig.DEFAULT_TRIGGER_REGION_END_PERCENT
                ),
                swipeAngleDegrees = json.optInt("swipeAngleDegrees", GestureConfig.DEFAULT_SWIPE_ANGLE_DEGREES),
                pointerRadiusDp = json.optInt("pointerRadiusDp", GestureConfig.DEFAULT_POINTER_RADIUS_DP),
                pointerControlAlpha = json.optInt(
                    "pointerControlAlpha",
                    GestureConfig.DEFAULT_POINTER_CONTROL_ALPHA
                ),
                pointerSensitivity = json.optInt("pointerSensitivity", GestureConfig.DEFAULT_POINTER_SENSITIVITY),
                pointerArrowDp = json.optInt("pointerArrowDp", GestureConfig.DEFAULT_POINTER_ARROW_DP),
                pointerTouchAreaDp = json.optInt("pointerTouchAreaDp", GestureConfig.DEFAULT_POINTER_TOUCH_AREA_DP),
                pointerLineDp = json.optInt("pointerLineDp", GestureConfig.DEFAULT_POINTER_LINE_DP),
                pointerMarginDp = json.optInt("pointerMarginDp", GestureConfig.DEFAULT_POINTER_MARGIN_DP),
                pointerCancelDistanceDp = json.optInt(
                    "pointerCancelDistanceDp",
                    GestureConfig.DEFAULT_POINTER_CANCEL_DISTANCE_DP
                ),
                pointerTimeoutMs = json.optInt("pointerTimeoutMs", GestureConfig.DEFAULT_POINTER_TIMEOUT_MS),
                pointerSmoothing = json.optInt("pointerSmoothing", GestureConfig.DEFAULT_POINTER_SMOOTHING),
                pointerMaxSpeed = json.optInt("pointerMaxSpeed", GestureConfig.DEFAULT_POINTER_MAX_SPEED),
                pointerCurve = json.optInt("pointerCurve", GestureConfig.DEFAULT_POINTER_CURVE),
                pointerControlStyle = pointerStyle,
                trackerBallDp = json.optInt("trackerBallDp", GestureConfig.DEFAULT_TRACKER_BALL_DP),
                trackerCursorDp = json.optInt("trackerCursorDp", GestureConfig.DEFAULT_TRACKER_CURSOR_DP),
                trackerCancelRadiusDp = json.optInt(
                    "trackerCancelRadiusDp",
                    GestureConfig.DEFAULT_TRACKER_CANCEL_RADIUS_DP
                ),
                trackerSensitivity = json.optInt("trackerSensitivity", GestureConfig.DEFAULT_TRACKER_SENSITIVITY),
                trackerMaxSpeed = json.optInt("trackerMaxSpeed", GestureConfig.DEFAULT_TRACKER_MAX_SPEED),
                trackerSmoothing = json.optInt("trackerSmoothing", GestureConfig.DEFAULT_TRACKER_SMOOTHING),
                pointerColorRed = json.optInt("pointerColorRed", GestureConfig.DEFAULT_POINTER_COLOR_RED),
                pointerColorGreen = json.optInt("pointerColorGreen", GestureConfig.DEFAULT_POINTER_COLOR_GREEN),
                pointerColorBlue = json.optInt("pointerColorBlue", GestureConfig.DEFAULT_POINTER_COLOR_BLUE),
                actionByKey = actionByKey
            )
        }
    }
}

private data class HookStatus(
    val text: String,
    val active: Boolean
)

private data class SettingsPage(
    val title: String,
    val icon: ImageVector
)

private enum class PointerSubPage {
    Line,
    Tracker,
    Appearance
}

@Composable
private fun EdgeGestureTheme(content: @Composable () -> Unit) {
    val light = lightColorScheme(
        primary = Color(0xFF006D3F),
        onPrimary = Color.White,
        secondary = Color(0xFF006A6A),
        tertiary = Color(0xFF6750A4),
        background = Color(0xFFF7FAF7),
        surface = Color(0xFFFEFCF8),
        surfaceVariant = Color(0xFFE7EFE8),
        outline = Color(0xFFB8C8BD)
    )
    val dark = darkColorScheme(
        primary = Color(0xFF64D692),
        secondary = Color(0xFF4DD5D3),
        tertiary = Color(0xFFD0BCFF)
    )
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) dark else light,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    settings: SettingsState,
    onSettingsChange: (SettingsState) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    hookStatus: HookStatus
) {
    val pages = remember {
        listOf(
            SettingsPage(t("总览", "Overview"), Icons.Rounded.Settings),
            SettingsPage(t("触发", "Trigger"), Icons.Rounded.TouchApp),
            SettingsPage(t("指针", "Pointer"), Icons.Rounded.RadioButtonChecked),
            SettingsPage(t("动作", "Actions"), Icons.Rounded.AccountTree)
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let(onExport)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(onImport)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("EdgeGesture", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (settings.enabled) t("手势已启用", "Gestures enabled") else t("手势未启用", "Gestures disabled"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }) {
                        Icon(Icons.Rounded.Upload, contentDescription = t("导入配置", "Import config"))
                    }
                    IconButton(onClick = { exportLauncher.launch("EdgeGesture-config.json") }) {
                        Icon(Icons.Rounded.Download, contentDescription = t("导出配置", "Export config"))
                    }
                    IconButton(onClick = onReset) {
                        Icon(Icons.Rounded.Restore, contentDescription = t("恢复推荐值", "Restore recommended values"))
                    }
                    IconButton(onClick = onSave) {
                        Icon(Icons.Rounded.Save, contentDescription = t("保存设置", "Save settings"))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                pages.forEachIndexed { index, page ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = { Icon(page.icon, contentDescription = null) },
                        label = { Text(page.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
            ) { pageIndex ->
                when (pageIndex) {
                    1 -> TriggerPage(
                        settings = settings,
                        onSettingsChange = onSettingsChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    )
                    2 -> PointerPage(
                        settings = settings,
                        onSettingsChange = onSettingsChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    )
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            when (pageIndex) {
                                0 -> OverviewPage(settings, onSettingsChange, hookStatus)
                                3 -> ActionPage(settings, onSettingsChange)
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewPage(
    settings: SettingsState,
    onSettingsChange: (SettingsState) -> Unit,
    hookStatus: HookStatus
) {
    StatusCard(settings, hookStatus, onSettingsChange)

    SettingsSection(
        title = t("当前模式", "Current Mode"),
        icon = Icons.Rounded.PowerSettingsNew
    ) {
        ModeSelector(settings, onSettingsChange)
        SettingSlider(
            title = t("触发边缘宽度", "Trigger Edge Width"),
            valueText = "${settings.edgeWidthDp}dp",
            description = t("越宽越容易触发，也越容易靠近系统侧滑区域。", "Wider is easier to trigger, but closer to the system back gesture area."),
            value = settings.edgeWidthDp,
            range = 8..56,
            onValueChange = { onSettingsChange(settings.copy(edgeWidthDp = it)) }
        )
        SettingSlider(
            title = t("直线箭头灵敏度", "Line Arrow Sensitivity"),
            valueText = "${settings.pointerSensitivity}%",
            description = t("影响直线箭头模式下手指移动到指针移动的比例。", "Controls the finger-to-pointer movement ratio in line arrow mode."),
            value = settings.pointerSensitivity,
            range = 40..180,
            onValueChange = { onSettingsChange(settings.copy(pointerSensitivity = it)) }
        )
    }

    SettingsSection(t("说明", "Notes"), Icons.Rounded.Palette) {
        Text(
            text = t(
                "启用 LSPosed 模块后建议重启。杀掉 App 不影响手势；App 只负责保存参数。需要排查时，在 LSPosed 日志里搜索 EdgeGesture。若调出时卡顿，优先降低控制圆透明度。",
                "Reboot after enabling the LSPosed module. Killing the app does not stop gestures; the app only saves settings. Search EdgeGesture in LSPosed logs for troubleshooting. If the overlay stutters, lower the control circle opacity first."
            ),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusCard(
    settings: SettingsState,
    hookStatus: HookStatus,
    onSettingsChange: (SettingsState) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (hookStatus.active) Color(0xFF10A85A) else Color(0xFFE05A47),
                            CircleShape
                        )
                )
                Column(Modifier.weight(1f)) {
                    Text(t("模块状态", "Module Status"), style = MaterialTheme.typography.labelLarge)
                    Text(
                        hookStatus.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SettingSwitch(
                title = t("启用模块手势", "Enable Module Gestures"),
                description = t("关闭后不处理边缘上划。", "When disabled, edge swipe-up will not be handled."),
                checked = settings.enabled,
                onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) }
            )
        }
    }
}

@Composable
private fun TriggerPage(
    settings: SettingsState,
    onSettingsChange: (SettingsState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EdgeRangePreview(settings)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsSection(t("触发区域", "Trigger Area"), Icons.Rounded.TouchApp) {
                SettingSlider(
                    title = t("区域起点", "Area Start"),
                    valueText = "${settings.triggerRegionStartPercent}%",
                    description = t("从屏幕上方开始计算。", "Measured from the top of the screen."),
                    value = settings.triggerRegionStartPercent,
                    range = 0..100,
                    onValueChange = { onSettingsChange(settings.copy(triggerRegionStartPercent = it)) }
                )
                SettingSlider(
                    title = t("区域终点", "Area End"),
                    valueText = "${settings.triggerRegionEndPercent}%",
                    description = t("区域终点可以低于起点，内部会自动取有效范围。", "The end can be lower than the start; the valid range is normalized automatically."),
                    value = settings.triggerRegionEndPercent,
                    range = 0..100,
                    onValueChange = { onSettingsChange(settings.copy(triggerRegionEndPercent = it)) }
                )
                SettingSlider(
                    title = t("边缘宽度", "Edge Width"),
                    valueText = "${settings.edgeWidthDp}dp",
                    description = t("推荐先保持 18dp 左右，避免系统返回冲突。", "Start around 18dp to avoid conflicts with the system back gesture."),
                    value = settings.edgeWidthDp,
                    range = 8..56,
                    onValueChange = { onSettingsChange(settings.copy(edgeWidthDp = it)) }
                )
            }

            SettingsSection(t("高级触发", "Advanced Trigger"), Icons.Rounded.Tune) {
                SettingSlider(
                    title = t("上划触发距离", "Swipe-Up Distance"),
                    valueText = "${settings.swipeDistanceDp}dp",
                    description = t("距离越大，越不容易误触。", "A longer distance reduces accidental triggers."),
                    value = settings.swipeDistanceDp,
                    range = 40..180,
                    onValueChange = { onSettingsChange(settings.copy(swipeDistanceDp = it)) }
                )
                SettingSlider(
                    title = t("方向允许偏角", "Allowed Angle"),
                    valueText = "±${settings.swipeAngleDegrees}°",
                    description = t("越小越严格，越不容易和横向侧滑混淆。", "Smaller values are stricter and reduce horizontal gesture conflicts."),
                    value = settings.swipeAngleDegrees,
                    range = 5..85,
                    onValueChange = { onSettingsChange(settings.copy(swipeAngleDegrees = it)) }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PointerPage(
    settings: SettingsState,
    onSettingsChange: (SettingsState) -> Unit,
    modifier: Modifier = Modifier
) {
    var subPage by remember {
        mutableStateOf(
            if (settings.pointerControlStyle == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
                PointerSubPage.Tracker
            } else {
                PointerSubPage.Line
            }
        )
    }

    LaunchedEffect(settings.pointerControlStyle) {
        if (subPage != PointerSubPage.Appearance) {
            subPage = if (settings.pointerControlStyle == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
                PointerSubPage.Tracker
            } else {
                PointerSubPage.Line
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PointerPreview(settings)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsSection(t("指针设置", "Pointer Settings"), Icons.Rounded.RadioButtonChecked) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    FilterChip(
                        selected = subPage == PointerSubPage.Line,
                        onClick = {
                            subPage = PointerSubPage.Line
                            onSettingsChange(settings.copy(pointerControlStyle = GestureConfig.POINTER_STYLE_LINE_ARROW))
                        },
                        label = { Text(t("直线", "Line")) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    FilterChip(
                        selected = subPage == PointerSubPage.Tracker,
                        onClick = {
                            subPage = PointerSubPage.Tracker
                            onSettingsChange(settings.copy(pointerControlStyle = GestureConfig.POINTER_STYLE_TRACKER_CURSOR))
                        },
                        label = { Text(t("摇杆", "Tracker")) },
                        leadingIcon = {
                            Icon(Icons.Rounded.RadioButtonChecked, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    FilterChip(
                        selected = subPage == PointerSubPage.Appearance,
                        onClick = { subPage = PointerSubPage.Appearance },
                        label = { Text(t("外观", "Appearance")) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            when (subPage) {
                PointerSubPage.Line -> LinePointerPage(settings, onSettingsChange)
                PointerSubPage.Tracker -> TrackerPointerPage(settings, onSettingsChange)
                PointerSubPage.Appearance -> AppearancePage(settings, onSettingsChange)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LinePointerPage(
    settings: SettingsState,
    onSettingsChange: (SettingsState) -> Unit
) {
    SettingsSection(t("直线箭头", "Line Arrow"), Icons.Rounded.Tune) {
        SettingSlider(
            title = t("速度 / 灵敏度", "Speed / Sensitivity"),
            valueText = "${settings.pointerSensitivity}%",
            description = t("直线箭头跟手程度。", "How closely the line arrow follows your finger."),
            value = settings.pointerSensitivity,
            range = 40..180,
            onValueChange = { onSettingsChange(settings.copy(pointerSensitivity = it)) }
        )
        SettingSlider(
            title = t("控制圆半径", "Control Radius"),
            valueText = "${settings.pointerRadiusDp}dp",
            description = t("拇指活动范围，越大越稳定。", "Thumb movement range; larger values feel steadier."),
            value = settings.pointerRadiusDp,
            range = 48..320,
            onValueChange = { onSettingsChange(settings.copy(pointerRadiusDp = it)) }
        )
        SettingSlider(
            title = t("最大速度", "Max Speed"),
            valueText = t("${settings.pointerMaxSpeed}%屏高/秒", "${settings.pointerMaxSpeed}% screen/s"),
            description = t("限制指针每秒最多移动多少屏幕高度。", "Limits how much screen height the pointer can move per second."),
            value = settings.pointerMaxSpeed,
            range = 40..500,
            onValueChange = { onSettingsChange(settings.copy(pointerMaxSpeed = it)) }
        )
        SettingSlider(
            title = t("点击区域", "Touch Area"),
            valueText = "${settings.pointerTouchAreaDp}dp",
            description = t("注入点击时模拟手指接触面积。", "Simulated contact area used when injecting a tap."),
            value = settings.pointerTouchAreaDp,
            range = 6..48,
            onValueChange = { onSettingsChange(settings.copy(pointerTouchAreaDp = it)) }
        )
    }

    SettingsSection(t("直线高级", "Line Advanced"), Icons.Rounded.Settings) {
        SettingSlider(
            title = t("控制圆 / 取消圆", "Control / Cancel Circle"),
            valueText = "${settings.pointerRadiusDp}dp",
            description = t("指针或光标在这个圆内松手会取消点击。", "Release is canceled when the pointer or cursor is inside this circle."),
            value = settings.pointerRadiusDp,
            range = 48..320,
            onValueChange = { onSettingsChange(settings.copy(pointerRadiusDp = it)) }
        )
        SettingSlider(
            title = t("自动取消时间", "Auto Cancel Time"),
            valueText = t("${settings.timeoutSeconds}秒", "${settings.timeoutSeconds}s"),
            description = t("定位太久会自动取消，避免指针一直停留。", "Cancels after a long hold so the pointer does not stay on screen."),
            value = settings.timeoutSeconds,
            range = 2..10,
            onValueChange = { onSettingsChange(settings.copy(pointerTimeoutMs = it * 1000)) }
        )
        SettingSlider(
            title = t("平滑度", "Smoothing"),
            valueText = "${settings.pointerSmoothing}%",
            description = t("越高越稳，越低越跟手。", "Higher is steadier; lower follows faster."),
            value = settings.pointerSmoothing,
            range = 5..90,
            onValueChange = { onSettingsChange(settings.copy(pointerSmoothing = it)) }
        )
        SettingSlider(
            title = t("控制曲线", "Control Curve"),
            valueText = "${settings.pointerCurve}%",
            description = t("保留给映射函数微调。", "Fine tuning for the pointer mapping curve."),
            value = settings.pointerCurve,
            range = 60..220,
            onValueChange = { onSettingsChange(settings.copy(pointerCurve = it)) }
        )
        SettingSlider(
            title = t("边界留白", "Edge Margin"),
            valueText = "${settings.pointerMarginDp}dp",
            description = t("指针离屏幕边缘的最小距离。", "Minimum distance between the pointer and screen edges."),
            value = settings.pointerMarginDp,
            range = 0..48,
            onValueChange = { onSettingsChange(settings.copy(pointerMarginDp = it)) }
        )
    }
}

@Composable
private fun TrackerPointerPage(
    settings: SettingsState,
    onSettingsChange: (SettingsState) -> Unit
) {
    SettingsSection(t("控制圆 / 取消", "Control / Cancel"), Icons.Rounded.Settings) {
        SettingSlider(
            title = t("控制圆半径", "Control Circle Radius"),
            valueText = "${settings.pointerRadiusDp}dp",
            description = t("光标圆球在这个圆内松手会取消点击。", "Release is canceled when the cursor ball is inside this circle."),
            value = settings.pointerRadiusDp,
            range = 48..320,
            onValueChange = { onSettingsChange(settings.copy(pointerRadiusDp = it)) }
        )
    }

    SettingsSection(t("摇杆光标", "Tracker Cursor"), Icons.Rounded.RadioButtonChecked) {
        SettingSlider(
            title = t("摇杆灵敏度", "Tracker Sensitivity"),
            valueText = "${settings.trackerSensitivity}%",
            description = t("摇杆移动到光标移动的比例。", "Ratio between tracker movement and cursor movement."),
            value = settings.trackerSensitivity,
            range = 40..220,
            onValueChange = { onSettingsChange(settings.copy(trackerSensitivity = it)) }
        )
        SettingSlider(
            title = t("摇杆最大速度", "Tracker Max Speed"),
            valueText = t("${settings.trackerMaxSpeed}%屏高/秒", "${settings.trackerMaxSpeed}% screen/s"),
            description = t("限制摇杆模式的光标速度。", "Limits cursor speed in tracker mode."),
            value = settings.trackerMaxSpeed,
            range = 40..500,
            onValueChange = { onSettingsChange(settings.copy(trackerMaxSpeed = it)) }
        )
        SettingSlider(
            title = t("光标圆大小", "Cursor Size"),
            valueText = "${settings.trackerCursorDp}dp",
            description = t("松手点击光标圆中心。", "Release taps the center of the cursor circle."),
            value = settings.trackerCursorDp,
            range = 8..56,
            onValueChange = { onSettingsChange(settings.copy(trackerCursorDp = it)) }
        )
        SettingSlider(
            title = t("摇杆圆球大小", "Tracker Ball Size"),
            valueText = "${settings.trackerBallDp}dp",
            description = t("手指附近显示的摇杆中心点。", "Size of the tracker center shown near your finger."),
            value = settings.trackerBallDp,
            range = 4..32,
            onValueChange = { onSettingsChange(settings.copy(trackerBallDp = it)) }
        )
        SettingSlider(
            title = t("摇杆平滑度", "Tracker Smoothing"),
            valueText = "${settings.trackerSmoothing}%",
            description = t("越高越稳，越低越跟手。", "Higher is steadier; lower follows faster."),
            value = settings.trackerSmoothing,
            range = 5..90,
            onValueChange = { onSettingsChange(settings.copy(trackerSmoothing = it)) }
        )
    }
}

@Composable
private fun AppearancePage(
    settings: SettingsState,
    onSettingsChange: (SettingsState) -> Unit
) {
    SettingsSection(t("外观", "Appearance"), Icons.Rounded.Palette) {
        SettingSlider(
            title = t("控制圆透明度", "Control Circle Opacity"),
            valueText = settings.pointerControlAlpha.toString(),
            description = t("调到 0 就隐藏控制圆。", "Set to 0 to hide the control circle."),
            value = settings.pointerControlAlpha,
            range = 0..255,
            onValueChange = { onSettingsChange(settings.copy(pointerControlAlpha = it)) }
        )
        SettingSlider(
            title = t("箭头大小", "Arrow Size"),
            valueText = "${settings.pointerArrowDp}dp",
            description = t("只影响直线箭头模式。", "Only affects line arrow mode."),
            value = settings.pointerArrowDp,
            range = 8..36,
            onValueChange = { onSettingsChange(settings.copy(pointerArrowDp = it)) }
        )
        SettingSlider(
            title = t("线条粗细", "Line Width"),
            valueText = "${settings.pointerLineDp}dp",
            description = t("只影响直线箭头和摇杆中心点描边。", "Only affects the line arrow and tracker center outline."),
            value = settings.pointerLineDp,
            range = 1..8,
            onValueChange = { onSettingsChange(settings.copy(pointerLineDp = it)) }
        )
    }

    SettingsSection(t("颜色", "Color"), Icons.Rounded.Palette) {
        ColorSwatch(settings.pointerColor)
        SettingSlider(
            title = t("红色", "Red"),
            valueText = settings.pointerColorRed.toString(),
            description = null,
            value = settings.pointerColorRed,
            range = 0..255,
            onValueChange = { onSettingsChange(settings.copy(pointerColorRed = it)) }
        )
        SettingSlider(
            title = t("绿色", "Green"),
            valueText = settings.pointerColorGreen.toString(),
            description = null,
            value = settings.pointerColorGreen,
            range = 0..255,
            onValueChange = { onSettingsChange(settings.copy(pointerColorGreen = it)) }
        )
        SettingSlider(
            title = t("蓝色", "Blue"),
            valueText = settings.pointerColorBlue.toString(),
            description = null,
            value = settings.pointerColorBlue,
            range = 0..255,
            onValueChange = { onSettingsChange(settings.copy(pointerColorBlue = it)) }
        )
    }
}

@Composable
private fun ActionPage(
    settings: SettingsState,
    onSettingsChange: (SettingsState) -> Unit
) {
    var selectedEdge by remember { mutableStateOf("right") }
    SettingsSection(t("边缘", "Edge"), Icons.Rounded.AccountTree) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            GestureConfig.edges.forEach { edge ->
                FilterChip(
                    selected = selectedEdge == edge,
                    onClick = { selectedEdge = edge },
                    label = { Text(edgeLabel(edge)) }
                )
            }
        }
    }

    SettingsSection(t("${edgeLabel(selectedEdge)}动作", "${edgeLabel(selectedEdge)} Actions"), Icons.Rounded.TouchApp) {
        GestureConfig.gestures.forEach { gesture ->
            val key = GestureConfig.actionKey(selectedEdge, gesture)
            ActionDropdownRow(
                title = gestureLabel(gesture),
                gesture = gesture,
                selectedAction = settings.actionByKey[key] ?: GestureConfig.defaultAction(selectedEdge, gesture),
                onActionSelected = { action ->
                    onSettingsChange(settings.copy(actionByKey = settings.actionByKey + (key to action)))
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingSlider(
    title: String,
    valueText: String,
    description: String?,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.coerceIn(range.first, range.last).toFloat(),
            onValueChange = { next ->
                onValueChange(next.roundToInt().coerceIn(range.first, range.last))
            },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}

@Composable
private fun ModeSelector(
    settings: SettingsState,
    onSettingsChange: (SettingsState) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        FilterChip(
            selected = settings.pointerControlStyle == GestureConfig.POINTER_STYLE_LINE_ARROW,
            onClick = {
                onSettingsChange(settings.copy(pointerControlStyle = GestureConfig.POINTER_STYLE_LINE_ARROW))
            },
            label = { Text(t("直线箭头", "Line Arrow")) },
            leadingIcon = {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        FilterChip(
            selected = settings.pointerControlStyle == GestureConfig.POINTER_STYLE_TRACKER_CURSOR,
            onClick = {
                onSettingsChange(settings.copy(pointerControlStyle = GestureConfig.POINTER_STYLE_TRACKER_CURSOR))
            },
            label = { Text(t("摇杆光标", "Tracker Cursor")) },
            leadingIcon = {
                Icon(Icons.Rounded.RadioButtonChecked, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(color, CircleShape)
        )
        Text(
            t("当前指针颜色", "Current Pointer Color"),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ActionDropdownRow(
    title: String,
    gesture: String,
    selectedAction: String,
    onActionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val actions = actionValuesForGesture(gesture)
    val displayAction = selectedAction.takeIf { it in actions } ?: GestureConfig.ACTION_NONE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(actionLabel(displayAction))
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.ExpandMore, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                actions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(actionLabel(action)) },
                        onClick = {
                            expanded = false
                            onActionSelected(action)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PointerPreview(settings: SettingsState) {
    SettingsSection(t("动态预览", "Live Preview"), Icons.Rounded.Palette) {
        val color = settings.pointerColor
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp)
                    .padding(10.dp)
            ) {
                val stroke = Stroke(width = settings.pointerLineDp.dp.toPx(), cap = StrokeCap.Round)
                val alpha = settings.pointerControlAlpha.coerceIn(0, 255) / 255f
                val anchor = Offset(size.width * 0.82f, size.height * 0.66f)
                val controlRadius = settings.pointerRadiusDp.dp.toPx().coerceAtMost(size.height * 0.42f)

                if (alpha > 0f) {
                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = controlRadius,
                        center = anchor,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                if (settings.pointerControlStyle == GestureConfig.POINTER_STYLE_TRACKER_CURSOR) {
                    val cursor = Offset(size.width * 0.42f, size.height * 0.34f)
                    drawCircle(
                        color = color,
                        radius = (settings.trackerBallDp.dp.toPx() / 2f).coerceAtLeast(3.dp.toPx()),
                        center = anchor
                    )
                    drawLine(
                        color = color.copy(alpha = 0.45f),
                        start = anchor,
                        end = cursor,
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.20f),
                        radius = settings.trackerCursorDp.dp.toPx() / 2f,
                        center = cursor
                    )
                    drawCircle(
                        color = color,
                        radius = settings.trackerCursorDp.dp.toPx() / 2f,
                        center = cursor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else {
                    val start = Offset(size.width, anchor.y)
                    val end = Offset(size.width * 0.34f, size.height * 0.28f)
                    drawArrow(start, end, settings.pointerArrowDp.dp.toPx(), color, stroke)
                }
            }
        }
    }
}

@Composable
private fun EdgeRangePreview(settings: SettingsState) {
    SettingsSection(t("范围预览", "Range Preview"), Icons.Rounded.TouchApp) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(10.dp)
            ) {
                val phoneW = size.width * 0.42f
                val phoneH = size.height * 0.82f
                val left = (size.width - phoneW) / 2f
                val top = (size.height - phoneH) / 2f
                val right = left + phoneW
                val bottom = top + phoneH
                val edgePx = settings.edgeWidthDp.dp.toPx().coerceAtMost(phoneW * 0.16f)
                val distancePx = settings.swipeDistanceDp.dp.toPx().coerceAtMost(phoneW * 0.58f)
                val start = minOf(settings.triggerRegionStartPercent, settings.triggerRegionEndPercent)
                    .coerceIn(0, 100) / 100f
                val end = maxOf(settings.triggerRegionStartPercent, settings.triggerRegionEndPercent)
                    .coerceIn(0, 100) / 100f
                val activeTop = top + phoneH * start
                val activeBottom = top + phoneH * end

                drawRoundRect(
                    color = Color(0xFFFAFCFB),
                    topLeft = Offset(left, top),
                    size = Size(phoneW, phoneH),
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                )
                val inactive = Color(0xFFFFD642).copy(alpha = 0.32f)
                drawRect(inactive, Offset(left, top), Size(edgePx, phoneH))
                drawRect(inactive, Offset(right - edgePx, top), Size(edgePx, phoneH))
                drawRect(inactive, Offset(left, top), Size(phoneW, edgePx))
                drawRect(inactive, Offset(left, bottom - edgePx), Size(phoneW, edgePx))
                drawRect(
                    color = Color(0xFF00C853).copy(alpha = 0.72f),
                    topLeft = Offset(right - edgePx, activeTop),
                    size = Size(edgePx, activeBottom - activeTop)
                )
                drawRoundRect(
                    color = Color(0xFF39423D),
                    topLeft = Offset(left, top),
                    size = Size(phoneW, phoneH),
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                    style = Stroke(width = 1.4.dp.toPx())
                )

                val triggerY = (activeTop + activeBottom) / 2f
                val triggerX = right - edgePx / 2f
                val guideColor = Color(0xFF008DD2)
                drawLine(
                    color = guideColor,
                    start = Offset(triggerX, triggerY),
                    end = Offset(triggerX - distancePx, triggerY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                val radians = Math.toRadians(settings.swipeAngleDegrees.toDouble()).toFloat()
                listOf(Math.PI.toFloat() - radians, Math.PI.toFloat() + radians).forEach { angle ->
                    drawLine(
                        color = guideColor.copy(alpha = 0.72f),
                        start = Offset(triggerX, triggerY),
                        end = Offset(
                            triggerX + cos(angle) * distancePx * 0.72f,
                            triggerY + sin(angle) * distancePx * 0.72f
                        ),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(guideColor, radius = 4.dp.toPx(), center = Offset(triggerX, triggerY))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    arrowSize: Float,
    color: Color,
    stroke: Stroke
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = sqrt(dx * dx + dy * dy)
    if (length < 1f) return

    val ux = dx / length
    val uy = dy / length
    val wing = arrowSize * 0.62f
    val base = Offset(end.x - ux * arrowSize, end.y - uy * arrowSize)
    val left = Offset(base.x + -uy * wing, base.y + ux * wing)
    val right = Offset(base.x - -uy * wing, base.y - ux * wing)

    drawLine(color, start, end, strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(color, end, left, strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(color, end, right, strokeWidth = stroke.width, cap = StrokeCap.Round)
}

private fun edgeLabel(edge: String): String {
    return when (edge) {
        "left" -> t("左边缘", "Left Edge")
        "right" -> t("右边缘", "Right Edge")
        "top" -> t("上边缘", "Top Edge")
        "bottom" -> t("下边缘", "Bottom Edge")
        else -> edge
    }
}

private fun gestureLabel(gesture: String): String {
    return when (gesture) {
        "click" -> t("单击", "Tap")
        "double_click" -> t("双击", "Double Tap")
        "long_press" -> t("长按", "Long Press")
        "swipe_up" -> t("上划", "Swipe Up")
        "swipe_down" -> t("下划", "Swipe Down")
        "swipe_left" -> t("左划", "Swipe Left")
        "swipe_right" -> t("右划", "Swipe Right")
        else -> gesture
    }
}

private fun actionLabel(action: String): String {
    return when (action) {
        GestureConfig.ACTION_NONE -> t("无动作", "No Action")
        GestureConfig.ACTION_ONE_HAND_TAP -> t("单手点击屏幕", "One-Hand Tap")
        GestureConfig.ACTION_BACK -> t("返回", "Back")
        GestureConfig.ACTION_HOME -> t("主页", "Home")
        GestureConfig.ACTION_RECENTS -> t("最近任务", "Recents")
        else -> action
    }
}

private fun actionValuesForGesture(gesture: String): List<String> {
    return GestureConfig.actionValuesForGesture(gesture)
}
