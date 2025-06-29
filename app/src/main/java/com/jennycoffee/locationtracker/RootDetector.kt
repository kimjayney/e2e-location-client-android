package com.jennycoffee.locationtracker

import android.content.Context
import android.os.Build
import java.io.File

object RootDetector {
    
    fun isDeviceRooted(context: Context): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3() || checkRootMethod4(context)
    }
    
    // 방법 1: 일반적인 루팅 앱/파일 확인
    private fun checkRootMethod1(): Boolean {
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        for (path in rootPaths) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }
    
    // 방법 2: which 명령어로 su 확인
    private fun checkRootMethod2(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val inputStream = process.inputStream
            val bufferedReader = inputStream.bufferedReader()
            val result = bufferedReader.readLine()
            bufferedReader.close()
            inputStream.close()
            result != null
        } catch (e: Exception) {
            false
        }
    }
    
    // 방법 3: Build.TAGS 확인
    private fun checkRootMethod3(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
    
    // 방법 4: 일반적인 루팅 관련 앱 패키지 확인
    private fun checkRootMethod4(context: Context): Boolean {
        val rootPackages = arrayOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.qihoo.permmgr",
            "com.alephzain.framaroot"
        )
        
        val packageManager = context.packageManager
        for (packageName in rootPackages) {
            try {
                packageManager.getPackageInfo(packageName, 0)
                return true
            } catch (e: Exception) {
                // 패키지가 없으면 계속 진행
            }
        }
        return false
    }
} 