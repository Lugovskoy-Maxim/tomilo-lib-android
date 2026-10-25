import java.util.Properties
import java.security.MessageDigest

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

fun configuredPlayAdUnit(name: String): String =
    (keystoreProperties.getProperty(name)
        ?: providers.gradleProperty(name).orNull
        ?: System.getenv(name))
        ?.trim()
        .orEmpty()

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val rustoreAdUnitIds = mapOf(
    "YANDEX_REWARDED_AD_UNIT_ID" to "R-M-19689456-1",
    "YANDEX_REWARDED_AD_UNIT_ID_2" to "R-M-19689456-3",
    "YANDEX_INTERSTITIAL_AD_UNIT_ID" to "R-M-19689456-2",
    "YANDEX_NATIVE_CATALOG_AD_UNIT_ID" to "R-M-19689456-4",
)

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
        versionCode = 53
        versionName = "1.5.1"

        buildConfigField("String", "API_BASE_URL", "\"https://tomilo-lib.ru/api/\"")
        buildConfigField("String", "CDN_BASE_URL", "\"https://cdn.tomilo-lib.ru\"")
        buildConfigField("String", "S3_BASE_URL", "\"https://s3.regru.cloud/tomilolib\"")
        buildConfigField("String", "SITE_URL", "\"https://tomilo-lib.ru\"")
        buildConfigField("String", "GITHUB_REPO", "\"Lugovskoy-Maxim/tomilo-lib-android\"")
        buildConfigField("boolean", "HAS_FCM", hasGoogleServices.toString())
        // ID проекта из RuStore Консоль → Push-уведомления → Проекты (пусто = SDK не инициализируется)
        val rustorePushProjectId = prop("RUSTORE_PUSH_PROJECT_ID") ?: ""
        buildConfigField("String", "RUSTORE_PUSH_PROJECT_ID", "\"$rustorePushProjectId\"")
        // Ad units are set per store flavor; empty defaults keep accidental cross-store use off.
        buildConfigField("String", "YANDEX_REWARDED_AD_UNIT_ID", "\"\"")
        buildConfigField("String", "YANDEX_REWARDED_AD_UNIT_ID_2", "\"\"")
        buildConfigField("String", "YANDEX_INTERSTITIAL_AD_UNIT_ID", "\"\"")
        buildConfigField("String", "YANDEX_NATIVE_CATALOG_AD_UNIT_ID", "\"\"")
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
            // RuStore's current Yandex ad units.
            buildConfigField("String", "YANDEX_REWARDED_AD_UNIT_ID", buildConfigString(rustoreAdUnitIds.getValue("YANDEX_REWARDED_AD_UNIT_ID")))
            buildConfigField("String", "YANDEX_REWARDED_AD_UNIT_ID_2", buildConfigString(rustoreAdUnitIds.getValue("YANDEX_REWARDED_AD_UNIT_ID_2")))
            buildConfigField("String", "YANDEX_INTERSTITIAL_AD_UNIT_ID", buildConfigString(rustoreAdUnitIds.getValue("YANDEX_INTERSTITIAL_AD_UNIT_ID")))
            buildConfigField("String", "YANDEX_NATIVE_CATALOG_AD_UNIT_ID", buildConfigString(rustoreAdUnitIds.getValue("YANDEX_NATIVE_CATALOG_AD_UNIT_ID")))
            // Имя приложения в лаунчере для стора
            resValue("string", "app_name", "TOMILO LIB")
        }
        create("play") {
            dimension = "store"
            // Play-сборка использует отдельные блоки РСЯ из PLAY_YANDEX_* ниже.
            // Пустой ID отключает только соответствующий рекламный формат.
            applicationId = "ru.tomilolib.mobile"
            buildConfigField("String", "STORE_CHANNEL", "\"play\"")
            buildConfigField("boolean", "IS_CONSUMER_BUILD", "true")
            // Set PLAY_YANDEX_* in ~/.gradle/gradle.properties or the build environment.
            // Blank values disable that ad format until its Play-specific unit is configured.
            buildConfigField("String", "YANDEX_REWARDED_AD_UNIT_ID", buildConfigString(configuredPlayAdUnit("PLAY_YANDEX_REWARDED_AD_UNIT_ID")))
            buildConfigField("String", "YANDEX_REWARDED_AD_UNIT_ID_2", buildConfigString(configuredPlayAdUnit("PLAY_YANDEX_REWARDED_AD_UNIT_ID_2")))
            buildConfigField("String", "YANDEX_INTERSTITIAL_AD_UNIT_ID", buildConfigString(configuredPlayAdUnit("PLAY_YANDEX_INTERSTITIAL_AD_UNIT_ID")))
            buildConfigField("String", "YANDEX_NATIVE_CATALOG_AD_UNIT_ID", buildConfigString(configuredPlayAdUnit("PLAY_YANDEX_NATIVE_CATALOG_AD_UNIT_ID")))
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
            // Версия debug-артефакта отличается суффиксом; package ID задаётся flavor.
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

// Release Play artifacts must never ship with blank, reused, or duplicate ad units.
val playAdUnitProperties = listOf(
    "PLAY_YANDEX_REWARDED_AD_UNIT_ID",
    "PLAY_YANDEX_REWARDED_AD_UNIT_ID_2",
    "PLAY_YANDEX_INTERSTITIAL_AD_UNIT_ID",
    "PLAY_YANDEX_NATIVE_CATALOG_AD_UNIT_ID",
)
val verifyPlayAdUnitIsolation by tasks.registering {
    group = "verification"
    description = "Requires unique Google Play ad units separate from RuStore."
    doLast {
        val playUnits = playAdUnitProperties.associateWith(::configuredPlayAdUnit)
        val missing = playUnits.filterValues { it.isBlank() }.keys
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Google Play release requires its own Yandex ad units: ${missing.joinToString()}. " +
                    "Configure these PLAY_YANDEX_* secrets before building a Play release.",
            )
        }

        val rustoreUnits = rustoreAdUnitIds.values.toSet()
        val reusedRustoreUnits = playUnits.filterValues { it in rustoreUnits }.keys
        if (reusedRustoreUnits.isNotEmpty()) {
            throw GradleException(
                "Google Play ad units must be separate from RuStore: ${reusedRustoreUnits.joinToString()}.",
            )
        }

        val duplicatePlayUnits = playUnits.entries
            .groupBy { it.value.lowercase() }
            .values
            .filter { it.size > 1 }
            .flatten()
            .map { it.key }
        if (duplicatePlayUnits.isNotEmpty()) {
            throw GradleException(
                "Each Google Play placement needs a unique Yandex unit: ${duplicatePlayUnits.joinToString()}.",
            )
        }
    }
}

