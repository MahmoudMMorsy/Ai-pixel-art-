plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.retro.pixelanimator"
    compileSdk = 34

    signingConfigs {
        create("releaseSigning") {
            storeFile = file("retro_release.keystore")
            storePassword = "retro12345"
            keyAlias = "retro_alias"
            keyPassword = "retro12345"
        }
    }

    defaultConfig {
        applicationId = "com.retro.pixelanimator"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val geminiKey = System.getenv("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")

        // Configure native compiler arguments for stable-diffusion.cpp
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_shared"
                cppFlags += "-O3 -DGGML_MAX_NAME=128"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("releaseSigning")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("releaseSigning")
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
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.ui:ui:1.6.2")
    implementation("androidx.compose.ui:ui-graphics:1.6.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.2")
    implementation("androidx.compose.material3:material3:1.2.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.mediapipe:tasks-vision-image-generator:latest.release")

    debugImplementation("androidx.compose.ui:ui-tooling:1.6.2")
}
