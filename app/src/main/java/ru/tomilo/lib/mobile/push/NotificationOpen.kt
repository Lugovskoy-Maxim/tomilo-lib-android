package ru.tomilo.lib.mobile.push

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import ru.tomilo.lib.mobile.data.api.NotificationDto

data class NotificationOpen(
    val openList: Boolean = true,
    val titleId: String? = null,
    val chapterId: String? = null,
    val linkUrl: String? = null,
    val conversationId: String? = null,
    val conversationTitle: String? = null,
)

fun NotificationDto.toOpenRequest(): NotificationOpen = NotificationOpen(
    openList = true,
    titleId = resolvedTitleId().ifBlank { null },
    chapterId = resolvedChapterId().ifBlank { null },
    linkUrl = linkUrl?.takeIf { it.isNotBlank() },
)

fun NotificationDto.resolvedTitleId(): String {
    entityId(titleId).ifBlank { null }?.let { return it }
    val meta = metadata as? JsonObject ?: return ""
    entityId(meta["titleId"]).ifBlank { null }?.let { return it }
    val entityType = meta["entityType"]?.jsonPrimitive?.contentOrNull.orEmpty()
    if (entityType == "title") {
        entityId(meta["entityId"]).ifBlank { null }?.let { return it }
    }
    return ""
}

fun NotificationDto.resolvedChapterId(): String {
    entityId(chapterId).ifBlank { null }?.let { return it }
    val meta = metadata as? JsonObject ?: return ""
    entityId(meta["primaryChapterId"]).ifBlank { null }?.let { return it }
    entityId(meta["latestChapterId"]).ifBlank { null }?.let { return it }
    entityId(meta["firstChapterId"]).ifBlank { null }?.let { return it }
    entityId(meta["chapterId"]).ifBlank { null }?.let { return it }
    val chapters = meta["chapters"] as? JsonArray
    chapters?.firstOrNull()?.let { first ->
        val obj = first as? JsonObject
        val id = entityId(obj?.get("chapterId"))
            .ifBlank { entityId(obj?.get("_id")) }
            .ifBlank { entityId(obj?.get("id")) }
            .ifBlank { entityId(first) }
        if (id.isNotBlank()) return id
    }
    val entityType = meta["entityType"]?.jsonPrimitive?.contentOrNull.orEmpty()
    if (entityType == "chapter") {
        entityId(meta["entityId"]).ifBlank { null }?.let { return it }
    }
    return ""
}

fun entityId(value: JsonElement?): String {
    if (value == null || value is JsonNull) return ""
    return when (value) {
        is JsonPrimitive -> value.contentOrNull.orEmpty().trim()
        is JsonObject -> {
            value["_id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                .ifBlank { value["id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty() }
                .ifBlank { value["slug"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty() }
        }
        else -> ""
    }
}
