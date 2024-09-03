package com.jennycoffee.locationtracker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    private lateinit var textViewLocation: TextView
    private lateinit var locationListView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val locationList = ArrayList<String>()

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
//            val latitude = intent?.getStringExtra("LATITUDE") ?: "N/A"
//            val longitude = intent?.getStringExtra("LONGITUDE") ?: "N/A"
//            val locationString = "Lat: $latitude, Lon: $longitude"
//
//            textViewLocation.text = locationString
//
//            // ListView에 위치 로그 추가
//            locationList.add(locationString)
//            adapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewLocation = findViewById(R.id.textViewLocation)
        locationListView = findViewById(R.id.locationListView)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, locationList)
        locationListView.adapter = adapter

        // 권한 체크 및 요청
        checkPermissions()

        // 브로드캐스트 리시버 등록
        val filter = IntentFilter("LOCATION_UPDATE")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(
                this,
                locationReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(locationReceiver, filter)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // 권한이 이미 부여된 경우 서비스 시작
            startLocationService()
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService()
            } else {
                // 권한이 거부된 경우 사용자에게 알림
                Log.d("MainActivity", "Location permission denied")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(locationReceiver)
    }
}
