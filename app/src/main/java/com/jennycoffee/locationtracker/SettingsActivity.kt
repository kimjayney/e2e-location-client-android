package com.jennycoffee.locationtracker

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL

class SettingsActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private val TAG = "SettingsActivity"

    private lateinit var buttonSave: Button
    private lateinit var buttonBack: Button
    private lateinit var buttonResetSharedUrl: Button
    private lateinit var batteryModeSpinner: Spinner
    private lateinit var notificationSwitch: Switch
    private lateinit var buttonManagePush: Button
    private lateinit var shareStatusSpinner: Spinner
    private lateinit var currentSettingsText: TextView

    // ViewModel 인스턴스 생성
    private val viewModel: SettingsViewModel by viewModels()

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
            notificationSwitch = findViewById(R.id.notificationSwitch)
            buttonManagePush = findViewById(R.id.buttonManagePush)
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
            initializeSwitch()
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

            // 푸시 알림 관리 버튼 리스너
            buttonManagePush.setOnClickListener { view ->
                animateButtonClick(view)
                val intent = Intent(this, PushManagementActivity::class.java)
                startActivity(intent)
            }
            Log.d(TAG, "버튼 리스너 설정 완료")
        } catch (e: Exception) {
            Log.e(TAG, "버튼 리스너 설정 실패", e)
        }

        Log.d(TAG, "onCreate 완료")

        // 앱 시작 시 키 확인 및 생성
        checkAndGenerateKeysIfNeeded()
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

        // 위치 공유 상태 Spinner 초기화
        val shareStatuses = arrayOf("위치 공유 허용", "위치 공유 차단")
        val shareAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, shareStatuses)
        shareAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        shareStatusSpinner.adapter = shareAdapter

        // 현재 공유 상태 설정
        val currentShareStatus = AppPreferences.getShareStatus(this)
        shareStatusSpinner.setSelection(if (currentShareStatus) 0 else 1)

        // 스피너 선택 리스너 추가
        shareStatusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 이 리스너는 UI 초기화 시에도 호출될 수 있으므로, 실제 사용자 선택 시에만 동작하도록 로직을 추가할 수 있습니다.
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initializeSwitch() {
        // ViewModel의 상태를 스위치에 반영
        notificationSwitch.isChecked = viewModel.isNotificationsEnabled

        // 스위치 클릭 시, ViewModel에 이벤트 전달
        notificationSwitch.setOnClickListener {
            val deviceId = AppPreferences.getInput1(this)
            val deviceKey = AppPreferences.getInput2(this)
            if (deviceId.isEmpty() || deviceKey.isEmpty()) {
                (it as Switch).isChecked = !it.isChecked // 상태 원상 복구
                Toast.makeText(this, "초기 설정이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.onNotificationSwitchClicked((it as Switch).isChecked)
        }

        // ViewModel의 UI 상태 변경을 감지하고 UI 업데이트
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    notificationSwitch.isEnabled = false
                }
                is UiState.Success -> {
                    notificationSwitch.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is UiState.Error -> {
                    notificationSwitch.isEnabled = true
                    notificationSwitch.isChecked = !notificationSwitch.isChecked // 실패 시 스위치 원상 복구
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkAndGenerateKeysIfNeeded() {
        val deviceId = AppPreferences.getInput1(this)
        val deviceKey = AppPreferences.getInput2(this)

        // 키가 없으면 자동 생성 다이얼로그 표시
        if (deviceId.isEmpty() || deviceKey.isEmpty()) {
            showInitialSetupDialog()
        }
    }

    private fun showInitialSetupDialog() {
        AlertDialog.Builder(this)
            .setTitle("초기 설정 필요")
            .setMessage("위치 추적을 시작하기 위해 초기 설정이 필요합니다. 디바이스 ID와 키를 생성하고 서버에 등록합니다.")
            .setPositiveButton("설정 시작") { _, _ ->
                resetSharedUrl()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "초기 설정이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
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
        DeviceRegistration.registerNewDevice(this, newDeviceId, newDeviceKey, newShareControlKey)
        
        Toast.makeText(this, "공유 URL이 초기화되었습니다", Toast.LENGTH_SHORT).show()
    }

    internal fun createRandomString(length: Int): String {
        val allowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { allowed.random() }
            .joinToString("")
    }

    internal fun createSecureKey(length: Int): String {
        val allowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"#${'$'}%&'()*+,-./:;<=>?@[\\]^_`{|}~"
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
            
            // 위치 공유 상태 저장
            val newShareStatus = shareStatusSpinner.selectedItemPosition == 0 // 0: 허용, 1: 차단
            val oldShareStatus = AppPreferences.getShareStatus(this)
            
            if (newShareStatus != oldShareStatus) {
                AppPreferences.saveShareStatus(this, newShareStatus)
                // 서버 업데이트 로직을 별도로 호출
                updateShareStatusOnServer(newShareStatus)
            }

            Log.d(TAG, "설정 저장 완료")
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            updateCurrentSettings()

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
        val input1 = AppPreferences.getInput1(this)
        val input2 = AppPreferences.getInput2(this)

        if (input1.isEmpty() || input2.isEmpty()) {
            Log.d(TAG, "설정이 완료되지 않아 위치 추적을 시작하지 않습니다")
            return
        }

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
                Log.d(TAG, "권한 허용됨")
                // 백그라운드 위치 권한 추가 요청 (Android 10 이상)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        AlertDialog.Builder(this)
                            .setTitle("백그라운드 위치 권한 필요")
                            .setMessage("정확한 위치 추적을 위해, 다음 화면에서 위치 권한을 '항상 허용'으로 설정해주세요.")
                            .setPositiveButton("설정으로 이동") { _, _ -> requestBackgroundLocationPermission() }
                            .show()
                    }
                }
                startLocationService()
            } else {
                Log.d(TAG, "위치 권한 거부됨")
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
} 