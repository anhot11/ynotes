import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "app.uamo.ynotes"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.uamo.ynotes"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2"
    }

    signingConfigs {
        create("release") {
            val keystoreBase64 = System.getenv("KEYSTORE_BASE64")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")
            
            if (!keystoreBase64.isNullOrEmpty()) {
                val keystoreFile = file("keystore.jks")
                // Eliminar posibles saltos de línea/espacios antes de decodificar
                val cleanBase64 = keystoreBase64.replace("\\s".toRegex(), "")
                keystoreFile.writeBytes(Base64.getDecoder().decode(cleanBase64))
                
                storeFile = keystoreFile
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else if (file("local-keystore.jks").exists()) {
                storeFile = file("local-keystore.jks")
                storePassword = "password"
                this.keyAlias = "ynotes"
                this.keyPassword = "password"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystoreBase64 = System.getenv("KEYSTORE_BASE64")
            if (!keystoreBase64.isNullOrEmpty() || file("local-keystore.jks").exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Glance AppWidget
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")
    
    // Navigation & ViewModel
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
    
    // Room
    val room_version = "2.6.0"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}
