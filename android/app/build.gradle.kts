plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.gymapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.gymapp"
        // Glass needs RenderEffect/RuntimeShader (Android 13+); older phones get plain blur.
        // 28 for ImageDecoder (upright, EXIF-rotated photos for the posture scan).
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        // ponytail: arm64 only (the Pixel 7a); MediaPipe's other ABIs add ~100 MB. Add more if another phone needs it.
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    androidResources {
        // The pose model is memory-mapped by MediaPipe, so it must stay uncompressed in the APK.
        noCompress += "task"
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.backdrop)
    implementation(libs.kyant.shapes)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    // Exercise pictures from free-exercise-db, cached on disk.
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    // On-device pose landmarks for the posture scan (Apache-2.0); photos never leave the phone.
    implementation(libs.mediapipe.vision)
    // Google's barcode scanner UI (no camera permission needed) for packaged food.
    implementation(libs.code.scanner)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
}
