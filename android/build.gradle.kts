plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.voybit.paymentgateway"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.browser:browser:1.8.0")
}
