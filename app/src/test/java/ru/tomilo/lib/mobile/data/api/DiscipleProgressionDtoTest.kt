package ru.tomilo.lib.mobile.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscipleProgressionDtoTest {
    @Test
    fun decodesProgressionOverviewFromServerShape() {
        val payload = """
            {
              "success": true,
              "data": {
                "cultivationStages": [{
                  "stageKey": "qi_condensation",
                  "order": 1,
                  "displayName": "Конденсация ци",
                  "breakthroughFromPrevious": {
                    "minDiscipleLevel": 5,
                    "minCultivationExp": 120,
                    "itemCosts": [{"itemId": "breakthrough_pill", "count": 1}]
                  },
                  "slotUnlocksAdded": []
                }],
                "buildCardDefinitions": [],
                "buildCardInstances": [],
                "disciples": [{
                  "characterId": "internal:1",
                  "isInternal": true,
                  "internalSlot": 1,
                  "displayName": "Ученик секты",
                  "level": 6,
                  "inWarehouse": false,
                  "cultivationStageKey": "qi_condensation",
                  "cultivationExp": 150,
                  "starLevel": 2,
                  "path": "fire",
                  "meditation": {"startedAt": null, "lastClaimAt": null},
                  "resolvedStats": {"attack": 1}
                }]
              }
            }
        """.trimIndent()

        val response = NetworkModule.json.decodeFromString<ApiResponse<DiscipleProgressionOverviewDto>>(payload)
        val stage = response.data!!.cultivationStages.single()
        val disciple = response.data!!.disciples.single()

        assertTrue(response.success)
        assertEquals("qi_condensation", stage.stageKey)
        assertEquals(1, stage.breakthroughFromPrevious?.itemCosts?.single()?.count)
        assertEquals("Ученик секты", disciple.displayName())
        assertEquals("fire", disciple.path)
        assertEquals(150, disciple.cultivationExp)
    }

    @Test
    fun decodesStopResponseWithNestedClaim() {
        val payload = """
            {
              "success": true,
              "data": {
                "characterId": "internal:1",
                "ended": true,
                "claim": {
                  "characterId": "internal:1",
                  "ticks": 3,
                  "characterExpGained": 15,
                  "cultivationExpGained": 30,
                  "events": []
                }
              }
            }
        """.trimIndent()

        val response = NetworkModule.json.decodeFromString<ApiResponse<CultivationMeditationResultDto>>(payload)

        assertTrue(response.success)
        assertEquals(true, response.data?.ended)
    }
}
