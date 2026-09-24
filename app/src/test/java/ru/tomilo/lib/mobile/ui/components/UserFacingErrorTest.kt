package ru.tomilo.lib.mobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.tomilo.lib.mobile.core.toUserFacingError
import ru.tomilo.lib.mobile.core.userFacingError

class UserFacingErrorTest {
    @Test
    fun mapsRateLimitToRetryGuidance() {
        assertEquals(
            "Слишком много запросов подряд. Подождите немного и попробуйте снова.",
            userFacingError("HTTP 429 Too Many Requests"),
        )
    }

    @Test
    fun mapsNotFoundStatusToReadableMessage() {
        assertEquals(
            "Запрошенные данные не найдены. Обновите экран и попробуйте снова.",
            userFacingError("HTTP status code: 404"),
        )
    }

    @Test
    fun mapsAuthenticationPermissionValidationAndServerStatuses() {
        assertEquals("Сессия завершилась. Войдите в аккаунт ещё раз.", userFacingError("HTTP 401"))
        assertEquals("Для этого действия недостаточно прав.", userFacingError("HTTP 403"))
        assertEquals(
            "Не удалось выполнить запрос. Проверьте данные и попробуйте снова.",
            userFacingError("HTTP 422"),
        )
        assertEquals(
            "Сервис временно недоступен. Попробуйте снова немного позже.",
            userFacingError("HTTP 503"),
        )
    }

    @Test
    fun hidesStructuredServerPayload() {
        assertEquals(
            "Произошла техническая ошибка. Попробуйте снова.",
            userFacingError("{\"error\":\"Internal Server Error\"}"),
        )
    }

    @Test
    fun hidesShortTechnicalMessagesFromServer() {
        assertEquals(
            "Произошла техническая ошибка. Попробуйте снова.",
            userFacingError("IllegalStateException: internal payment provider error"),
        )
    }

    @Test
    fun preservesShortActionableServerMessages() {
        assertEquals("Недостаточно монет", userFacingError("Недостаточно монет"))
    }

    @Test
    fun throwableErrorUsesFallbackOrSanitizesTechnicalMessage() {
        assertEquals("Не удалось удалить запись.", IllegalStateException().toUserFacingError("Не удалось удалить запись."))
        assertEquals(
            "Произошла техническая ошибка. Попробуйте снова.",
            IllegalStateException("{\"secret\":\"internal\"}").toUserFacingError("Не удалось удалить запись."),
        )
    }
}
