package com.rgblight.data

import android.content.Context
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
        val SLOTS = stringPreferencesKey("color_slots")
    }

    val savedColors: Flow<List<SavedColor>> = context.savedStore.data.map {
        parse(it[Keys.COLORS]) { o -> SavedColor.fromJson(o) }.sortedByDescending { c -> c.createdAt }
    }
    val savedEffects: Flow<List<SavedEffect>> = context.savedStore.data.map {
        parse(it[Keys.EFFECTS]) { o -> SavedEffect.fromJson(o) }.sortedByDescending { e -> e.createdAt }
    }
    val savedMixes: Flow<List<SavedMix>> = context.savedStore.data.map {
        parse(it[Keys.MIXES]) { o -> SavedMix.fromJson(o) }.sortedByDescending { m -> m.createdAt }
    }

    /** Слоты нижнего ряда на вкладке «Цвет». Всегда минимум 3, максимум 10. */
    val colorSlots: Flow<List<ColorSlot>> = context.savedStore.data.map { p ->
        val raw = parse(p[Keys.SLOTS]) { o -> ColorSlot.fromJson(o) }.sortedBy { it.index }
        val byIndex = raw.associateBy { it.index }
        (0 until 10).map { idx -> byIndex[idx] ?: ColorSlot(idx, null) }
    }

    suspend fun saveColor(c: SavedColor) = mutateColors { list -> list.filter { it.id != c.id } + c }
    suspend fun deleteColor(id: String) = mutateColors { list -> list.filter { it.id != id } }
    suspend fun saveEffect(e: SavedEffect) = mutateEffects { list -> list.filter { it.id != e.id } + e }
    suspend fun deleteEffect(id: String) = mutateEffects { list -> list.filter { it.id != id } }
    suspend fun saveMix(m: SavedMix) = mutateMixes { list -> list.filter { it.id != m.id } + m }
    suspend fun deleteMix(id: String) = mutateMixes { list -> list.filter { it.id != id } }

    suspend fun setColorSlot(slot: ColorSlot) {
        val current = parse(context.savedStore.data.first()[Keys.SLOTS]) { o -> ColorSlot.fromJson(o) }
        val updated = current.filter { it.index != slot.index } + slot
        context.savedStore.edit { p -> p[Keys.SLOTS] = serialize(updated) { it.toJson() } }
    }

    suspend fun clearColorSlot(index: Int) {
        val current = parse(context.savedStore.data.first()[Keys.SLOTS]) { o -> ColorSlot.fromJson(o) }
        val updated = current.filter { it.index != index }
        context.savedStore.edit { p -> p[Keys.SLOTS] = serialize(updated) { it.toJson() } }
    }

    private suspend fun mutateColors(block: (List<SavedColor>) -> List<SavedColor>) {
        val current = parse(context.savedStore.data.first()[Keys.COLORS]) { o -> SavedColor.fromJson(o) }
        val updated = block(current)
        context.savedStore.edit { p -> p[Keys.COLORS] = serialize(updated) { it.toJson() } }
    }
    private suspend fun mutateEffects(block: (List<SavedEffect>) -> List<SavedEffect>) {
        val current = parse(context.savedStore.data.first()[Keys.EFFECTS]) { o -> SavedEffect.fromJson(o) }
        val updated = block(current)
        context.savedStore.edit { p -> p[Keys.EFFECTS] = serialize(updated) { it.toJson() } }
    }
    private suspend fun mutateMixes(block: (List<SavedMix>) -> List<SavedMix>) {
        val current = parse(context.savedStore.data.first()[Keys.MIXES]) { o -> SavedMix.fromJson(o) }
        val updated = block(current)
        context.savedStore.edit { p -> p[Keys.MIXES] = serialize(updated) { it.toJson() } }
    }

    private fun <T> parse(s: String?, mapper: (JSONObject) -> T): List<T> {
        if (s.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(s)
            (0 until arr.length()).mapNotNull { i -> runCatching { mapper(arr.getJSONObject(i)) }.getOrNull() }
        }.getOrDefault(emptyList())
    }
    private fun <T> serialize(list: List<T>, mapper: (T) -> JSONObject): String {
        val arr = JSONArray()
        list.forEach { arr.put(mapper(it)) }
        return arr.toString()
    }
}