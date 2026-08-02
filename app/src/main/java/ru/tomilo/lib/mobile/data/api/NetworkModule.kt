package ru.tomilo.lib.mobile.data.api

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import ru.tomilo.lib.mobile.BuildConfig
import java.io.File
import java.util.concurrent.TimeUnit

object NetworkModule {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun createApi(context: Context, tokenProvider: () -> String?): TomiloApi {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 50L * 1024L * 1024L) // 50 MB

        val authInterceptor = Interceptor { chain ->
            val token = tokenProvider()
            val req = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(req)
        }

        // Кеш только публичных GET без Authorization (иначе 401/пустые чаты залипают)
        val cacheInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            val isPublicGet = request.method == "GET" &&
                request.header("Authorization").isNullOrBlank()
            if (isPublicGet && response.isSuccessful) {
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=120")
                    .removeHeader("Pragma")
                    .build()
            } else {
                response.newBuilder()
                    .header("Cache-Control", "no-store")
                    .build()
            }
        }

        val offlineCacheInterceptor = Interceptor { chain ->
            var request = chain.request()
            val isPublicGet = request.method == "GET" &&
                request.header("Authorization").isNullOrBlank()
            if (isPublicGet && !isNetworkAvailable(context)) {
                request = request.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=${60 * 60 * 24 * 7}")
                    .build()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(offlineCacheInterceptor)
            .addInterceptor(authInterceptor)
            .addNetworkInterceptor(cacheInterceptor)
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(TomiloApi::class.java)
    }

    /** Shared client for image/page downloads (disk cache). */
    fun createMediaClient(context: Context): OkHttpClient {
        val cache = Cache(File(context.cacheDir, "media_cache"), 200L * 1024L * 1024L)
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }
}
