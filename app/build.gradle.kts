plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

import java.util.Properties

// 환경변수 로드
val envFile = file(".env")
val envProperties = Properties()
if (envFile.exists()) {
    envFile.readLines().forEach { line ->
        if (line.contains("=") && !line.startsWith("#")) {
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) {
                envProperties[parts[0].trim()] = parts[1].trim()
            }
        }
    }
}

android {
    namespace = "com.jennycoffee.locationtracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jennycoffee.locationtracker"
        minSdk = 24
        targetSdk = 34
        versionCode = envProperties.getProperty("APP_VERSION_CODE", "1").toInt()
        versionName = envProperties.getProperty("APP_VERSION", "1.0.0")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "SERVER_URL", "\"${envProperties.getProperty("SERVER_URL", "https://jayneycoffee.api.location.rainclab.net")}\"")
        buildConfigField("String", "WEB_URL", "\"${envProperties.getProperty("WEB_URL", "https://jayneycoffee.location.rainclab.net")}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore/release-key.jks")
            storePassword = envProperties.getProperty("KEYSTORE_PASSWORD", "location123")
            keyAlias = envProperties.getProperty("KEY_ALIAS", "location_tracker_key")
            keyPassword = envProperties.getProperty("KEY_PASSWORD", "location123")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}