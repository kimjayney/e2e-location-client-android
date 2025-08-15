package com.jennycoffee.locationtracker

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.jennycoffee.locationtracker.BuildConfig
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private val TAG = "SettingsActivity"

    private lateinit var buttonSave: Button
    private lateinit var buttonBack: Button
    private lateinit var buttonResetSharedUrl: Button
    private lateinit var batteryModeSpinner: Spinner
    private lateinit var trackingControlSpinner: Spinner
    private lateinit var shareStatusSpinner: Spinner
    private lateinit var currentSettingsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate 시작")
        
        try {
            setContentView(R.layout.activity_settings)
            Log.d(TAG, "레이아웃 로드 완료")
        } catch (e: Exception) {
            Log.e(TAG, "레이아웃 로드 실패", e)
            Toast.makeText(this, "화면 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            // UI 요소 초기화
            buttonSave = findViewById(R.id.buttonSave)
            buttonBack = findViewById(R.id.buttonBack)
            buttonResetSharedUrl = findViewById(R.id.buttonResetSharedUrl)
            batteryModeSpinner = findViewById(R.id.batteryModeSpinner)
            trackingControlSpinner = findViewById(R.id.trackingControlSpinner)
            shareStatusSpinner = findViewById(R.id.shareStatusSpinner)
            currentSettingsText = findViewById(R.id.currentSettingsText)
            Log.d(TAG, "UI 요소 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "UI 요소 초기화 실패", e)
            Toast.makeText(this, "UI 초기화 실패: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            // Spinner 초기화
            initializeSpinners()
            Log.d(TAG, "Spinner 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "Spinner 초기화 실패", e)
        }

        try {
            // 현재 설정 표시
            updateCurrentSettings()
            Log.d(TAG, "현재 설정 표시 완료")
        } catch (e: Exception) {
            Log.e(TAG, "현재 설정 표시 실패", e)
        }

        try {
            // 공유 URL 초기화 버튼 클릭 리스너
            buttonResetSharedUrl.setOnClickListener { view ->
                Log.d(TAG, "공유 URL 초기화 버튼 클릭됨")
                animateButtonClick(view)
                showResetSharedUrlDialog()
            }

            // 저장 버튼 클릭 리스너
            buttonSave.setOnClickListener { view ->
                Log.d(TAG, "저장 버튼 클릭됨")
                animateButtonClick(view)
                saveSettings()
            }

            // 뒤로가기 버튼 클릭 리스너
            buttonBack.setOnClickListener { view ->
                Log.d(TAG, "뒤로가기 버튼 클릭됨")
                animateButtonClick(view)
                finish()
            }
            Log.d(TAG, "버튼 리스너 설정 완료")
        } catch (e: Exception) {
            Log.e(TAG, "버튼 리스너 설정 실패", e)
        }

        Log.d(TAG, "onCreate 완료")
    }

    private fun initializeSpinners() {
        // 배터리 모드 Spinner 초기화
        val batteryModes = arrayOf("일반 모드", "절약 모드", "초절약 모드")
        val batteryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, batteryModes)
        batteryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        batteryModeSpinner.adapter = batteryAdapter

        // 현재 배터리 모드 설정
        val currentBatteryMode = AppPreferences.getBatteryMode(this)
        val batteryModeIndex = when (currentBatteryMode) {
            "normal" -> 0
            "power" -> 1
            "ultra" -> 2
            else -> 0
        }
        batteryModeSpinner.setSelection(batteryModeIndex)

        // 위치 추적 제어 Spinner 초기화
        val trackingControls = arrayOf("추적 재개", "5분 동안 일시정지", "10분 동안 일시정지", "30분 동안 일시정지")
        val trackingAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, trackingControls)
        trackingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        trackingControlSpinner.adapter = trackingAdapter

        // 현재 추적 상태 설정
        if (AppPreferences.isTrackingPaused(this)) {
            trackingControlSpinner.setSelection(1) // 기본값으로 5분 일시정지
        } else {
            trackingControlSpinner.setSelection(0) // 추적 재개
        }

        // 위치 공유 상태 Spinner 초기화
        val shareStatuses = arrayOf("위치 공유 허용", "위치 공유 차단")
        val shareAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, shareStatuses)
        shareAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        shareStatusSpinner.adapter = shareAdapter

        // 현재 공유 상태 설정
        val currentShareStatus = AppPreferences.getShareStatus(this)
        shareStatusSpinner.setSelection(if (currentShareStatus) 0 else 1)
    }

    private fun resetSharedUrl() {
        // 새로운 키 생성
        val newDeviceId = createRandomString(16)
        val newDeviceKey = createRandomString(16)
        val newPrivateKey = createSecureKey(32)
        val newShareControlKey = createRandomString(16)
        
        // 키 저장
        AppPreferences.saveInputs(this, newDeviceId, newDeviceKey, newPrivateKey)
        AppPreferences.saveShareControlKey(this, newShareControlKey)
        
        // 디바이스 등록 API 호출
        registerNewDevice(newDeviceId, newDeviceKey, newShareControlKey)
        
        Toast.makeText(this, "공유 URL이 초기화되었습니다", Toast.LENGTH_SHORT).show()
    }

    private fun registerNewDevice(deviceId: String, deviceKey: String, shareControlKey: String) {
        Thread {
            try {
                val url = BuildConfig.SERVER_URL + "/api/device/register?device=$deviceId&authorization=$deviceKey&shareControlKey=$shareControlKey"
                Log.d(TAG, "디바이스 등록 API 호출: $url")
                
                val urlObj = URL(url)
                val connection = urlObj.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                var responseMessage = ""
                
                try {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        responseMessage = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d(TAG, "디바이스 등록 성공: $responseMessage")
                    } else {
                        responseMessage = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "에러 응답 없음"
                        Log.e(TAG, "디바이스 등록 실패: HTTP $responseCode - $responseMessage")
                    }
                } catch (e: Exception) {
                    responseMessage = "응답 읽기 실패: ${e.message}"
                    Log.e(TAG, "응답 읽기 실패", e)
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                Log.e(TAG, "디바이스 등록 API 호출 실패: ${e.message}")
            }
        }.start()
    }

    internal fun createRandomString(length: Int): String {
        val allowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { allowed.random() }
            .joinToString("")
    }

    internal fun createSecureKey(length: Int): String {
        val allowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
        return (1..length)
            .map { allowed.random() }
            .joinToString("")
    }

    private fun saveSettings() {
        Log.d(TAG, "saveSettings 시작")
        
        try {
            // 배터리 모드 저장
            val selectedBatteryMode = when (batteryModeSpinner.selectedItemPosition) {
                0 -> "normal"
                1 -> "power"
                2 -> "ultra"
                else -> "normal"
            }
            Log.d(TAG, "선택된 배터리 모드: $selectedBatteryMode")
            
            AppPreferences.saveBatteryMode(this, selectedBatteryMode)
            
            // 위치 추적 제어 처리
            when (trackingControlSpinner.selectedItemPosition) {
                0 -> { // 추적 재개
                    Log.d(TAG, "추적 재개 처리")
                    AppPreferences.resumeTracking(this)
                }
                1 -> { // 5분 일시정지
                    Log.d(TAG, "5분 일시정지 처리")
                    AppPreferences.pauseTracking(this, 5)
                    Toast.makeText(this, getString(R.string.tracking_paused), Toast.LENGTH_SHORT).show()
                }
                2 -> { // 10분 일시정지
                    Log.d(TAG, "10분 일시정지 처리")
                    AppPreferences.pauseTracking(this, 10)
                    Toast.makeText(this, getString(R.string.tracking_paused), Toast.LENGTH_SHORT).show()
                }
                3 -> { // 30분 일시정지
                    Log.d(TAG, "30분 일시정지 처리")
                    AppPreferences.pauseTracking(this, 30)
                    Toast.makeText(this, getString(R.string.tracking_paused), Toast.LENGTH_SHORT).show()
                }
            }

            // 위치 공유 상태 저장 및 서버 업데이트
            val newShareStatus = shareStatusSpinner.selectedItemPosition == 0 // 0: 허용, 1: 차단
            val oldShareStatus = AppPreferences.getShareStatus(this)
            
            if (newShareStatus != oldShareStatus) {
                AppPreferences.saveShareStatus(this, newShareStatus)
                updateShareStatusOnServer(newShareStatus)
            }

            Log.d(TAG, "설정 저장 완료")
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            updateCurrentSettings()

            // 권한 체크 및 서비스 시작 (일시정지가 아닌 경우에만)
            if (trackingControlSpinner.selectedItemPosition == 0) {
                Log.d(TAG, "권한 체크 시작")
                checkPermissions()
            }

            Log.d(TAG, "saveSettings 완료")
            
        } catch (e: Exception) {
            Log.e(TAG, "saveSettings에서 예외 발생", e)
            Toast.makeText(this, "설정 저장 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateShareStatusOnServer(isAllowed: Boolean) {
        val deviceId = AppPreferences.getInput1(this)
        val deviceKey = AppPreferences.getInput2(this)
        val shareValue = if (isAllowed) "1" else "0"
        
        // shareControlKey 가져오기 (없으면 새로 생성)
        var shareControlKey = AppPreferences.getShareControlKey(this)
        if (shareControlKey.isEmpty()) {
            shareControlKey = AppPreferences.generateAndSaveShareControlKey(this)
        }
        
        val url = BuildConfig.SERVER_URL + "/api/sharecontrol?device=$deviceId&authorization=$deviceKey&share=$shareValue&shareControlKey=$shareControlKey"
        Log.d(TAG, "위치 공유 상태 업데이트 API 호출: $url")
        
        Thread {
            try {
                val urlObj = URL(url)
                val connection = urlObj.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                var responseMessage = ""
                
                try {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        responseMessage = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d(TAG, "위치 공유 상태 업데이트 성공: $responseMessage")
                        
                        // UI 업데이트는 메인 스레드에서
                        runOnUiThread {
                            Toast.makeText(this, getString(R.string.sharing_status_updated), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        responseMessage = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "에러 응답 없음"
                        Log.e(TAG, "위치 공유 상태 업데이트 실패: HTTP $responseCode - $responseMessage")
                    }
                } catch (e: Exception) {
                    responseMessage = "응답 읽기 실패: ${e.message}"
                    Log.e(TAG, "응답 읽기 실패", e)
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                Log.e(TAG, "위치 공유 상태 업데이트 API 호출 실패: ${e.message}")
            }
        }.start()
    }

    private fun updateCurrentSettings() {
        Log.d(TAG, "updateCurrentSettings 시작")
        try {
            val interval = AppPreferences.getLocationInterval(this)
            val distance = AppPreferences.getLocationDistance(this)

            val intervalText = when (interval) {
                60000L -> "1분"
                120000L -> "2분"
                300000L -> "5분"
                else -> "${interval / 1000}초"
            }

            val distanceText = "${distance.toInt()}미터"

            currentSettingsText.text = getString(R.string.current_settings, intervalText, distanceText)
            Log.d(TAG, "현재 설정 업데이트: $intervalText 간격, $distanceText 거리")
        } catch (e: Exception) {
            Log.e(TAG, "현재 설정 업데이트 실패", e)
        }
    }

    private fun checkPermissions() {
        Log.d(TAG, "checkPermissions 시작")
        try {
            val input1 = AppPreferences.getInput1(this)
            val input2 = AppPreferences.getInput2(this)

            if (input1.isEmpty() || input2.isEmpty()) {
                Log.d(TAG, "설정이 완료되지 않아 위치 추적을 시작하지 않습니다")
                return
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "위치 권한 요청")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            } else {
                Log.d(TAG, "위치 권한이 이미 부여됨, 서비스 시작")
                startLocationService()
            }
        } catch (e: Exception) {
            Log.e(TAG, "권한 체크 중 예외 발생", e)
        }
    }

    private fun startLocationService() {
        Log.d(TAG, "startLocationService 시작")
        try {
            val input1 = AppPreferences.getInput1(this)
            val input2 = AppPreferences.getInput2(this)

            if (input1.isEmpty() || input2.isEmpty()) {
                Log.d(TAG, "설정이 완료되지 않아 서비스를 시작하지 않습니다")
                return
            }

            val serviceIntent = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d(TAG, "위치 서비스 시작 완료")
            Toast.makeText(this, "위치 추적이 시작되었습니다", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "위치 서비스 시작 실패", e)
            Toast.makeText(this, "서비스 시작 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun animateButtonClick(view: View) {
        try {
            val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f)
            val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)
            val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
            val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)

            scaleDown.duration = 100
            scaleDownY.duration = 100
            scaleUp.duration = 100
            scaleUpY.duration = 100

            scaleDown.start()
            scaleDownY.start()

            scaleDown.addUpdateListener {
                if (it.animatedFraction >= 1f) {
                    scaleUp.start()
                    scaleUpY.start()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "버튼 애니메이션 실패", e)
        }
    }

    private fun showResetSharedUrlDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.reset_shared_url_title))
        builder.setMessage(getString(R.string.reset_shared_url_message))
        builder.setPositiveButton(getString(R.string.yes)) { _, _ ->
            resetSharedUrl()
        }
        builder.setNegativeButton(getString(R.string.no)) { _, _ ->
            // 사용자가 취소한 경우
        }
        builder.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: requestCode=$requestCode")
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "위치 권한 허용됨, 서비스 시작")
                startLocationService()
            } else {
                Log.d(TAG, "위치 권한 거부됨")
                Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
} 