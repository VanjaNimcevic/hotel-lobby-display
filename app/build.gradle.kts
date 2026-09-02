plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.vanja.hotellobbydisplay"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.vanja.hotellobbydisplay"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.leanback)
    implementation(libs.glide)

    // APV-8: video playback (Media3 / ExoPlayer)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // APV-8: local database (Room). room-compiler generates the DAO/database
    // implementation code at build time, so it is an annotationProcessor.
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)

    // APV-8: JSON parsing (Gson)
    implementation(libs.gson)

    // APV-8: background downloads (WorkManager)
    implementation(libs.androidx.work.runtime)
}