plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
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
    implementation("com.github.TeamNewPipe:newpipeextractor:master-SNAPSHOT")
    implementation(files("libs/ffmpeg-kit.aar"))
    implementation("com.arthenica:smart-exception-common:0.2.1")
    implementation("com.arthenica:smart-exception-java:0.2.1")
    implementation("com.arthenica:smart-exception-java9:0.2.1")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("androidx.media:media:1.7.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.navigation:navigation-runtime-android:2.9.2")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")
}
