import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services) apply false
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
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.tomilo.lib.mobile"
        minSdk = 26
        targetSdk = 36
        // RuStore / production consumer release
        // Каждый production-релиз получает новый versionCode: магазины не
        // позволяют заменить уже загруженную сборку тем же кодом версии.
        versionCode = 43
        versionName = "1.3.7"

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
     * - play — Google Play. Консоль Play уже зарегистрирована на
     *   `ru.tomilolib.mobile`, поэтому applicationId этого flavor задан явно.
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
            applicationId = "ru.tomilolib.mobile"
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    implementation(libs.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    testImplementation(libs.junit)

    implementation(libs.work.runtime.ktx)

    // Яндекс РСЯ — rewarded (R-M-…)
    implementation(libs.yandex.mobileads)

    // FCM: push-уведомления (fallback — NotificationsPollWorker, для устройств
    // без Google Play Services getToken() просто падает, ловим try/catch)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    // RuStore Push SDK: второй канал push (актуален для rustore-флейвора,
    // где Google Play Services обычно нет). Тоже деградирует в polling.
    implementation(libs.rustore.pushclient)

    // RuStore native store features. They are guarded by STORE_CHANNEL at runtime,
    // so Play builds keep working while the rustore flavor receives native flows.
    implementation(libs.rustore.appupdate)
    implementation(libs.rustore.review)
}
