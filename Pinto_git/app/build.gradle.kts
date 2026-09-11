plugins {
    id("com.android.application")
}

android {
    namespace = "dev.milky.pinto"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.milky.pinto"
        minSdk = 28
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.1"

        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
