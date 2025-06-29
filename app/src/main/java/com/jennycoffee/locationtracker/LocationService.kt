package com.jennycoffee.locationtracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import kotlin.random.Random

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.jennycoffee.locationtracker.BuildConfig

class LocationService : Service() {
    var locationSessionPrivatekey = ""
    var locationSessionDeviceId= ""
    var locationSessionDeviceAuthorization = ""
    companion object {
        private const val TAG = "LocationService"
        // 기본값 (사용자 설정에 따라 동적으로 변경됨)
        private const val DEFAULT_LOCATION_INTERVAL = 60000L  // 1분
        private const val DEFAULT_LOCATION_DISTANCE = 30f     // 30미터
        private const val CHANNEL_ID = "location_notification_channel"
        
        // 추가 절약 설정
        private const val MIN_UPDATE_INTERVAL = 30000L   // 최소 30초 간격
        private const val MAX_UPDATE_INTERVAL = 300000L  // 최대 5분 간격
        private const val SPEED_THRESHOLD = 5f           // 5m/s 이상일 때만 자주 업데이트
    }

    private var mLocationManager: LocationManager? = null
    private var lastUpdateTime = 0L
    private var lastLocation: Location? = null

    // IV 생성 함수
    fun createIV(length: Int): String {
        val letters = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { letters.random() }
            .joinToString("")
    }

