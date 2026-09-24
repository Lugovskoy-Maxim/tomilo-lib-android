package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.cardWantDataStore by preferencesDataStore("tomilo_card_wants")

/** Local wishlist used to filter public card trades, matching the website's browser storage. */
class CardWantPrefs(private val context: Context) {
    private val cardIdsKey = stringSetPreferencesKey("card_ids")

    suspend fun cardIds(): Set<String> = try {
        context.cardWantDataStore.data.first()[cardIdsKey].orEmpty()
    } catch (_: IOException) {
        emptySet()
    }

    suspend fun saveCardIds(cardIds: Set<String>) {
        try {
            context.cardWantDataStore.edit { preferences ->
                preferences[cardIdsKey] = cardIds.filter(String::isNotBlank).toSet()
            }
        } catch (_: IOException) {
            // Keep the catalog interactive when optional local wishlist storage is unavailable.
        }
    }
}
