plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.jbk.mlapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jbk.mlapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    androidResources {
        noCompress; "tflite"
        noCompress; "lite"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.litert)
    testImplementation(libs.junit)
    // https://mvnrepository.com/artifact/com.nex3z/finger-paint-view
    implementation(libs.finger.paint.view)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    //implementation("org.tensorflow:tensorflow-lite:2.16.1")



}