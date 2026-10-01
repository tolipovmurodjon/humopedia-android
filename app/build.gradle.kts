import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// The published app's key (build-rules AND-1/AND-2): passwords live in keystore.properties, outside git.
// Without the file a release build comes out unsigned (Play refuses it); it is never signed with the debug key.
val keystoreFile = rootProject.file("keystore.properties")
val keystore = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) } }

android {
    namespace = "com.mcompany.humo"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.mcompany.humo"
        minSdk = 26
        targetSdk = 36
        // Only goes up (AND-3). 27 crashed on launch (R8 renamed a TWA component); 28 is live as 2.0.1.
        versionCode = 29
        versionName = "2.0.2"

    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidbrowserhelper)
    testImplementation(libs.junit)
}
