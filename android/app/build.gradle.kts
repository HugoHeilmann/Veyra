plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.veyra"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.veyra"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("veyra-key.jks")
            storePassword = "L1fc1e.L2c'eq'olfe!"
            keyAlias = "veyraKey"
            keyPassword = "L1fc1e.L2c'eq'olfe!"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")

    // NewPipe / extraction
    implementation("com.github.teamnewpipe:newpipeextractor:v0.25.1")

    // FFmpeg kit (local AAR + exceptions)
    implementation(files("libs/ffmpeg-kit.aar"))
    implementation("com.arthenica:smart-exception-common:0.2.1")
    implementation("com.arthenica:smart-exception-java:0.2.1")
    implementation("com.arthenica:smart-exception-java9:0.2.1")

    // Coil
    implementation("io.coil-kt:coil-compose:2.4.0")

    // MediaCompat (ancienne API – ok si encore utilisée)
    implementation("androidx.media:media:1.7.0")

    // Guava (ListenableFuture pour Media3)
    implementation("com.google.guava:guava:32.0.1-android")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Material
    implementation("com.google.android.material:material:1.11.0")

    // Datastore
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // Media3 (ExoPlayer + Session + UI)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Glance (widget)
    implementation("androidx.glance:glance-appwidget:1.1.0")

    // Core / lifecycle / activity
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.navigation:navigation-runtime-android:2.9.2")
}