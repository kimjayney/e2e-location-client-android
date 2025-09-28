plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

import java.util.Properties

// 환경변수 로드
val envFile = rootProject.file(".env")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        baseline = file("lint-baseline.xml")
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
    implementation("com.google.android.material:material:1.12.0") // Material 2 라이브러리 추가
    implementation(libs.androidx.appcompat)

    // Firebase (BOM - Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx")
    
    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // QR 코드 스캐너 (ZXing)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // 코루틴
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2") // lifecycleScope 사용

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.2")
    implementation("androidx.activity:activity-ktx:1.9.0") // by viewModels() 사용
}