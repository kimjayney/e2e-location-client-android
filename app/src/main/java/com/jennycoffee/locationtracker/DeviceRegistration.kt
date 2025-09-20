package com.jennycoffee.locationtracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.json.JSONObject

object DeviceRegistration {

    private const val TAG = "DeviceRegistration"
    fun registerNewDevice(context: Context, deviceId: String, deviceKey: String, shareControlKey: String) {
        Thread {
            val handler = Handler(Looper.getMainLooper())
            try {
                val url ="https://jayneycoffee.api.location.rainclab.net/api/device/register?device=$deviceId&authorization=$deviceKey&shareControlKey=$shareControlKey"
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

                handler.post {
                    Log.d(TAG, "ResponseCode: $responseCode, Body: $responseBody")

                    if (responseCode == 200 && responseBody != null) {
                        Toast.makeText(context, "디바이스 등록 성공",Toast.LENGTH_SHORT).show()
                        Log.d("jenny",url)
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val status = jsonResponse.optBoolean("status", false)
                            val message = if (java.util.Locale.getDefault().language == "ko") {
                                jsonResponse.optString("message_ko_KR", "알 수 없는 응답")
                            } else {
                                jsonResponse.optString("message_en_US", "Unknown response")
                            }

                            if (status) { // 새로운 기기 등록 성공
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                // 웹 브라우저 열기
                                val recapthaUrl = "${BuildConfig.WEB_URL}/recaptha?deviceId=$deviceId&authorization=$deviceKey&shareControlKey=$shareControlKey"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recapthaUrl))
                                context.startActivity(intent)
                            } else { // 이미 등록된 기기
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: org.json.JSONException) {
                            Log.e(TAG, "JSON 파싱 오류. 응답이 JSON이 아닐 수 있음: $responseBody", e)
                            // JSON 파싱에 실패했지만, 성공으로 간주하고 reCAPTCHA 진행
                            val recapthaUrl = "${BuildConfig.WEB_URL}/recaptha?deviceId=$deviceId&authorization=$deviceKey&shareControlKey=$shareControlKey"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recapthaUrl))
                            context.startActivity(intent)
                        }
                    } else {
                        Toast.makeText(context, "디바이스 등록 실패: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                handler.post {
                    Log.e(TAG, "디바이스 등록 중 오류 발생", e)
                    Toast.makeText(context, "디바이스 등록 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}