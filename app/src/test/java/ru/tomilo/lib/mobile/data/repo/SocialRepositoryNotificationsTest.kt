package ru.tomilo.lib.mobile.data.repo

import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.tomilo.lib.mobile.data.api.ApiResponse
import ru.tomilo.lib.mobile.data.api.TomiloApi

class SocialRepositoryNotificationsTest {
    @Test
    fun markAllNotificationsReadReturnsFailureWhenServerRejectsRequest() = runBlocking {
        val api = notificationsApi(ApiResponse(success = false, message = "HTTP 503"))

        val result = SocialRepository(api).markAllNotificationsRead()

        assertEquals("HTTP 503", result.exceptionOrNull()?.message)
    }

    @Test
    fun markAllNotificationsReadReturnsSuccessWhenServerAcceptsRequest() = runBlocking {
        val api = notificationsApi(ApiResponse(success = true))

        val result = SocialRepository(api).markAllNotificationsRead()

        assertTrue(result.isSuccess)
    }

    @Test
    fun markNotificationReadReturnsFailureWhenServerRejectsRequest() = runBlocking {
        val api = notificationsApi(ApiResponse(success = false, message = "HTTP 503"), "markNotificationRead")

        val result = SocialRepository(api).markNotificationRead("notification-1")

        assertEquals("HTTP 503", result.exceptionOrNull()?.message)
    }

    @Test
    fun markNotificationReadReturnsSuccessWhenServerAcceptsRequest() = runBlocking {
        val api = notificationsApi(ApiResponse(success = true), "markNotificationRead")

        val result = SocialRepository(api).markNotificationRead("notification-1")

        assertTrue(result.isSuccess)
    }

    private fun notificationsApi(
        response: ApiResponse<JsonElement>,
        methodName: String = "markAllNotificationsRead",
    ): TomiloApi =
        Proxy.newProxyInstance(
            TomiloApi::class.java.classLoader,
            arrayOf(TomiloApi::class.java),
        ) { _, method, _ ->
            check(method.name == methodName)
            response
        } as TomiloApi
}
