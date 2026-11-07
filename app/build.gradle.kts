plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val openLibraryContact: String = (project.findProperty("openlibrary.contact") as? String) ?: ""

val keyAliasEnv: String? = System.getenv("KEY_ALIAS")
val keyPasswordEnv: String? = System.getenv("KEY_PASSWORD")
val storePasswordEnv: String? = System.getenv("STORE_PASSWORD")
val storeFileEnv: String? = System.getenv("STORE_FILE")

val hasReleaseSigning: Boolean = !keyAliasEnv.isNullOrBlank() &&
    !keyPasswordEnv.isNullOrBlank() &&
    !storePasswordEnv.isNullOrBlank() &&
    !storeFileEnv.isNullOrBlank() &&
    file(storeFileEnv).exists()

android {
    namespace = "com.sniperoftdm.booktracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sniperoftdm.booktracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
                storePassword = storePasswordEnv
                storeFile = file(storeFileEnv!!)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "network"
    productFlavors {
        create("offline") {
            dimension = "network"
            buildConfigField("boolean", "HAS_ONLINE_LOOKUP", "false")
            versionNameSuffix = "-offline"
        }
        create("online") {
            dimension = "network"
            buildConfigField("boolean", "HAS_ONLINE_LOOKUP", "true")
            versionNameSuffix = "-online"
            buildConfigField("String", "OPENLIBRARY_CONTACT", "\"$openLibraryContact\"")
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

    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)

    "onlineImplementation"(libs.okhttp)
    "onlineImplementation"(libs.coil.network.okhttp)
    "onlineImplementation"(libs.play.services.code.scanner)
    "testOnlineImplementation"(libs.okhttp.mockwebserver)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.testing)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.register("verifyOfflineHasNoInternet") {
    group = "verification"
    description = "Verifies that offline variants do not contain android.permission.INTERNET, and online variants do."
    dependsOn(
        "processOfflineDebugMainManifest",
        "processOfflineDebugManifest",
        "processOnlineDebugMainManifest",
        "processOnlineDebugManifest"
    )

    doLast {
        val intermediatesDir = layout.buildDirectory.asFile.get().resolve("intermediates")
        val offlineManifests = fileTree(intermediatesDir).matching {
            include("**/offline*/**/AndroidManifest.xml")
        }.files

        if (offlineManifests.isEmpty()) {
            throw GradleException("verifyOfflineHasNoInternet: No offline AndroidManifest.xml found in intermediates!")
        }

        for (manifest in offlineManifests) {
            val content = manifest.readText()
            if (content.contains("android.permission.INTERNET")) {
                throw GradleException("SECURITY VIOLATION: Offline manifest contains android.permission.INTERNET: ${manifest.path}")
            }
        }

        val onlineManifests = fileTree(intermediatesDir).matching {
            include("**/online*/**/AndroidManifest.xml")
        }.files

        if (onlineManifests.isEmpty()) {
            throw GradleException("verifyOfflineHasNoInternet: No online AndroidManifest.xml found in intermediates!")
        }

        val onlineHasInternet = onlineManifests.any { it.readText().contains("android.permission.INTERNET") }
        if (!onlineHasInternet) {
            throw GradleException("SANITY CHECK FAILED: Online manifest must contain android.permission.INTERNET")
        }
    }
}

tasks.named("check") {
    dependsOn("verifyOfflineHasNoInternet")
}
