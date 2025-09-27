package com.jennycoffee.locationtracker

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "SettingsViewModel"

    // UI 상태를 나타내는 LiveData
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    // 알림 설정 스위치 상태
    val isNotificationsEnabled = AppPreferences.getNotificationsEnabled(application)

    fun onNotificationSwitchClicked(isChecked: Boolean) {
        viewModelScope.launch {
            // 1. 로딩 상태로 변경 (UI에서 스위치를 비활성화)
            _uiState.value = UiState.Loading

            try {
                val token = if (isChecked) {
                    // 스위치를 켤 때: 저장된 토큰을 사용하거나 새로 가져옴
                    val storedToken = AppPreferences.getFcmToken(getApplication())
                    if (storedToken.isNotEmpty()) storedToken else FirebaseMessaging.getInstance().token.await()
                } else {
                    null // 스위치를 끌 때는 토큰 필요 없음
                }

                if (isChecked && token.isNullOrEmpty()) {
                    throw IllegalStateException("FCM 토큰이 유효하지 않습니다.")
                }

                // 2. 서버 API 호출
                val result = setAllowNotificationOnServer(isChecked, token)

                if (result.isSuccess) {
                    // 3. 성공 시, 상태 저장 및 성공 상태 전파
                    AppPreferences.saveNotificationsEnabled(getApplication(), isChecked)
                    _uiState.value = UiState.Success(if (isChecked) "알림이 허용되었습니다." else "알림이 차단되었습니다.")
                } else {
                    throw Exception(result.errorMessage ?: "서버에서 알림 설정을 실패했습니다.")
                }
            } catch (e: Exception) {
                // 4. 실패 시, 실패 상태 전파
                Log.e(TAG, "알림 설정 실패", e)
                _uiState.value = UiState.Error("알림 설정 실패: ${e.message}")
            }
        }
    }

    private suspend fun setAllowNotificationOnServer(isAllowed: Boolean, fcmToken: String?): NetworkResult {
        val context = getApplication<Application>().applicationContext
        val deviceId = AppPreferences.getInput1(context)
        val deviceKey = AppPreferences.getInput2(context)
        val setAllow = if (isAllowed) "1" else "0"

        return withContext(Dispatchers.IO) {
            try {
                var urlString = "${BuildConfig.SERVER_URL}/api/device/set-allow-notification?deviceId=$deviceId&authorization=$deviceKey&setAllowNotification=$setAllow"
                if (isAllowed && !fcmToken.isNullOrEmpty()) {
                    val encodedToken = URLEncoder.encode(fcmToken, "UTF-8")
                    urlString += "&notiToken=$encodedToken"
                }

                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                val isSuccess = (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED)

                if (isSuccess) {
                    connection.disconnect()
                    return@withContext NetworkResult(true)
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "Set notification status failed. Response: $responseCode, Body: $errorBody")
                    connection.disconnect()
                    return@withContext NetworkResult(false, errorBody)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting notification status", e)
                return@withContext NetworkResult(false, e.message)
            }
        }
    }
}

// ViewModel이 UI에 전달할 상태 정의
sealed class UiState {
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}

// 네트워크 결과와 오류 메시지를 함께 담는 데이터 클래스
data class NetworkResult(val isSuccess: Boolean, val errorMessage: String? = null)