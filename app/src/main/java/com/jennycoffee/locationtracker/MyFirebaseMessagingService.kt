package com.jennycoffee.locationtracker

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    /**
     * FCM 등록 토큰이 갱신될 때 호출됩니다.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // 1. 갱신된 토큰을 SharedPreferences에 저장합니다.
        AppPreferences.saveFcmToken(this, token)

        // 2. 만약 사용자가 이미 알림을 허용한 상태라면, 서버에도 갱신된 토큰을 보내야 합니다.
        if (AppPreferences.getNotificationsEnabled(this)) {
            // TODO: 서버에 갱신된 토큰을 전송하는 API 호출 로직 추가 (예: setAllowNotificationOnServer)
            Log.d(TAG, "Notification is enabled. Should update the new token to the server.")
        }
    }

    /**
     * 앱이 포그라운드에 있을 때 메시지를 수신하면 호출됩니다.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // TODO: 수신된 푸시 메시지를 처리하는 로직 (예: 알림 표시)
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }
}