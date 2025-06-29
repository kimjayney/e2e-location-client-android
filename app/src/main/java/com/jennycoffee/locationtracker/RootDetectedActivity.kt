package com.jennycoffee.locationtracker

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class RootDetectedActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root_detected)

        val buttonExit = findViewById<Button>(R.id.buttonExit)
        buttonExit.setOnClickListener {
            // 앱 종료
            finish()
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        }

        // 3초 후 자동 종료
        buttonExit.postDelayed({
            if (!isFinishing) {
                finish()
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(0)
            }
        }, 3000)
    }

    override fun onBackPressed() {
        // 뒤로가기 버튼 비활성화
        Toast.makeText(this, "앱을 종료하려면 버튼을 누르세요", Toast.LENGTH_SHORT).show()
    }
} 