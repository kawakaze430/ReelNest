plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val apiOrigin = providers.gradleProperty("reelnestApiOrigin").orElse("https://api.example.org").get()
require(Regex("^https://[A-Za-z0-9.-]+(?::[0-9]{2,5})?$").matches(apiOrigin)) {
    "Only a reviewed HTTPS public API origin is allowed"
}
android {
    namespace = "com.reelnest.catalogpreview"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.reelnest.catalogpreview"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_ORIGIN", "\"$apiOrigin\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
