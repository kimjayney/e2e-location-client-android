package com.jennycoffee.locationtracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.io.BufferedReader
import java.net.URLDecoder

class PushManagementActivity : AppCompatActivity() {

    private val TAG = "PushManagementActivity"

    private lateinit var targetDeviceList: ListView
    private lateinit var myQrCodeImageView: ImageView
    private lateinit var addTargetDeviceButton: Button

    private val deviceList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_push_management)

        myQrCodeImageView = findViewById(R.id.myQrCodeImageView)
        targetDeviceList = findViewById(R.id.targetDeviceList)
        addTargetDeviceButton = findViewById(R.id.addTargetDeviceButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        targetDeviceList.adapter = adapter

        addTargetDeviceButton.setOnClickListener {
            // QR 코드 스캐너 실행
            IntentIntegrator(this).apply {
                setPrompt("알림을 받을 대상 기기의 QR 코드를 스캔하세요.")
                initiateScan()
            }
        }

        // 내 QR 코드 생성 및 표시, 등록된 기기 목록 가져오기
        generateAndShowMyQrCode()
        fetchTargetDevices()
    }

    private fun fetchTargetDevices() {
        val deviceId = AppPreferences.getInput1(this)
        val authorization = AppPreferences.getInput2(this)

        if (deviceId.isEmpty() || authorization.isEmpty()) {
            Toast.makeText(this, "현재 기기 정보가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                val url = "${BuildConfig.SERVER_URL}/api/device/notification-targets?deviceId=$deviceId&authorization=$authorization"
                (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val responseBody = this.inputStream.bufferedReader().use(BufferedReader::readText)
                        val jsonResponse = JSONObject(responseBody) // 1. 응답을 JSONObject로 파싱
                        val fetchedList = mutableListOf<String>()

                        if (jsonResponse.optBoolean("success")) {
                            // 2. JSONObject에서 "targets" 키로 JSONArray를 가져옴
                            val targetsArray = jsonResponse.getJSONArray("targets") 
                            for (i in 0 until targetsArray.length()) {
                                val item = targetsArray.getJSONObject(i)
                                // 3. 각 객체에서 "toDeviceId" 값을 추출
                                val targetId = item.getString("toDeviceId")
                                fetchedList.add(targetId)
                            }
                        }

                        runOnUiThread {
                            deviceList.clear()
                            deviceList.addAll(fetchedList)
                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        val errorBody = this.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                        Log.e(TAG, "Failed to fetch targets: Response code $responseCode")
                        Log.e(TAG, "Error Body: $errorBody")
                        runOnUiThread {
                            Toast.makeText(this@PushManagementActivity, "목록을 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.disconnect()
            } catch (e: Exception) {
                Log.e("PushManagement", "Error fetching target devices", e)
            }
        }.start()
    }

    private fun addTargetDevice(toDeviceId: String, toNotificationControlKey: String, onResult: (Boolean, String?) -> Unit) {
        val deviceId = AppPreferences.getInput1(this)
        val authorization = AppPreferences.getInput2(this)

        if (deviceId.isEmpty() || authorization.isEmpty()) {
            Toast.makeText(this, "현재 기기 정보가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            var success = false
            var errorMessage: String? = null
            try {
                val encodedToId = URLEncoder.encode(toDeviceId, StandardCharsets.UTF_8.toString())
                val encodedToKey = URLEncoder.encode(toNotificationControlKey, StandardCharsets.UTF_8.toString())

                val url = "${BuildConfig.SERVER_URL}/api/device/register-notification-target" + // API 엔드포인트 수정
                        "?deviceId=$deviceId" +
                        "&authorization=$authorization" +
                        "&toDeviceId=$encodedToId" +
                        "&toNotificationControlKey=$encodedToKey"

                (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                        success = true
                    } else {
                        // 실패 시, 서버가 보낸 자세한 오류 메시지를 읽습니다.
                        errorMessage = errorStream?.bufferedReader()?.use { it.readText() } ?: "서버로부터 상세 오류 메시지를 받지 못했습니다. (코드: $responseCode)"
                        Log.e(TAG, "Add target device failed: Response code $responseCode, Message: $errorMessage")
                    }
                }.disconnect()

            } catch (e: Exception) {
                Log.e("PushManagement", "Failed to add target device", e)
                errorMessage = e.message
            }

            onResult(success, errorMessage)
        }.start()
    }

    private fun generateAndShowMyQrCode() {
        val deviceId = AppPreferences.getInput1(this)
        val authorization = AppPreferences.getInput2(this)

        if (deviceId.isEmpty() || authorization.isEmpty()) return

        Thread {
            try {
                // 1. 서버에서 notificationControlKey 가져오기
                val url = "${BuildConfig.SERVER_URL}/api/device/notification-token?deviceId=$deviceId&authorization=$authorization"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val jsonResponse = JSONObject(responseBody)
                    val notificationControlKey = jsonResponse.optString("notificationControlKey")

                    if (notificationControlKey.isNotEmpty()) {
                        // 2. QR 코드 내용 생성 (URL 쿼리 파라미터 형식)
                        val qrContent = "toDeviceId=$deviceId&toNotificationControlKey=$notificationControlKey"

                        // 3. QR 코드 이미지 생성
                        val barcodeEncoder = BarcodeEncoder()
                        val bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 400, 400)

                        // 4. UI 스레드에서 이미지 표시
                        runOnUiThread {
                            myQrCodeImageView.setImageBitmap(bitmap)
                        }
                    }
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                    Log.e(TAG, "Failed to get notification token. Response: ${connection.responseCode}")
                    Log.e(TAG, "Error Body: $errorBody")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error generating QR code", e)
            }
        }.start()
    }

    // QR 코드 스캔 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "스캔이 취소되었습니다.", Toast.LENGTH_LONG).show()
            } else {
                // 스캔 결과 파싱 및 처리
                handleScannedData(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleScannedData(scannedData: String) {
        try {
            // URL 쿼리 파라미터 형식으로 파싱
            val params = scannedData.split("&").map {
                val parts = it.split("=")
                parts[0] to URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8.name())
            }.toMap()

            val toDeviceId = params["toDeviceId"]
            val toNotificationControlKey = params["toNotificationControlKey"]

            if (toDeviceId.isNullOrEmpty() || toNotificationControlKey.isNullOrEmpty()) {
                Toast.makeText(this, "유효하지 않은 QR 코드입니다.", Toast.LENGTH_LONG).show()
                return
            }

            // 사용자에게 확인 다이얼로그 표시
            AlertDialog.Builder(this)
                .setTitle("대상 기기 추가 확인")
                .setMessage("다음 기기를 알림 대상으로 추가하시겠습니까?\n\nID: $toDeviceId\nKey: $toNotificationControlKey")
                .setPositiveButton("추가") { _, _ ->
                    addTargetDevice(toDeviceId, toNotificationControlKey) { success, errorMessage ->
                        runOnUiThread {
                            if (success) {
                                Toast.makeText(this, "디바이스가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                                fetchTargetDevices() // 목록 새로고침
                            } else {
                                Toast.makeText(this, "디바이스 추가 실패: ${errorMessage ?: "알 수 없는 오류"}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                .setNegativeButton("취소", null)
                .show()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse scanned data", e)
            Toast.makeText(this, "QR 코드 분석에 실패했습니다.", Toast.LENGTH_LONG).show()
        }
    }
}