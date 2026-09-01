plugins { id("com.android.application") }

android {
    namespace = "de.cyberhoe.iptv"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.cyberhoe.iptv"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.1"
    }

    buildFeatures { buildConfig = true }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")
}
