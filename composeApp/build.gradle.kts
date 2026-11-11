import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.androidApplication)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("tahadhari")
        browser {
            commonWebpackConfig { outputFileName = "tahadhari.js" }
            // Browser tests need a headless Chrome, which dev and CI machines may not have.
            testTask { enabled = false }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        desktopTest.dependencies {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.work.runtime.ktx)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.test.runner)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.java)
        }
    }
}

// AI proxy URL for Android. Default: the hosted server (same as HOSTED_PROXY_URL in ProxyConfig.kt).
// Override in local.properties: tahadhari.proxyUrl=http://10.0.2.2:8080 reaches a server on your
// computer from the emulator; blank means offline text only.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val proxyUrl: String = localProps.getProperty("tahadhari.proxyUrl") ?: "https://tahadhari-ai.onrender.com"

// Release signing: keystore.properties (ignored by version control, see keystore.properties.example)
// or TAHADHARI_KEYSTORE_* environment variables. Both absent = debug-signed release (warning).
class ReleaseSigning(val storeFile: File, val storePassword: String, val keyAlias: String, val keyPassword: String)

val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun signingValue(prop: String, env: String): String? =
    (keystoreProps.getProperty(prop) ?: System.getenv(env))?.takeIf { it.isNotBlank() }

val releaseSigning: ReleaseSigning? = run {
    val path = signingValue("storeFile", "TAHADHARI_KEYSTORE_FILE")
    val storePassword = signingValue("storePassword", "TAHADHARI_KEYSTORE_PASSWORD")
    val alias = signingValue("keyAlias", "TAHADHARI_KEY_ALIAS")
    val keyPassword = signingValue("keyPassword", "TAHADHARI_KEY_PASSWORD") ?: storePassword
    val file = path?.let { rootProject.file(it) }
    if (file != null && file.exists() && storePassword != null && alias != null && keyPassword != null) {
        ReleaseSigning(file, storePassword, alias, keyPassword)
    } else {
        null
    }
}

android {
    namespace = "ke.tahadhari"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "ke.tahadhari"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // Single source of truth: gradle/libs.versions.toml. AppInfo.APP_VERSION (commonMain) must match.
        versionCode = libs.versions.app.versionCode.get().toInt()
        versionName = libs.versions.app.version.get()
        buildConfigField("String", "PROXY_URL", "\"$proxyUrl\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { buildConfig = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = releaseSigning.storeFile
                storePassword = releaseSigning.storePassword
                keyAlias = releaseSigning.keyAlias
                keyPassword = releaseSigning.keyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Never fail a clean clone: without signing values the release APK uses the debug key.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug").also {
                logger.warn(
                    "WARNING: no release signing config (keystore.properties or TAHADHARI_KEYSTORE_* env vars); " +
                        "the release APK is signed with the DEBUG key and must not be published.",
                )
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Lets the Compose UI test rule launch an activity (instrumented tests only).
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

compose.desktop {
    application {
        mainClass = "ke.tahadhari.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "Tahadhari"
            packageVersion = libs.versions.app.version.get() // must match AppInfo.APP_VERSION
            description = "Plain-language flood alerts for Kenya's 47 counties"
            vendor = "Tahadhari"
            copyright = "Copyright (c) 2026 Tahadhari. MIT License."
            licenseFile.set(rootProject.file("LICENSE"))
            windows {
                iconFile.set(project.file("icons/tahadhari.ico"))
                menuGroup = "Tahadhari"
                upgradeUuid = "6f0c4b1e-8a3d-4c57-9e21-b7d5a1c3f842"
                perUserInstall = true
            }
            macOS {
                iconFile.set(project.file("icons/tahadhari.icns"))
                bundleID = "ke.tahadhari"
            }
            linux {
                iconFile.set(project.file("icons/tahadhari.png"))
            }
        }
    }
}