val playReleasePackagingTasks = setOf(
    "assemblePlayRelease",
    "bundlePlayRelease",
    "packagePlayRelease",
    "packagePlayReleaseBundle",
)
tasks.configureEach {
    if (name in playReleasePackagingTasks) {
        dependsOn(verifyPlayAdUnitIsolation)
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
    implementation(libs.androidx.browser)
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

// The offline game must ship exactly the files recorded by its verified build.
val verifySectTrainingBundle = tasks.register("verifySectTrainingBundle") {
    val bundle = layout.projectDirectory.dir("src/main/assets/sect-training")
    inputs.dir(bundle)
    doLast {
        val directory = bundle.asFile
        val manifestFile = directory.resolve("bundle-manifest.json")
        val manifest = groovy.json.JsonSlurper().parse(manifestFile) as Map<*, *>
        require(manifest["format"] == 2 && manifest["offlineTrainingOnly"] == true && manifest["hostModes"] == listOf("training", "campaign")) { "Invalid sect game manifest" }
        val expected = manifest["files"] as Map<*, *>
        val actual = fileTree(directory).files.filter { it != manifestFile }.associateBy { it.relativeTo(directory).invariantSeparatorsPath }
        require(actual.keys == expected.keys) { "Sect training file list mismatch" }
        actual.forEach { (name, file) ->
            val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
            require(hash == expected[name]) { "Sect training hash mismatch: $name" }
        }
    }
}
tasks.named("preBuild") { dependsOn(verifySectTrainingBundle) }