    // AES 암호화 함수
    fun aesEncrypt(key: String, iv: String, data: String): String? {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
            val ivParameterSpec = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
            val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedData, Base64.NO_WRAP) // NO_WRAP 옵션 사용
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // AES 복호화 함수
    fun aesDecrypt(key: String, iv: String, data: String?): String? {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
            val ivParameterSpec = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))

            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
            val decodedData = Base64.decode(data, Base64.DEFAULT)
            String(cipher.doFinal(decodedData), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sendGetRequest(url: String, location: Location? = null) {
        Thread {
            try {
                val urlObj = URL(url)
                val connection = urlObj.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000 // 10초 타임아웃
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                var responseMessage = ""
                
                try {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        responseMessage = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d(TAG, "서버 응답: $responseMessage")
                    } else {
                        responseMessage = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "에러 응답 없음"
                        Log.e(TAG, "서버 에러 응답: $responseMessage")
                    }
                } catch (e: Exception) {
                    responseMessage = "응답 읽기 실패: ${e.message}"
                    Log.e(TAG, "응답 읽기 실패", e)
                }

                // 로그 저장
                if (location != null) {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // 성공 응답 처리
                        val successMessage = when {
                            responseMessage.contains("success") -> "성공"
                            responseMessage.contains("ok") -> "성공"
                            responseMessage.isNotEmpty() -> "응답: ${responseMessage.take(50)}${if (responseMessage.length > 50) "..." else ""}"
                            else -> "성공 (응답 없음)"
                        }
                        Log.d(TAG, "로그 저장: 성공 - $successMessage")
                    } else {
                        // 실패 응답 처리
                        val errorMessage = "HTTP $responseCode: ${responseMessage.take(50)}${if (responseMessage.length > 50) "..." else ""}"
                        Log.d(TAG, "로그 저장: 실패 - $errorMessage")
                    }
                } else {
                    Log.d(TAG, "위치 정보가 없어서 로그를 저장하지 않습니다")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "네트워크 요청 실패: ${e.message}")
                // 예외 로그 저장 제거
            }
        }.start()
    }
    fun encodeUrlWithUrlParam(baseUrl: String, paramUrl: String): String {
        val encoded = URLEncoder.encode(paramUrl, StandardCharsets.UTF_8.toString())
        return "$baseUrl?target=$encoded"
    }

    fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    // 동적 설정값 가져오기
    private fun getLocationInterval(): Long {
        return AppPreferences.getLocationInterval(this)
    }
    
    private fun getLocationDistance(): Float {
        return AppPreferences.getLocationDistance(this)
    }

    private inner class MyLocationListener(provider: String) : LocationListener {
        private var mLastLocation: Location = Location(provider)

        init {
            Log.e(TAG, "LocationListener $provider")
        }

        override fun onLocationChanged(location: Location) {
            // 일시정지 상태 확인
            if (AppPreferences.isTrackingPaused(this@LocationService)) {
                Log.d(TAG, "위치 추적이 일시정지되어 업데이트를 건너뜁니다")
                return
            }
            
            val currentTime = System.currentTimeMillis()
            
            // 스마트 업데이트 로직
            if (!shouldUpdateLocation(location, currentTime)) {
                Log.d(TAG, "위치 업데이트 스킵됨 - 조건 불충족")
                return
            }
            
            val testkey = locationSessionPrivatekey
            val iv = createIV(16)

            val encrypted_lat = aesEncrypt(testkey, iv, location.latitude.toString())
            val encrypted_lng = aesEncrypt(testkey, iv, location.longitude.toString())
            Log.d(TAG, "Key: $testkey")
            Log.d(TAG, "encrypted_lat: $encrypted_lat")
            Log.d(TAG, "encrypted_lng: $encrypted_lng")
            val decrypt_lat = aesDecrypt(testkey, iv, encrypted_lat)
            val decrypt_lng = aesDecrypt(testkey, iv, encrypted_lng)
            Log.d(TAG, "decrypted_lng: $decrypt_lat")
            Log.d(TAG, "decrypt_lng: $decrypt_lng")

            val baseUrl = BuildConfig.SERVER_URL + "/api/update"

            val parameters = "?lat=${encrypted_lat?.let { encode(it) }}" +
                    "&lng=${encrypted_lng?.let { encode(it) }}" +
                    "&iv=${encode(iv)}" +
                    "&device=${encode(locationSessionDeviceId)}" +
                    "&authorization=${encode(locationSessionDeviceAuthorization)}"

            val finalUrl = baseUrl + parameters

            Log.d("urlStr", finalUrl)
            sendGetRequest(finalUrl, location)

            // 업데이트 정보 저장
            lastUpdateTime = currentTime
            lastLocation = location
            
            // 마지막 위치 정보를 AppPreferences에 저장
            AppPreferences.saveLastLocation(this@LocationService, location.latitude, location.longitude)
            
            // MainActivity로 위치 업데이트 브로드캐스트 전송
            val intent = Intent("LOCATION_UPDATE")
            intent.putExtra("LATITUDE", location.latitude.toString())
            intent.putExtra("LONGITUDE", location.longitude.toString())
            sendBroadcast(intent)
            
            Log.d(TAG, "위치 업데이트 전송됨: 위도=${location.latitude}, 경도=${location.longitude}")
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "onProviderDisabled: $provider")
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "onProviderEnabled: $provider")
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Log.e(TAG, "onStatusChanged: $provider")
        }

        // 스마트 업데이트 조건 체크
        private fun shouldUpdateLocation(newLocation: Location, currentTime: Long): Boolean {
            // 첫 번째 위치 업데이트는 항상 전송
            if (lastLocation == null) {
                Log.d(TAG, "첫 번째 위치 업데이트")
                return true
            }
            
            // 최소 업데이트 간격 체크
            val timeSinceLastUpdate = currentTime - lastUpdateTime
            if (timeSinceLastUpdate < MIN_UPDATE_INTERVAL) {
                Log.d(TAG, "최소 업데이트 간격 미충족: ${timeSinceLastUpdate}ms")
                return false
            }
            
            // 거리 체크 (사용자 설정에 따른 최소 이동 거리)
            val distance = newLocation.distanceTo(lastLocation!!)
            if (distance < getLocationDistance()) {
                Log.d(TAG, "거리 조건 미충족: ${distance}m < ${getLocationDistance()}m")
                return false
            }
            
            // 속도 기반 동적 간격 조정
            val speed = newLocation.speed
            val dynamicInterval = when {
                speed > SPEED_THRESHOLD -> MIN_UPDATE_INTERVAL  // 빠른 이동 시 1분
                speed > 2f -> MIN_UPDATE_INTERVAL * 2           // 보통 이동 시 2분
                else -> MAX_UPDATE_INTERVAL                     // 정지 시 5분
            }
            
            if (timeSinceLastUpdate < dynamicInterval) {
                Log.d(TAG, "동적 간격 미충족: ${timeSinceLastUpdate}ms < ${dynamicInterval}ms (속도: ${speed}m/s)")
                return false
            }
            
            Log.d(TAG, "위치 업데이트 조건 충족: 거리=${distance}m, 속도=${speed}m/s, 간격=${timeSinceLastUpdate}ms")
            return true
        }
    }

    private val mLocationListeners = arrayOf(
        MyLocationListener(LocationManager.GPS_PROVIDER),
        MyLocationListener(LocationManager.NETWORK_PROVIDER)
    )

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand")
        val input1 = AppPreferences.getInput1(this)
        val input2 = AppPreferences.getInput2(this)
        val input3 = AppPreferences.getInput3(this)
        Log.d("LocationService", "불러온 값: input1=$input1, input2=$input2, input3=$input3")
        locationSessionDeviceId = input1
        locationSessionDeviceAuthorization = input2
        locationSessionPrivatekey = input3
        
        // 디바이스 등록 요청 (위치 정보 없이)
        sendGetRequest(BuildConfig.SERVER_URL + "/api/device/register?device=$input1&authorization=$input2")
        
        startForegroundService()
        return START_STICKY
    }

    override fun onCreate() {
        Log.e(TAG, "onCreate")
        initializeLocationManager()

        try {
            mLocationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, getLocationInterval(), getLocationDistance(),
                mLocationListeners[1]
            )
        } catch (ex: SecurityException) {
            Log.i(TAG, "fail to request location update, ignore", ex)
        } catch (ex: IllegalArgumentException) {
            Log.d(TAG, "network provider does not exist, ${ex.message}")
        }

        try {
            mLocationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, getLocationInterval(), getLocationDistance(),
                mLocationListeners[0]
            )
        } catch (ex: SecurityException) {
            Log.i(TAG, "fail to request location update, ignore", ex)
        } catch (ex: IllegalArgumentException) {
            Log.d(TAG, "gps provider does not exist ${ex.message}")
        }
    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy")
        super.onDestroy()
        if (mLocationManager != null) {
            for (listener in mLocationListeners) {
                try {
                    mLocationManager?.removeUpdates(listener)
                } catch (ex: Exception) {
                    Log.i(TAG, "fail to remove location listeners, ignore", ex)
                }
            }
        }
    }

    private fun initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager")
        if (mLocationManager == null) {
            mLocationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Tracking location in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        // Foreground 서비스 시작
        startForeground(1, notification)
    }
}
