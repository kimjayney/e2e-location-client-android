package com.jennycoffee.locationtracker

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast

class SettingsActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private val TAG = "SettingsActivity"

    private lateinit var editText1: EditText
    private lateinit var editText2: EditText
    private lateinit var editText3: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonBack: Button
    private lateinit var batteryModeGroup: RadioGroup
    private lateinit var currentSettingsText: TextView
    private lateinit var trackingControlGroup: RadioGroup

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
            editText1 = findViewById(R.id.editText1)
            editText2 = findViewById(R.id.editText2)
            editText3 = findViewById(R.id.editText3)
            buttonSave = findViewById(R.id.buttonSave)
            buttonBack = findViewById(R.id.buttonBack)
            batteryModeGroup = findViewById(R.id.batteryModeGroup)
            currentSettingsText = findViewById(R.id.currentSettingsText)
            trackingControlGroup = findViewById(R.id.trackingControlGroup)
            Log.d(TAG, "UI 요소 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "UI 요소 초기화 실패", e)
            Toast.makeText(this, "UI 초기화 실패: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            // 저장된 설정 불러오기
            editText1.setText(AppPreferences.getInput1(this))
            editText2.setText(AppPreferences.getInput2(this))
            editText3.setText(AppPreferences.getInput3(this))
            Log.d(TAG, "저장된 설정 불러오기 완료")
        } catch (e: Exception) {
            Log.e(TAG, "저장된 설정 불러오기 실패", e)
        }

        try {
            // 배터리 모드 설정 초기화
            initializeBatteryMode()
            Log.d(TAG, "배터리 모드 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "배터리 모드 초기화 실패", e)
        }

        try {
            // 위치 추적 제어 초기화
            initializeTrackingControls()
            Log.d(TAG, "위치 추적 제어 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "위치 추적 제어 초기화 실패", e)
        }

        try {
            // 현재 설정 표시
            updateCurrentSettings()
            Log.d(TAG, "현재 설정 표시 완료")
        } catch (e: Exception) {
            Log.e(TAG, "현재 설정 표시 실패", e)
        }

        try {
            // 저장 버튼 클릭 리스너
            buttonSave.setOnClickListener { view ->
                Log.d(TAG, "저장 버튼 클릭됨")
                animateButtonClick(view)
                saveSettings()
            }

            // 키 초기화 버튼 클릭 리스너
            val buttonResetKey = findViewById<Button>(R.id.buttonResetKey)
            buttonResetKey.setOnClickListener { view ->
                Log.d(TAG, "키 초기화 버튼 클릭됨")
                animateButtonClick(view)
                val newKey = createSecureKey(32)
                editText3.setText(newKey)
                Toast.makeText(this, "새 키가 생성되었습니다", Toast.LENGTH_SHORT).show()
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

    private fun createIV(length: Int): String {
        Log.d(TAG, "IV 생성 시작: 길이=$length")
        val letters = "abcdefghijklmnopqrstuvwxyz0123456789"
        val result = (1..length)
            .map { letters.random() }
            .joinToString("")
        Log.d(TAG, "IV 생성 완료: $result")
        return result
    }

    // 영문 대소문자 + 숫자 + 특수문자 포함 키 생성 함수
    private fun createSecureKey(length: Int): String {
        // 영문 대소문자 + 숫자 + 특수문자만 사용
        val allowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
        val result = (1..length)
            .map { allowed.random() }
            .joinToString("")
        Log.d(TAG, "createSecureKey 생성 완료: $result")
        return result
    }

    private fun saveSettings() {
        Log.d(TAG, "saveSettings 시작")
        
        try {
            val input1 = editText1.text.toString()
            val input2 = editText2.text.toString()
            var input3 = editText3.text.toString()

            Log.d(TAG, "입력값: input1=$input1, input2=$input2, input3=$input3")

            // 입력 검증
            if (input1.isEmpty() || input2.isEmpty()) {
                Log.d(TAG, "입력 검증 실패: 필수 입력값 누락")
                Toast.makeText(this, "디바이스 ID와 키를 모두 입력해주세요", Toast.LENGTH_LONG).show()
                return
            }

            if (input3.isEmpty()) {
                Log.d(TAG, "input3이 비어있어서 자동 생성")
                input3 = createSecureKey(32)
                editText3.setText(input3)
                Toast.makeText(this, getString(R.string.auto_generated_key, input3), Toast.LENGTH_LONG).show()
            }

            // 배터리 모드 저장
            val selectedMode = when (batteryModeGroup.checkedRadioButtonId) {
                R.id.radioNormal -> "normal"
                R.id.radioPower -> "power"
                R.id.radioUltra -> "ultra"
                else -> "normal"
            }
            Log.d(TAG, "선택된 배터리 모드: $selectedMode")
            
            AppPreferences.saveBatteryMode(this, selectedMode)
            AppPreferences.saveInputs(this, input1, input2, input3)
            Log.d(TAG, "설정 저장 완료")
            
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            updateCurrentSettings()

            // 위치 추적 제어 처리
            val trackingAction = when (trackingControlGroup.checkedRadioButtonId) {
                R.id.radioTrackingNormal -> "resume"
                R.id.radioPause5 -> "pause5"
                R.id.radioPause10 -> "pause10"
                R.id.radioPause30 -> "pause30"
                R.id.radioStop -> "stop"
                else -> "unknown"
            }
            Log.d(TAG, "위치 추적 제어 액션: $trackingAction")

            when (trackingControlGroup.checkedRadioButtonId) {
                R.id.radioTrackingNormal -> {
                    Log.d(TAG, "추적 재개 처리")
                    AppPreferences.resumeTracking(this)
                    // 서비스 시작은 checkPermissions에서 처리
                }
                R.id.radioPause5 -> {
                    Log.d(TAG, "5분 일시정지 처리")
                    AppPreferences.pauseTracking(this, 5)
                    Toast.makeText(this, getString(R.string.tracking_paused), Toast.LENGTH_SHORT).show()
                }
                R.id.radioPause10 -> {
                    Log.d(TAG, "10분 일시정지 처리")
                    AppPreferences.pauseTracking(this, 10)
                    Toast.makeText(this, getString(R.string.tracking_paused), Toast.LENGTH_SHORT).show()
                }
                R.id.radioPause30 -> {
                    Log.d(TAG, "30분 일시정지 처리")
                    AppPreferences.pauseTracking(this, 30)
                    Toast.makeText(this, getString(R.string.tracking_paused), Toast.LENGTH_SHORT).show()
                }
                R.id.radioStop -> {
                    Log.d(TAG, "완전 중지 처리")
                    AppPreferences.stopTracking(this)
                    stopLocationService()
                    Toast.makeText(this, getString(R.string.tracking_stopped), Toast.LENGTH_SHORT).show()
                }
            }

            // 권한 체크 및 서비스 시작 (완전 중지가 아닌 경우에만)
            if (trackingControlGroup.checkedRadioButtonId != R.id.radioStop) {
                Log.d(TAG, "권한 체크 시작")
                checkPermissions()
            }

            Log.d(TAG, "saveSettings 완료")
            
        } catch (e: Exception) {
            Log.e(TAG, "saveSettings에서 예외 발생", e)
            Toast.makeText(this, "설정 저장 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeBatteryMode() {
        Log.d(TAG, "initializeBatteryMode 시작")
        val currentMode = AppPreferences.getBatteryMode(this)
        val radioButtonId = when (currentMode) {
            "normal" -> R.id.radioNormal
            "power" -> R.id.radioPower
            "ultra" -> R.id.radioUltra
            else -> R.id.radioNormal
        }
        batteryModeGroup.check(radioButtonId)
        Log.d(TAG, "배터리 모드 초기화: $currentMode -> $radioButtonId")
    }

    private fun initializeTrackingControls() {
        Log.d(TAG, "initializeTrackingControls 시작")
        // 현재 추적 상태에 따라 기본값 설정
        if (AppPreferences.isTrackingPaused(this)) {
            trackingControlGroup.check(R.id.radioStop)
            Log.d(TAG, "추적 일시정지 상태로 설정")
        } else {
            trackingControlGroup.check(R.id.radioTrackingNormal)
            Log.d(TAG, "추적 활성화 상태로 설정")
        }
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

    private fun stopLocationService() {
        Log.d(TAG, "stopLocationService 시작")
        try {
            val serviceIntent = Intent(this, LocationService::class.java)
            stopService(serviceIntent)
            Log.d(TAG, "위치 서비스 중지 완료")
            Toast.makeText(this, "위치 추적이 중지되었습니다", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "위치 서비스 중지 실패", e)
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