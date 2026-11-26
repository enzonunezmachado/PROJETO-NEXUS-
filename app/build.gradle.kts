import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val nexusKeystoreProperties = Properties().apply {
    val propFile = rootProject.file("keystore.properties")
    if (propFile.isFile) {
        propFile.inputStream().use { load(it) }
    }
}

fun signingOption(propKey: String, envKey: String): String? =
    (nexusKeystoreProperties.getProperty(propKey)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() })?.trim()

fun resolveKeystoreFile(path: String): java.io.File {
    val candidate = file(path)
    return if (candidate.isFile) candidate else rootProject.file(path)
}

android {
    namespace = "com.nexus.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexus.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "5.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val extDebugPath = signingOption("debugStoreFile", "NEXUS_DEBUG_KEYSTORE_PATH")
        val extDebugStorePwd = signingOption("debugStorePassword", "NEXUS_DEBUG_KEYSTORE_PASSWORD")
        val extDebugAlias = signingOption("debugKeyAlias", "NEXUS_DEBUG_KEYSTORE_ALIAS")
            ?: signingOption("debugKeyAlias", "NEXUS_DEBUG_KEY_ALIAS")
        val extDebugKeyPwd = signingOption("debugKeyPassword", "NEXUS_DEBUG_KEY_PASSWORD")
        if (extDebugPath != null &&
            extDebugStorePwd != null &&
            extDebugAlias != null &&
            extDebugKeyPwd != null
        ) {
            val keystoreFile = resolveKeystoreFile(extDebugPath)
            if (keystoreFile.isFile) {
                getByName("debug") {
                    storeFile = keystoreFile
                    storePassword = extDebugStorePwd
                    keyAlias = extDebugAlias
                    keyPassword = extDebugKeyPwd
                }
            }
        }

        val releasePath = signingOption("storeFile", "NEXUS_KEYSTORE_PATH")
        val releaseStorePwd = signingOption("storePassword", "NEXUS_KEYSTORE_PASSWORD")
        val releaseAlias = signingOption("keyAlias", "NEXUS_KEY_ALIAS")
        val releaseKeyPwd = signingOption("keyPassword", "NEXUS_KEY_PASSWORD")
        if (releasePath != null &&
            releaseStorePwd != null &&
            releaseAlias != null &&
            releaseKeyPwd != null
        ) {
            val keystoreFile = resolveKeystoreFile(releasePath)
            if (keystoreFile.isFile) {
                create("release") {
                    storeFile = keystoreFile
                    storePassword = releaseStorePwd
                    keyAlias = releaseAlias
                    keyPassword = releaseKeyPwd
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
