package com.jennycoffee.locationtracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
class MainActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    private lateinit var textViewLocation: TextView
    private lateinit var locationListView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val locationList = ArrayList<String>()

    private lateinit var editText1: EditText
    private lateinit var editText2: EditText
    private lateinit var editText3: EditText
    private lateinit var buttonSave: Button

    fun createIV(length: Int): String {
        val letters = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { letters.random() }
            .joinToString("")
    }
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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewLocation = findViewById(R.id.textViewLocation)
//        locationListView = findViewById(R.id.locationListView)
        editText1 = findViewById(R.id.editText1)
        editText2 = findViewById(R.id.editText2)
        editText3 = findViewById(R.id.editText3)
        buttonSave = findViewById(R.id.buttonSave)

        editText1.setText(AppPreferences.getInput1(this))
        editText2.setText(AppPreferences.getInput2(this))
        editText3.setText(AppPreferences.getInput3(this))
        val button = findViewById<Button>(R.id.LinkOpen)


        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, locationList)
//        locationListView.adapter = adapter
        buttonSave.setOnClickListener {
            val input1 = editText1.text.toString()
            val input2 = editText2.text.toString()
            var input3 = editText3.text.toString()
            if (input3 == "") {
                input3 = createIV(32)
            }
            AppPreferences.saveInputs(this, input1, input2, input3)
            Toast.makeText(this, input3, Toast.LENGTH_SHORT).show()
        }


        button.setOnClickListener {
            val id = AppPreferences.getInput1(this)  // 또는 getInput2(this), getInput3(this) 등
            val deviceKey = AppPreferences.getInput2(this)  // 또는 getInput2(this), getInput3(this) 등
            val privateKey = AppPreferences.getInput3(this)  // 또는 getInput2(this), getInput3(this) 등

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jayneycoffee.location.rainclab.net/#locationui?deviceId=$id&deviceKey=$deviceKey&privateKey=$privateKey"))
            startActivity(intent)
        }


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
