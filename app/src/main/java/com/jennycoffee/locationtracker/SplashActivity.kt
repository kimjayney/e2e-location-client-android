package com.jennycoffee.locationtracker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var splashIcon: ImageView
    private lateinit var splashTitle: TextView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        splashIcon = findViewById(R.id.splashIcon)
        splashTitle = findViewById(R.id.splashTitle)
        statusText = findViewById(R.id.statusText)

        // 초기 상태 설정
        splashIcon.alpha = 0f
        splashIcon.scaleX = 0.5f
        splashIcon.scaleY = 0.5f
        splashTitle.alpha = 0f
        splashTitle.translationY = 50f
        statusText.alpha = 0f

        // 애니메이션 시작
        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        // 아이콘 애니메이션
        val iconFadeIn = ObjectAnimator.ofFloat(splashIcon, View.ALPHA, 0f, 1f)
        val iconScaleX = ObjectAnimator.ofFloat(splashIcon, View.SCALE_X, 0.5f, 1f)
        val iconScaleY = ObjectAnimator.ofFloat(splashIcon, View.SCALE_Y, 0.5f, 1f)
        val iconRotation = ObjectAnimator.ofFloat(splashIcon, View.ROTATION, -30f, 0f)

        val iconAnimator = AnimatorSet()
        iconAnimator.playTogether(iconFadeIn, iconScaleX, iconScaleY, iconRotation)
        iconAnimator.duration = 1000
        iconAnimator.interpolator = AccelerateDecelerateInterpolator()

        // 제목 애니메이션
        val titleFadeIn = ObjectAnimator.ofFloat(splashTitle, View.ALPHA, 0f, 1f)
        val titleSlideUp = ObjectAnimator.ofFloat(splashTitle, View.TRANSLATION_Y, 50f, 0f)

        val titleAnimator = AnimatorSet()
        titleAnimator.playTogether(titleFadeIn, titleSlideUp)
        titleAnimator.duration = 800
        titleAnimator.interpolator = AccelerateDecelerateInterpolator()

        // 상태 텍스트 애니메이션
        val statusFadeIn = ObjectAnimator.ofFloat(statusText, View.ALPHA, 0f, 1f)
        statusFadeIn.duration = 500

        // 순차 실행
        val fullAnimator = AnimatorSet()
        fullAnimator.play(iconAnimator).before(titleAnimator)
        fullAnimator.play(titleAnimator).before(statusFadeIn)
        fullAnimator.start()

        // 단계별 진행 상태 업데이트
        Handler(Looper.getMainLooper()).postDelayed({
            statusText.text = "루팅 확인 중..."
        }, 500)

        Handler(Looper.getMainLooper()).postDelayed({
            // 루팅 감지
            if (RootDetector.isDeviceRooted(this)) {
                statusText.text = "보안 경고: 루팅된 기기입니다"
                // 루팅된 기기인 경우 보안 경고 화면으로 이동
                val intent = Intent(this, RootDetectedActivity::class.java)
                startActivity(intent)
                finish()
                return@postDelayed
            }
            statusText.text = "보안 확인 완료"
        }, 1500)

        Handler(Looper.getMainLooper()).postDelayed({
            statusText.text = "서비스 초기화 중..."
        }, 2000)

        Handler(Looper.getMainLooper()).postDelayed({
            statusText.text = "메인 화면으로 이동 중..."
        }, 2500)

        // 3초 후 메인 액티비티로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            // 화면 전환 애니메이션
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 3000)
    }
} 