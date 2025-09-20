package com.jennycoffee.locationtracker

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jennycoffee.locationtracker.BuildConfig
import android.util.Base64
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var lastLocationText: TextView
    private lateinit var buttonToggleTracking: Button
    private lateinit var buttonSettings: Button
    private lateinit var buttonViewWeb: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI 요소 초기화
        statusText = findViewById(R.id.statusText)
        lastLocationText = findViewById(R.id.lastLocationText)
        buttonToggleTracking = findViewById(R.id.buttonToggleTracking)
        buttonSettings = findViewById(R.id.buttonSettings)
        buttonViewWeb = findViewById(R.id.buttonViewWeb)

        // 위치 추적 토글 버튼 클릭 리스너
        buttonToggleTracking.setOnClickListener { view ->
            animateButtonClick(view)
            toggleLocationTracking()
        }

        // 설정 버튼 클릭 리스너
        buttonSettings.setOnClickListener { view ->
            animateButtonClick(view)
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 웹에서 위치 보기 버튼 클릭 리스너
        buttonViewWeb.setOnClickListener { view ->
            animateButtonClick(view)
            openWebLocation()
        }

        // 상태 업데이트
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun toggleLocationTracking() {
        if (AppPreferences.isTrackingActive(this)) {
            // 위치 추적 중지
            AppPreferences.stopTracking(this)
            stopLocationService()
            Toast.makeText(this, "위치 추적이 중지되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            // 위치 추적 시작
            if (checkAndGenerateKeysIfNeeded()) {
                AppPreferences.resumeTracking(this)
                startLocationService()
                Toast.makeText(this, "위치 추적이 시작되었습니다", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 약간의 지연 후 상태 업데이트 (SharedPreferences 반영을 위해)
        buttonToggleTracking.postDelayed({
            updateStatus()
        }, 100)
    }

    private fun checkAndGenerateKeysIfNeeded(): Boolean {
        val deviceId = AppPreferences.getInput1(this)
        val deviceKey = AppPreferences.getInput2(this)
        val privateKey = AppPreferences.getInput3(this)

        // 키가 없으면 자동 생성
        if (deviceId.isEmpty() || deviceKey.isEmpty() || privateKey.isEmpty()) {
            generateNewKeys()
            return true
        }
        return true
    }

    private fun generateNewKeys() {
        // 16자리 영문자+숫자 랜덤 문자열 생성
        val deviceId = createRandomString(16)
        val deviceKey = createRandomString(16)
        val privateKey = createSecureKey(32)
        val shareControlKey = createRandomString(16)
        
        // 키 저장
        AppPreferences.saveInputs(this, deviceId, deviceKey, privateKey)
        AppPreferences.saveShareControlKey(this, shareControlKey)
        
        // 디바이스 등록 API 호출
        registerNewDevice(deviceId, deviceKey, shareControlKey)
        
        Toast.makeText(this, "새로운 키가 자동 생성되었습니다", Toast.LENGTH_SHORT).show()
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

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }

    private fun openWebLocation() {
        // 위치 공유가 차단된 경우 확인
        if (!AppPreferences.isLocationSharingAllowed(this)) {
            Toast.makeText(this, "위치 공유가 차단되어 있습니다. 설정에서 허용해주세요.", Toast.LENGTH_LONG).show()
            return
        }

        val id = AppPreferences.getInput1(this)
        val deviceKey = AppPreferences.getInput2(this)
        val privateKey = AppPreferences.getInput3(this)

        if (id.isEmpty() || deviceKey.isEmpty()) {
            Toast.makeText(this, "먼저 디바이스 ID와 키를 설정해주세요", Toast.LENGTH_LONG).show()
            return
        }

        // privateKey를 Base64로 인코딩
        val encodedPrivateKey = Base64.encodeToString(
            privateKey.toByteArray(StandardCharsets.UTF_8), 
            Base64.NO_WRAP
        )

        // 현재 시간대 오프셋을 시간 단위로 계산
        val timezoneOffsetInMillis = java.util.TimeZone.getDefault().rawOffset
        val timezoneOffsetInHours = timezoneOffsetInMillis / (1000 * 60 * 60)

        val url = "${BuildConfig.WEB_URL}#locationui?deviceId=$id&deviceKey=$deviceKey&privateKey=$encodedPrivateKey&base64=true&timezone=$timezoneOffsetInHours"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun updateStatus() {
        // 위치 추적 상태 확인
        val isTracking = AppPreferences.isTrackingActive(this)
        val isPaused = AppPreferences.isTrackingPaused(this)
        
        val status = when {
            isPaused -> "위치 추적이 일시 중지되었습니다"
            isTracking -> "위치 추적이 활성화되었습니다"
            else -> "위치 추적이 비활성화되었습니다"
        }
        
        statusText.text = status

        // 버튼 텍스트 업데이트
        buttonToggleTracking.text = if (isTracking) getString(R.string.stop_tracking_main) else getString(R.string.start_tracking)

        // 마지막 위치 정보 표시
        val lastLat = AppPreferences.getLastLatitude(this)
        val lastLng = AppPreferences.getLastLongitude(this)
        
        if (lastLat != 0.0 && lastLng != 0.0) {
            lastLocationText.text = "마지막 위치: ${String.format("%.6f", lastLat)}, ${String.format("%.6f", lastLng)}"
        } else {
            lastLocationText.text = "위치 정보가 없습니다"
        }
    }

    private fun animateButtonClick(view: View) {
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
    }

    private fun registerNewDevice(deviceId: String, deviceKey: String, shareControlKey: String) {
        Thread {
            try {
                val url = BuildConfig.WEB_URL + "/api/device/register?device=$deviceId&authorization=$deviceKey&shareControlKey=$shareControlKey"
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                val responseBody = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                }

                runOnUiThread {
                    if (responseCode == 200 && responseBody != null) {
                        try {
                            val jsonResponse = org.json.JSONObject(responseBody)
                            val status = jsonResponse.optBoolean("status", false)
                            val message = jsonResponse.optString("message_ko_KR", "알 수 없는 응답")

                            if (status) { // 새로운 기기 등록 성공
                                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                                // 웹 브라우저 열기
                                val recapthaUrl = "${BuildConfig.WEB_URL}/recaptha?deviceid=$deviceId&authorization=$deviceKey&shareControlKey=$shareControlKey"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recapthaUrl))
                                startActivity(intent)
                            } else { // 이미 등록된 기기
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: org.json.JSONException) {
                            Toast.makeText(this, "응답 처리 중 오류 발생", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "디바이스 등록 실패: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "디바이스 등록 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
