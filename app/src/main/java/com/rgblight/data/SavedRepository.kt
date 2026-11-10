package com.rgblight.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.savedStore by preferencesDataStore(name = "rgb_light_saved")

class SavedRepository(private val context: Context) {

    private object Keys {
        val COLORS = stringPreferencesKey("saved_colors")
        val EFFECTS = stringPreferencesKey("saved_effects")
        val MIXES = stringPreferencesKey("saved_mixes")
        val ACTIVE_MIX = stringPreferencesKey("active_mix")
    }

    val savedColors: Flow<List<SavedColor>> = context.savedStore.data.map { p ->
        parseColors(p[Keys.COLORS]).sortedByDescending { it.createdAt }
    }
    val savedEffects: Flow<List<SavedEffect>> = context.savedStore.data.map { p ->
        parseEffects(p[Keys.EFFECTS]).sortedByDescending { it.createdAt }
    }
    val savedMixes: Flow<List<SavedMix>> = context.savedStore.data.map { p ->
        parseMixes(p[Keys.MIXES]).sortedByDescending { it.createdAt }
    }
    val activeMix: Flow<ActiveMixState?> = context.savedStore.data.map { p ->
        p[Keys.ACTIVE_MIX]?.let { runCatching { ActiveMixState.fromJson(JSONObject(it)) }.getOrNull() }
    }

    suspend fun saveColor(color: SavedColor) = mutateColors { list ->
        list.filter { it.id != color.id } + color
    }

    suspend fun deleteColor(id: String) = mutateColors { list -> list.filter { it.id != id } }

    suspend fun saveEffect(effect: SavedEffect) = mutateEffects { list ->
        list.filter { it.id != effect.id } + effect
    }

    suspend fun deleteEffect(id: String) = mutateEffects { list -> list.filter { it.id != id } }

    suspend fun saveMix(mix: SavedMix) = mutateMixes { list ->
        list.filter { it.id != mix.id } + mix
    }

    suspend fun deleteMix(id: String) = mutateMixes { list -> list.filter { it.id != id } }

    suspend fun setActiveMix(state: ActiveMixState?) {
        context.savedStore.edit { p ->
            if (state == null) p.remove(Keys.ACTIVE_MIX)
            else p[Keys.ACTIVE_MIX] = state.toJson().toString()
        }
    }

    private suspend fun mutateColors(block: (List<SavedColor>) -> List<SavedColor>) {
        val current = parseColors(context.savedStore.data.first()[Keys.COLORS])
        val updated = block(current)
        context.savedStore.edit { p ->
            p[Keys.COLORS] = colorsToJson(updated).toString()
        }
    }
    private suspend fun mutateEffects(block: (List<SavedEffect>) -> List<SavedEffect>) {
        val current = parseEffects(context.savedStore.data.first()[Keys.EFFECTS])
        val updated = block(current)
        context.savedStore.edit { p ->
            p[Keys.EFFECTS] = effectsToJson(updated).toString()
        }
    }
    private suspend fun mutateMixes(block: (List<SavedMix>) -> List<SavedMix>) {
        val current = parseMixes(context.savedStore.data.first()[Keys.MIXES])
        val updated = block(current)
        context.savedStore.edit { p ->
            p[Keys.MIXES] = mixesToJson(updated).toString()
        }
    }

    private fun parseColors(s: String?): List<SavedColor> = parseArray(s) { SavedColor.fromJson(it) }
    private fun parseEffects(s: String?): List<SavedEffect> = parseArray(s) { SavedEffect.fromJson(it) }
    private fun parseMixes(s: String?): List<SavedMix> = parseArray(s) { SavedMix.fromJson(it) }

    private fun <T> parseArray(s: String?, mapper: (JSONObject) -> T): List<T> {
        if (s.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(s)
            (0 until arr.length()).mapNotNull { i ->
                runCatching { mapper(arr.getJSONObject(i)) }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    private fun colorsToJson(list: List<SavedColor>): JSONArray =
        JSONArray().apply { list.forEach { put(it.toJson()) } }
    private fun effectsToJson(list: List<SavedEffect>): JSONArray =
        JSONArray().apply { list.forEach { put(it.toJson()) } }
    private fun mixesToJson(list: List<SavedMix>): JSONArray =
        JSONArray().apply { list.forEach { put(it.toJson()) } }
}