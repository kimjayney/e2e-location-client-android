package com.jennycoffee.locationtracker

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object AppPreferences {
    private const val PREF_NAME = "LocationTrackerPrefs"
    private const val KEY_INPUT1 = "input1"
    private const val KEY_INPUT2 = "input2"
    private const val KEY_INPUT3 = "input3"
    private const val KEY_BATTERY_MODE = "battery_mode"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_TRACKING_PAUSED_UNTIL = "tracking_paused_until"
    private const val KEY_TRACKING_STOPPED = "tracking_stopped"
    private const val KEY_LAST_LATITUDE = "last_latitude"
    private const val KEY_LAST_LONGITUDE = "last_longitude"
    private const val KEY_LAST_SEND_TIME = "last_send_time"
    private const val KEY_SEND_COUNT = "send_count"
    private const val KEY_SEND_LOGS = "send_logs"
    private const val KEY_SHARE_STATUS = "share_status"
    private const val KEY_SHARE_CONTROL_KEY = "shareControlKey"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val MAX_LOG_ENTRIES = 100
    private const val KEY_FIRST_RUN = "first_run"

    fun saveInputs(context: Context, input1: String, input2: String, input3: String) {
        android.util.Log.d("AppPreferences", "saveInputs 시작: input1=$input1, input2=$input2, input3=$input3")
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_INPUT1, input1)
                .putString(KEY_INPUT2, input2)
                .putString(KEY_INPUT3, input3)
                .apply()
            android.util.Log.d("AppPreferences", "saveInputs 완료")
        } catch (e: Exception) {
            android.util.Log.e("AppPreferences", "saveInputs 실패", e)
        }
    }

    fun getInput1(context: Context): String {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val result = prefs.getString(KEY_INPUT1, "") ?: ""
            android.util.Log.d("AppPreferences", "getInput1: $result")
            return result
        } catch (e: Exception) {
            android.util.Log.e("AppPreferences", "getInput1 실패", e)
            return ""
        }
    }

    fun getInput2(context: Context): String {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val result = prefs.getString(KEY_INPUT2, "") ?: ""
            android.util.Log.d("AppPreferences", "getInput2: $result")
            return result
        } catch (e: Exception) {
            android.util.Log.e("AppPreferences", "getInput2 실패", e)
            return ""
        }
    }

    fun getInput3(context: Context): String {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val result = prefs.getString(KEY_INPUT3, "") ?: ""
            android.util.Log.d("AppPreferences", "getInput3: $result")
            return result
        } catch (e: Exception) {
            android.util.Log.e("AppPreferences", "getInput3 실패", e)
            return ""
        }
    }

    // 배터리 절약 모드 설정
    fun saveBatteryMode(context: Context, mode: String) {
        android.util.Log.d("AppPreferences", "saveBatteryMode: $mode")
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_BATTERY_MODE, mode).apply()
            android.util.Log.d("AppPreferences", "saveBatteryMode 완료")
        } catch (e: Exception) {
            android.util.Log.e("AppPreferences", "saveBatteryMode 실패", e)
        }
    }

    fun getBatteryMode(context: Context): String {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val result = prefs.getString(KEY_BATTERY_MODE, "normal") ?: "normal"
            android.util.Log.d("AppPreferences", "getBatteryMode: $result")
            return result
        } catch (e: Exception) {
            android.util.Log.e("AppPreferences", "getBatteryMode 실패", e)
            return "normal"
        }
    }

    // 배터리 모드별 설정값 반환
    fun getLocationInterval(context: Context): Long {
        return when (getBatteryMode(context)) {
            "ultra" -> 300000L  // 5분
            "power" -> 120000L  // 2분
            else -> 60000L      // 1분 (일반)
        }
    }

    fun getLocationDistance(context: Context): Float {
        return when (getBatteryMode(context)) {
            "ultra" -> 100f     // 100미터
            "power" -> 50f      // 50미터
            else -> 30f         // 30미터 (일반)
        }
    }

    // 언어 설정
    fun saveLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "ko") ?: "ko"
    }

    // 위치 추적 제어
    fun pauseTracking(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val pauseUntil = System.currentTimeMillis() + (minutes * 60 * 1000L)
        prefs.edit()
            .putLong(KEY_TRACKING_PAUSED_UNTIL, pauseUntil)
            .putBoolean(KEY_TRACKING_STOPPED, false)
            .apply()
    }

    fun stopTracking(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_TRACKING_PAUSED_UNTIL, 0L)
            .putBoolean(KEY_TRACKING_STOPPED, true)
            .apply()
    }

    fun resumeTracking(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_TRACKING_PAUSED_UNTIL, 0L)
            .putBoolean(KEY_TRACKING_STOPPED, false)
            .apply()
    }

    fun isTrackingPaused(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // 앱 최초 실행 시 기본값은 true (중지 상태)
        val isStopped = prefs.getBoolean(KEY_TRACKING_STOPPED, true)
        val pauseUntil = prefs.getLong(KEY_TRACKING_PAUSED_UNTIL, 0L)
        
        if (isStopped) return true
        if (pauseUntil > 0 && System.currentTimeMillis() < pauseUntil) return true
        return false
    }

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_FIRST_RUN, true)) {
            prefs.edit()
                .putBoolean(KEY_TRACKING_STOPPED, true) // 최초 실행 시 추적 중지 상태로 설정
                .putBoolean(KEY_FIRST_RUN, false)
                .apply()
        }
    }

    fun isTrackingActive(context: Context): Boolean {
        return !isTrackingPaused(context)
    }

    fun getPauseRemainingMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val pauseUntil = prefs.getLong(KEY_TRACKING_PAUSED_UNTIL, 0L)
        if (pauseUntil <= 0) return 0
        
        val remaining = pauseUntil - System.currentTimeMillis()
        return if (remaining > 0) (remaining / (60 * 1000)).toInt() else 0
    }

    fun getPauseUntilTime(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val pauseUntil = prefs.getLong(KEY_TRACKING_PAUSED_UNTIL, 0L)
        if (pauseUntil <= 0) return ""
        
        val date = java.util.Date(pauseUntil)
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return formatter.format(date)
    }

    // 마지막 위치 정보 저장/조회
    fun saveLastLocation(context: Context, latitude: Double, longitude: Double) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_LAST_LATITUDE, latitude.toFloat())
            .putFloat(KEY_LAST_LONGITUDE, longitude.toFloat())
            .apply()
    }

    fun getLastLatitude(context: Context): Double {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_LAST_LATITUDE, 0f).toDouble()
    }

    fun getLastLongitude(context: Context): Double {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_LAST_LONGITUDE, 0f).toDouble()
    }

    // 위치 전송 기록 저장/조회
    fun saveLocationSendRecord(context: Context, latitude: Double, longitude: Double) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        val currentCount = prefs.getInt(KEY_SEND_COUNT, 0)
        
        prefs.edit()
            .putLong(KEY_LAST_SEND_TIME, currentTime)
            .putInt(KEY_SEND_COUNT, currentCount + 1)
            .putFloat(KEY_LAST_LATITUDE, latitude.toFloat())
            .putFloat(KEY_LAST_LONGITUDE, longitude.toFloat())
            .apply()
    }

    fun getLastSendTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SEND_TIME, 0L)
    }

    fun getSendCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SEND_COUNT, 0)
    }

    fun getLastSendTimeFormatted(context: Context): String {
        val lastSendTime = getLastSendTime(context)
        if (lastSendTime == 0L) return "전송 기록 없음"
        
        val date = Date(lastSendTime)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
    }

    fun getTimeSinceLastSend(context: Context): String {
        val lastSendTime = getLastSendTime(context)
        if (lastSendTime == 0L) return "전송 기록 없음"
        
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - lastSendTime
        
        return when {
            diff < 60000 -> "${diff / 1000}초 전"
            diff < 3600000 -> "${diff / 60000}분 전"
            diff < 86400000 -> "${diff / 3600000}시간 전"
            else -> "${diff / 86400000}일 전"
        }
    }

    // 위치 공유 상태 관리
    fun saveShareStatus(context: Context, isAllowed: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SHARE_STATUS, isAllowed)
            .apply()
    }

    fun getShareStatus(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHARE_STATUS, true) // 기본값은 허용(true)
    }

    fun isLocationSharingAllowed(context: Context): Boolean {
        return getShareStatus(context)
    }

    // 외부에서 위치 공유 상태 조회 (API용)
    fun getShareStatusForAPI(context: Context): String {
        return if (getShareStatus(context)) "1" else "0"
    }

    // 서버에서 위치 공유 상태 동기화
    fun syncShareStatusFromServer(context: Context, serverStatus: String) {
        val isAllowed = serverStatus == "1"
        saveShareStatus(context, isAllowed)
    }
    
    // MARK: - Share Control Key
    fun saveShareControlKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SHARE_CONTROL_KEY, key).apply()
    }
    
    fun getShareControlKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SHARE_CONTROL_KEY, "") ?: ""
    }
    
    fun generateAndSaveShareControlKey(context: Context): String {
        val key = createRandomString(16)
        saveShareControlKey(context, key)
        return key
    }
    
    // MARK: - Random String Generation
    internal fun createRandomString(length: Int): String {
        val allowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { allowed.random() }.joinToString("")
    }

    // 알림 설정
    fun saveNotificationsEnabled(context: Context, isEnabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isEnabled).apply()
    }

    fun getNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // 기본값은 false (차단)
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
    }

    // FCM 토큰 저장
    fun saveFcmToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    // FCM 토큰 조회
    fun getFcmToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FCM_TOKEN, "") ?: ""
    }
}
