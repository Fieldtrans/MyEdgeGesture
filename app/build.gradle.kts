import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val signingProperties = Properties().apply {
    val file = rootProject.file("signing.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val normalizedSigningProperties = signingProperties.entries.associate { (key, value) ->
    key.toString()
        .removePrefix("\uFEFF")
        .removePrefix("\u00EF\u00BB\u00BF") to value.toString()
}

fun signingProperty(name: String): String? =
    normalizedSigningProperties[name] ?: System.getenv(name)

val releaseStoreFile = signingProperty("EDGEGESTURE_STORE_FILE")?.let(rootProject::file)
val hasReleaseSigning = releaseStoreFile?.exists() == true &&
        signingProperty("EDGEGESTURE_STORE_PASSWORD") != null &&
        signingProperty("EDGEGESTURE_KEY_ALIAS") != null &&
        signingProperty("EDGEGESTURE_KEY_PASSWORD") != null

android {
    namespace = "com.example.myedgegesture"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.myedgegesture"
        minSdk = 36
        targetSdk = 36
        versionCode = 9
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("releaseLocal") {
                storeFile = releaseStoreFile
                storePassword = signingProperty("EDGEGESTURE_STORE_PASSWORD")
                keyAlias = signingProperty("EDGEGESTURE_KEY_ALIAS")
                keyPassword = signingProperty("EDGEGESTURE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("releaseLocal")
            } else {
                signingConfigs.getByName("debug")
            }
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    compileOnly("de.robv.android.xposed:api:82")
}
