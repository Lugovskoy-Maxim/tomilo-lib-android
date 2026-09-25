package ru.tomilo.lib.mobile.core

import java.util.Locale

/** Converts transport and server failures into short, actionable copy for app surfaces. */
fun userFacingError(raw: String): String {
    val message = raw.trim()
    val lower = message.lowercase(Locale.ROOT)
    val httpStatus = Regex(
        "(?:http(?:\\s+status(?:\\s+code)?)?\\s*|status(?:\\s+code)?\\s*[:=]?\\s*|response\\s+code\\s*[:=]?\\s*)(\\d{3})",
        RegexOption.IGNORE_CASE,
    ).find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return when {
        message.isBlank() || lower == "ошибка" ->
            "Не удалось получить данные. Проверьте подключение и попробуйте снова."
        httpStatus == 401 || lower.contains("unauthorized") || lower.contains("invalid token") ->
            "Сессия завершилась. Войдите в аккаунт ещё раз."
        httpStatus == 403 || lower.contains("forbidden") ->
            "Для этого действия недостаточно прав."
        httpStatus == 404 ->
            "Запрошенные данные не найдены. Обновите экран и попробуйте снова."
        httpStatus == 429 || lower.contains("too many requests") || lower.contains("rate limit") ->
            "Слишком много запросов подряд. Подождите немного и попробуйте снова."
        httpStatus == 400 || httpStatus == 422 ->
            "Не удалось выполнить запрос. Проверьте данные и попробуйте снова."
        httpStatus in 500..599 ->
            "Сервис временно недоступен. Попробуйте снова немного позже."
        lower.contains("unable to resolve host") ||
            lower.contains("failed to connect") ||
            lower.contains("network is unreachable") ||
            lower.contains("no address associated") ||
            lower.contains("connection reset") ||
            lower.contains("socketexception") ->
            "Нет подключения к серверу. Проверьте интернет и попробуйте снова."
        lower.contains("timeout") || lower.contains("timed out") ->
            "Сервер отвечает слишком долго. Попробуйте ещё раз через несколько секунд."
        message.length > 180 ||
            lower.contains("exception") ||
            lower.contains("retrofit2.") ||
            lower.contains("kotlinx.coroutines") ||
            lower.contains("unexpected json token") ||
            lower.contains("expected start of") ||
            lower.startsWith("expected ") && (
                lower.contains(" at line ") ||
                    lower.contains(" at path ") ||
                    lower.contains("begin_object") ||
                    lower.contains("begin_array")
                ) ||
            lower.startsWith("cannot deserialize") ||
            lower.contains("jsonsyntaxexception") ||
            lower.contains("jsondataexception") ||
            lower.contains("malformedjsonexception") ||
            lower.contains("serializationexception") ||
            lower.contains("jsondecodingexception") ||
            lower.contains("json parse") ||
            Regex("(?m)^\\s*at\\s+[\\w.$]+\\(").containsMatchIn(message) ||
            lower.startsWith("<html") ||
            message.startsWith("{") || message.startsWith("[") ->
            "Произошла техническая ошибка. Попробуйте снова."
        else -> message
    }
}

fun Throwable.toUserFacingError(fallback: String): String =
    userFacingError(message?.takeIf(String::isNotBlank) ?: fallback)
