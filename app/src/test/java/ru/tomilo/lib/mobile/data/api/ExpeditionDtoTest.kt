package ru.tomilo.lib.mobile.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpeditionDtoTest {
    @Test
    fun decodesServerStatusAndRewardResult() {
        val payload = """
            {
              "success": true,
              "data": {
                "canStart": false,
                "actionBattleInProgress": false,
                "inProgress": false,
                "completesAt": null,
                "nextExpeditionAt": "2026-10-21T12:00:00.000Z",
                "costs": {"easy": 0, "normal": 25, "hard": 60},
                "lastResult": {
                  "at": "2026-10-20T12:00:00.000Z",
                  "difficulty": "normal",
                  "success": true,
                  "coinsGained": 40,
                  "expGained": 25,
                  "itemsGained": [{"itemId": "spirit_grass", "count": 2, "name": "Духовная трава"}],
                  "log": ["Ученики вернулись"],
                  "ambush": {"happened": true, "preventedByTalisman": true},
                  "runId": "server-operation-id"
                },
                "hasDisciples": true,
                "balance": 250,
                "ambushRiskPercent": 12,
                "pendingRealmCategoryId": "spirit_sanctuary",
                "arenaStatBonusBattlesRemaining": 0,
                "realmCategories": [{"id": "spirit_sanctuary", "label": "Святилище духа"}]
              }
            }
        """.trimIndent()

        val response = NetworkModule.json.decodeFromString<ApiResponse<ExpeditionStatusDto>>(payload)
        val status = response.data!!
        val result = status.lastResult!!

        assertTrue(response.success)
        assertEquals(25, status.costs.normal)
        assertEquals("spirit_sanctuary", status.pendingRealmCategoryId)
        assertEquals(25, result.expGained)
        assertEquals("spirit_grass", result.itemsGained.single().itemId)
        assertTrue(result.ambush?.preventedByTalisman == true)
    }

    @Test
    fun decodesRealmCatalogAndStartAcknowledgement() {
        val catalog = NetworkModule.json.decodeFromString<ApiResponse<RealmMapsCatalogDto>>(
            """{"success":true,"data":{"categories":[{"id":"spirit_sanctuary","label":"Святилище духа","description":"Бонус культивации","entryItemId":"realm_key_spirit"}],"packages":[]}}""",
        )
        val started = NetworkModule.json.decodeFromString<ApiResponse<StartExpeditionResultDto>>(
            """{"success":true,"data":{"started":true,"completesAt":"2026-10-20T12:02:00.000Z","balance":250}}""",
        )

        assertEquals("realm_key_spirit", catalog.data!!.categories.single().entryItemId)
        assertTrue(started.data!!.started)
        assertEquals(250, started.data!!.balance)
    }
}
