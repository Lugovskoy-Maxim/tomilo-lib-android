import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services") apply false
}

/**
 * FCM (push-уведомления). Применяем плагин только если google-services.json
 * реально существует — иначе `nest build`/сборка падает у всех, у кого его
 * ещё нет (проект без сконфигурированного Firebase). См. README для настройки.
 */
val hasGoogleServices = rootProject.file("app/google-services.json").exists()
if (hasGoogleServices) {
    apply(plugin = "com.google.gms.google-services")
}

/** Release-подпись: keystore.properties в корне проекта (не в git). */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun prop(name: String): String? =
    keystoreProperties.getProperty(name)
        ?: System.getenv(name)

val hasReleaseSigning =
    !prop("STORE_FILE").isNullOrBlank() &&
        !prop("STORE_PASSWORD").isNullOrBlank() &&
        !prop("KEY_ALIAS").isNullOrBlank() &&
        !prop("KEY_PASSWORD").isNullOrBlank()

android {
    namespace = "ru.tomilo.lib.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.tomilo.lib.mobile"
        minSdk = 26
        targetSdk = 35
        // RuStore / production consumer release
        versionCode = 32
        versionName = "1.2.17"

        buildConfigField("String", "API_BASE_URL", "\"https://tomilo-lib.ru/api/\"")
        buildConfigField("String", "CDN_BASE_URL", "\"https://cdn.tomilo-lib.ru\"")
        buildConfigField("String", "S3_BASE_URL", "\"https://s3.regru.cloud/tomilolib\"")
        buildConfigField("String", "SITE_URL", "\"https://tomilo-lib.ru\"")
        buildConfigField("String", "GITHUB_REPO", "\"Lugovskoy-Maxim/tomilo-lib-android\"")
        buildConfigField("boolean", "HAS_FCM", hasGoogleServices.toString())
        // ID проекта из RuStore Консоль → Push-уведомления → Проекты (пусто = SDK не инициализируется)
        val rustorePushProjectId = prop("RUSTORE_PUSH_PROJECT_ID") ?: ""
        buildConfigField("String", "RUSTORE_PUSH_PROJECT_ID", "\"$rustorePushProjectId\"")
        // РСЯ: «Реклама с вознаграждением 02-08-2026», валюта Reward, сумма 1
        buildConfigField("String", "YANDEX_REWARDED_AD_UNIT_ID", "\"R-M-19689456-1\"")
        // Interstitial между главами (~1/10 мин), блок РСЯ «Межстраничная»
        buildConfigField("String", "YANDEX_INTERSTITIAL_AD_UNIT_ID", "\"R-M-19689456-2\"")
        // по умолчанию (переопределяется flavor)
        buildConfigField("String", "STORE_CHANNEL", "\"rustore\"")
        buildConfigField("boolean", "IS_CONSUMER_BUILD", "true")
    }

    /**
     * Каналы магазинов.
     * - rustore — обычные пользователи, RuStore (APK/AAB, isDefault)
     * - play — Google Play
     * Один applicationId, один signing key → единая линейка обновлений.
     */
    flavorDimensions += "store"
    productFlavors {
        create("rustore") {
            dimension = "store"
            isDefault = true
            buildConfigField("String", "STORE_CHANNEL", "\"rustore\"")
            buildConfigField("boolean", "IS_CONSUMER_BUILD", "true")
            // Имя приложения в лаунчере для стора
            resValue("string", "app_name", "TOMILO LIB")
        }
        create("play") {
            dimension = "store"
            buildConfigField("String", "STORE_CHANNEL", "\"play\"")
            buildConfigField("boolean", "IS_CONSUMER_BUILD", "true")
            resValue("string", "app_name", "TOMILO LIB")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                val storePath = prop("STORE_FILE")!!
                storeFile = rootProject.file(storePath)
                storePassword = prop("STORE_PASSWORD")
                keyAlias = prop("KEY_ALIAS")
                keyPassword = prop("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    // Удобные имена артефактов: tomilo-rustore-1.2.4-release.apk
    applicationVariants.configureEach {
        val variant = this
        outputs.configureEach {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val ver = variant.versionName ?: "0"
            val flavor = variant.flavorName.ifBlank { "main" }
            val type = variant.buildType.name
            val qualifiedVersion = if (ver.endsWith("-$type")) ver else "$ver-$type"
            output.outputFileName = "tomilo-$flavor-$qualifiedVersion.apk"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    testImplementation("junit:junit:4.13.2")

    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Яндекс РСЯ — rewarded (R-M-…)
    implementation("com.yandex.android:mobileads:8.3.0")

    // FCM: push-уведомления (fallback — NotificationsPollWorker, для устройств
    // без Google Play Services getToken() просто падает, ловим try/catch)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // RuStore Push SDK: второй канал push (актуален для rustore-флейвора,
    // где Google Play Services обычно нет). Тоже деградирует в polling.
    implementation("ru.rustore.sdk:pushclient:7.4.0")

    // RuStore native store features. They are guarded by STORE_CHANNEL at runtime,
    // so Play builds keep working while the rustore flavor receives native flows.
    implementation("ru.rustore.sdk:appupdate:8.0.0")
    implementation("ru.rustore.sdk:review:10.0.0")
}
