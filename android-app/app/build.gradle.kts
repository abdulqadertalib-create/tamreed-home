plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

android {
    namespace = "iq.tamreed.home"

    compileSdk = 36

    signingConfigs {
        val keystorePath = System.getenv("TAMREED_KEYSTORE_PATH")
        val keystorePassword = System.getenv("TAMREED_KEYSTORE_PASSWORD")
        val keyAliasEnv = System.getenv("TAMREED_KEY_ALIAS")
        val keyPassword = System.getenv("TAMREED_KEY_PASSWORD")

        if (!keystorePath.isNullOrBlank() &&
            !keystorePassword.isNullOrBlank() &&
            !keyAliasEnv.isNullOrBlank() &&
            !keyPassword.isNullOrBlank()
        ) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasEnv
                this.keyPassword = keyPassword
                storeType = "PKCS12"
            }
        }
    }

    defaultConfig {
        applicationId = "iq.tamreed.home"

        minSdk = 26
        targetSdk = 36

        versionCode = 3
        versionName = "1.0.2"

        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"sb_publishable_HtMExFgxiFq_qhN2I9V76w_0YFG6L0j\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            // التوقيع يُفعّل فقط عند توفر متغيرات التوقيع في GitHub Actions.
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }

            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {

    // Google Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.3"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    // Ktor
    implementation("io.ktor:ktor-client-android:3.0.3")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Firebase Cloud Messaging (FCM)
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-messaging")
}
