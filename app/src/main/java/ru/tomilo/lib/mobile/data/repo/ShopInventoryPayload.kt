package ru.tomilo.lib.mobile.data.repo

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import ru.tomilo.lib.mobile.data.api.ShopDecorationDto

internal fun parseShopInventory(data: JsonElement?, json: Json): List<ShopDecorationDto> {
    val payload = when (data) {
        is JsonArray -> data
        is JsonObject -> data["decorations"] as? JsonArray
        else -> null
    } ?: error("Некорректный ответ инвентаря")
    return payload.map { json.decodeFromJsonElement<ShopDecorationDto>(it) }
}
