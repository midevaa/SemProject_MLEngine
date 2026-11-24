import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val modelDir: File = rootDir.resolve("../model")
check(modelDir.resolve("model_metadata.json").isFile) {
    "PlantID model files not found in ${modelDir.normalize()}. The app needs the ML team's " +
        "model/ folder at the repository root (see Mobile-App/docs/AI_INTEGRATION.md)."
}

// Release signing (DECISIONS D44). The key and its passwords live only on the
// machine that builds releases, in Mobile-App/keystore.properties (git-ignored;
// see docs/RELEASE.md). Without that file, release builds are simply unsigned.
val keystoreProperties = Properties().apply {
    val file = rootDir.resolve("keystore.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val hasReleaseKey = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !keystoreProperties.getProperty(it).isNullOrBlank() }

android {
    namespace = "io.github.ephym6.plantid"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.ephym6.plantid"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootDir.resolve(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // The trained model is owned by the ML team in the repository's root `model/`
    // folder (plantid_v1.tflite, labels.txt, model_metadata.json, classes.json).
    // It is packaged straight from there, not copied, so there is one source of
    // truth and no duplicate binary in git (DECISIONS D35).
    sourceSets {
        getByName("main") {
            assets.directories.add(modelDir.path)
        }
        // Exported Room schemas, so device tests can check migrations.
        getByName("androidTest") {
            assets.directories.add("$projectDir/schemas")
        }
    }
    androidResources {
        // Stored uncompressed so the interpreter can memory-map the model.
        noCompress += "tflite"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.litert)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
// Room writes each database schema version here so future migrations can be
// written and tested. Commit the JSON files in app/schemas/.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
