import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.play.publisher)
}

// Firebase reads app/google-services.json (Boss's, outside git, AND-2). CI has no copy, so the plugins apply only when it
// is there, and a release build without it fails rather than shipping an app whose Firebase calls throw.
val firebaseConfig = file("google-services.json")
if (firebaseConfig.exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
    apply(plugin = libs.plugins.firebase.perf.get().pluginId)
    apply(plugin = libs.plugins.firebase.appdistribution.get().pluginId)
}
gradle.taskGraph.whenReady {
    if (!firebaseConfig.exists() && allTasks.any { it.name.contains("Release") }) {
        throw GradleException("app/google-services.json is missing: release builds need Firebase.")
    }
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

// One command to Google Play (`./gradlew publishBundle`) instead of clicking through Play Console. The service account
// key is Boss's, outside git (AND-2); without it only the publish tasks fail.
play {
    serviceAccountCredentials.set(rootProject.file("serviceAccount-play.json"))
    track.set("production")
    releaseStatus.set(ReleaseStatus.COMPLETED)
    defaultToAppBundles.set(true)
}

dependencies {
    implementation(libs.androidbrowserhelper)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)
    debugImplementation(libs.firebase.appcheck.debug)
    testImplementation(libs.junit)
}
