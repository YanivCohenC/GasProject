plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.yaniv.FullTank"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yaniv.FullTank"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation (libs.osmdroid.android)
    implementation (libs.osmdroid.wms)
    implementation (libs.osmdroid.mapsforge)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.location)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}