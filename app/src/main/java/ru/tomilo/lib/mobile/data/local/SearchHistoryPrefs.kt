package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.searchHistoryStore by preferencesDataStore("tomilo_search_history")

class SearchHistoryPrefs(private val context: Context) {
    private val key = stringPreferencesKey("queries")

    val queriesFlow: Flow<List<String>> = context.searchHistoryStore.data.map { prefs ->
        parse(prefs[key])
    }

    suspend fun remember(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        context.searchHistoryStore.edit { prefs ->
            val next = listOf(clean) + parse(prefs[key]).filterNot { it.equals(clean, ignoreCase = true) }
            prefs[key] = next.take(8).joinToString("\u001f")
        }
    }

    suspend fun remove(query: String) {
        context.searchHistoryStore.edit { prefs ->
            prefs[key] = parse(prefs[key]).filterNot { it.equals(query, ignoreCase = true) }.joinToString("\u001f")
        }
    }

    suspend fun clear() {
        context.searchHistoryStore.edit { it.remove(key) }
    }

    private fun parse(raw: String?): List<String> =
        raw?.split('\u001f')?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
}
