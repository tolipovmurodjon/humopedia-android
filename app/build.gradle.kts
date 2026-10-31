import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.util.Properties

plugins {
    // AGP 9 compiles Kotlin itself (built-in Kotlin): no org.jetbrains.kotlin.android plugin.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
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
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mcompany.humo"
        minSdk = 26
        targetSdk = 36
        // Only goes up (AND-3). 34 = 2.3.0, the last TWA build. 35 = the first native build (NATIVE-BRIEF N0).
        versionCode = 42
        versionName = "3.0.0-alpha08"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric (Compose UI and screenshot tests on the JVM) needs the merged resources.
        unitTests.isIncludeAndroidResources = true
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Real liquid glass on the chrome (the bottom bar first)
    implementation(libs.kyant.backdrop)
    implementation(libs.kyant.shapes)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // The learner's progress on the path (small, local; the server copy comes with accounts)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Push, analytics and crash reports, as in 2.x
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.installations)
    debugImplementation(libs.firebase.appcheck.debug)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
