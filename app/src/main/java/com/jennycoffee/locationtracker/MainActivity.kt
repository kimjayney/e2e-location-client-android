package com.jennycoffee.locationtracker

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jennycoffee.locationtracker.BuildConfig
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var lastLocationText: TextView
    private lateinit var buttonToggleTracking: Button
    private lateinit var buttonSettings: Button
    private lateinit var buttonViewWeb: Button

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001 // SettingsActivity와 다른 코드 사용
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 앱 최초 실행 시 기본값 설정
        AppPreferences.initialize(this)

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

        // 앱 시작 시 키 확인 및 생성
        checkAndGenerateKeysIfNeeded()
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
            updateStatus()
            Toast.makeText(this, "위치 추적이 중지되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            // 키가 설정되었는지 확인
            val deviceId = AppPreferences.getInput1(this)
            val deviceKey = AppPreferences.getInput2(this)
            if (deviceId.isEmpty() || deviceKey.isEmpty()) {
                Toast.makeText(this, "초기 설정이 필요합니다. 앱을 다시 시작하거나 설정을 초기화해주세요.", Toast.LENGTH_LONG).show()
                return
            }

            // 위치 추적 시작 전, 권한 확인 및 동의 절차
            checkPermissions()
        }
    }

    private fun checkAndGenerateKeysIfNeeded() {
        val deviceId = AppPreferences.getInput1(this)
        val deviceKey = AppPreferences.getInput2(this)
        val privateKey = AppPreferences.getInput3(this)

        // 키가 없으면 자동 생성
        if (deviceId.isEmpty() || deviceKey.isEmpty() || privateKey.isEmpty()) {
            showInitialSetupDialog()
        }
    }

    private fun showInitialSetupDialog() {
        AlertDialog.Builder(this)
            .setTitle("초기 설정 필요")
            .setMessage("위치 추적을 시작하기 위해 초기 설정이 필요합니다. 디바이스 ID와 키를 생성하고 서버에 등록합니다.")
            .setPositiveButton("설정 시작") { _, _ ->
                generateNewKeys()
            }
            .setNegativeButton("나중에") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
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
        DeviceRegistration.registerNewDevice(this, deviceId, deviceKey, shareControlKey)
        
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

    private fun checkPermissions() {
        Log.d(TAG, "checkPermissions 시작")
        try {
            // 백그라운드 위치 정보 사용에 대한 동의 팝업 표시
            showBackgroundLocationConsentDialog()
        } catch (e: Exception) {
            Log.e(TAG, "권한 체크 중 예외 발생", e)
        }
    }

    private fun showBackgroundLocationConsentDialog() {
        AlertDialog.Builder(this)
            .setTitle("백그라운드 위치 정보 사용 안내")
            .setMessage("이 앱은 화면이 꺼져있거나 다른 앱을 사용하는 중에도 실시간으로 위치를 추적하고 공유하기 위해 백그라운드에서 사용자의 위치 데이터를 수집합니다. 이 기능을 사용하려면 '항상 허용'으로 위치 권한을 설정하고 알림 권한을 허용해야 합니다.\n\n동의하십니까?")
            .setPositiveButton("동의 및 계속하기") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "권한에 동의해야 위치 추적을 시작할 수 있습니다.", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun requestPermissions() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "필요한 권한 요청: $permissionsToRequest")
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            Log.d(TAG, "모든 권한이 이미 부여됨, 서비스 시작")
            startLocationService()
            Toast.makeText(this, "위치 추적이 시작되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "모든 요청된 권한 허용됨")
                startLocationService()
                Toast.makeText(this, "위치 추적이 시작되었습니다", Toast.LENGTH_SHORT).show()

            } else {
                Log.d(TAG, "하나 이상의 권한이 거부됨")
                Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            startActivity(intent)
        }
    }

    private fun startLocationService() {
        AppPreferences.resumeTracking(this)
        val serviceIntent = Intent(this, LocationService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        updateStatus()
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
        // AppPreferences.stopTracking(this)는 toggleLocationTracking에서 이미 호출됨
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
}
