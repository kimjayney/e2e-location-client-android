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

class LocationService : Service() {

    companion object {
        private const val TAG = "LocationService"
        private const val LOCATION_INTERVAL = 1000L
        private const val LOCATION_DISTANCE = 10f
        private const val CHANNEL_ID = "location_notification_channel"
    }

    private var mLocationManager: LocationManager? = null
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
            Base64.encodeToString(encryptedData, Base64.DEFAULT)
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

    fun sendGetRequest(url: String) {
        Thread {
            try {
                val urlObj = URL(url)
                val connection = urlObj.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    println("Response: $response")
                } else {
                    println("GET request failed. Response Code: $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private inner class MyLocationListener(provider: String) : LocationListener {
        private var mLastLocation: Location = Location(provider)

        init {
            Log.e(TAG, "LocationListener $provider")
        }

        override fun onLocationChanged(location: Location) {
            val testkey = createIV(32)
//            val testkey = "ht6hsbtiyvmy4dw4exv0shtgg6nuqy78"
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
            val urlStr = "https://jayneycoffee.api.location.rainclab.net/api/update?lat=$encrypted_lat&lng=$encrypted_lng&iv=$iv&device=your_device_id&authorization=your_device_key"
            Log.d("urlStr", urlStr)
            sendGetRequest(urlStr)

            mLastLocation.set(location)
            // 위치 변경 시 추가 작업 가능
//            val intent = Intent("LOCATION_UPDATE")
//            intent.putExtra("LATITUDE", location.latitude.toString())
//            intent.putExtra("LONGITUDE", location.longitude.toString())
//            sendBroadcast(intent)
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
        sendGetRequest("https://jayneycoffee.api.location.rainclab.net/api/device/register?device=your_device_id&authorization=your_device_key")
        startForegroundService()
        return START_STICKY
    }

    override fun onCreate() {
        Log.e(TAG, "onCreate")
        initializeLocationManager()

        try {
            mLocationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                mLocationListeners[1]
            )
        } catch (ex: SecurityException) {
            Log.i(TAG, "fail to request location update, ignore", ex)
        } catch (ex: IllegalArgumentException) {
            Log.d(TAG, "network provider does not exist, ${ex.message}")
        }

        try {
            mLocationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
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
