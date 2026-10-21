package ru.tomilo.lib.mobile.data.repo

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopInventoryPayloadTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun readsServerInventoryShape() {
        val data = json.parseToJsonElement("""{"decorations":[{"id":"badge-1","type":"badge","imageUrl":"/badge.png","isEquipped":true}],"ownedBadges":[],"equippedDecorations":{}}""")
        val items = parseShopInventory(data, json)
        assertEquals("badge-1", items.single().stableId())
        assertTrue(items.single().isEquipped == true)
    }

    @Test fun readsLegacyArray() {
        val items = parseShopInventory(json.parseToJsonElement("""[{"_id":"frame-1","type":"frame"}]"""), json)
        assertEquals("frame-1", items.single().stableId())
    }
}
