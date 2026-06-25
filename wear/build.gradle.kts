import java.util.Properties

// Wear OS app. AGP + Kotlin plugin versions come from :app's classpath (declared once there), so
// they're applied here without a version. The Compose compiler plugin is NOT used by :app, so it
// must carry its version here.
plugins {
    // Versions are declared centrally in settings.gradle (pluginManagement.plugins).
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing — same keystore as :app (required for Data Layer pairing). Reads
// keystore.properties (local) or VOCALIS_* env vars (CI).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
    }

fun releaseProp(
    key: String,
    env: String,
): String? = keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStoreFile = releaseProp("storeFile", "VOCALIS_STORE_FILE")

android {
    namespace = "website.ahdesign.vocalis.wear"
    compileSdk = 36

    defaultConfig {
        // Must match the phone app's applicationId + signing key for Data Layer pairing.
        applicationId = "website.ahdesign.vocalis"
        minSdk = 30 // Wear OS 3+
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile != null) {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseProp("storePassword", "VOCALIS_STORE_PASSWORD")
                keyAlias = releaseProp("keyAlias", "VOCALIS_KEY_ALIAS")
                keyPassword = releaseProp("keyPassword", "VOCALIS_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
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
}

dependencies {
    implementation(project(":core")) // shared contract + pure logic

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose (versions aligned by the BOM) + Wear Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")

    // Data Layer (watch <-> phone)
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.1")
}
