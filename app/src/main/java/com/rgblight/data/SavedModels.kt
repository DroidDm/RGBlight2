package com.rgblight.data

import org.json.JSONArray
import org.json.JSONObject

enum class EffectMode { FADE, FADE_PAUSE, FADE_CUSTOM, FADE_PULSE, PULSE, JUMP, NEW_YEAR }
enum class StepKind { COLOR, EFFECT }

data class SavedColor(
    val id: String,
    val name: String,
    val r: Int, val g: Int, val b: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("r", r); put("g", g); put("b", b); put("createdAt", createdAt)
    }
    companion object {
        fun fromJson(o: JSONObject) = SavedColor(
            o.optString("id"), o.optString("name"),
            o.optInt("r", 255), o.optInt("g", 255), o.optInt("b", 255),
            o.optLong("createdAt", 0)
        )
    }
}

/** Сохранённый эффект: либо протокольный (protocolId != null), либо кастомный. */
data class SavedEffect(
    val id: String,
    val name: String,
    val protocolId: Int?,      // 0..7 — протокольный id, или null — кастомный
    val mode: EffectMode,      // для кастомного
    val colors: List<Int>,     // для кастомного
    val speed: Int,
    val brightness: Int,
    val holdMs: Int,
    val rampMs: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("protocolId", protocolId ?: -1)
        put("mode", mode.name)
        put("colors", JSONArray(colors))
        put("speed", speed); put("brightness", brightness)
        put("holdMs", holdMs); put("rampMs", rampMs)
        put("createdAt", createdAt)
    }
    companion object {
        fun fromJson(o: JSONObject): SavedEffect {
            val arr = o.optJSONArray("colors") ?: JSONArray()
            val list = mutableListOf<Int>()
            for (i in 0 until arr.length()) list.add(arr.getInt(i))
            return SavedEffect(
                o.optString("id"), o.optString("name"),
                o.optInt("protocolId", -1).takeIf { it >= 0 },
                runCatching { EffectMode.valueOf(o.optString("mode", "FADE")) }.getOrDefault(EffectMode.FADE),
                list,
                o.optInt("speed", 128), o.optInt("brightness", 100),
                o.optInt("holdMs", 1000), o.optInt("rampMs", 500),
                o.optLong("createdAt", 0)
            )
        }
    }
}

/** Шаг микса. Цвета задаются как полные ARGB (0xFFRRGGBB), чтобы не было прозрачности. */
data class MixStep(
    val kind: StepKind,
    val color: Int = 0xFFFFFFFF.toInt(),
    val effectMode: EffectMode = EffectMode.FADE,
    val colors: List<Int> = listOf(
        0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt()
    ),
    val holdMs: Int = 1000,
    val rampMs: Int = 500
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("kind", kind.name)
        put("color", color)
        put("effectMode", effectMode.name)
        put("colors", JSONArray(colors))
        put("holdMs", holdMs); put("rampMs", rampMs)
    }
    companion object {
        fun fromJson(o: JSONObject): MixStep {
            val arr = o.optJSONArray("colors") ?: JSONArray()
            val list = mutableListOf<Int>()
            for (i in 0 until arr.length()) list.add(arr.getInt(i))
            return MixStep(
                runCatching { StepKind.valueOf(o.optString("kind", "COLOR")) }.getOrDefault(StepKind.COLOR),
                o.optInt("color", 0xFFFFFFFF.toInt()),
                runCatching { EffectMode.valueOf(o.optString("effectMode", "FADE")) }.getOrDefault(EffectMode.FADE),
                list,
                o.optInt("holdMs", 1000), o.optInt("rampMs", 500)
            )
        }
    }
}

data class SavedMix(
    val id: String,
    val name: String,
    val steps: List<MixStep>,
    val speed: Int,
    val brightness: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("steps", JSONArray().apply { steps.forEach { put(it.toJson()) } })
        put("speed", speed); put("brightness", brightness)
        put("createdAt", createdAt)
    }
    companion object {
        fun fromJson(o: JSONObject): SavedMix {
            val arr = o.optJSONArray("steps") ?: JSONArray()
            val list = mutableListOf<MixStep>()
            for (i in 0 until arr.length()) runCatching { list.add(MixStep.fromJson(arr.getJSONObject(i))) }
            return SavedMix(
                o.optString("id"), o.optString("name"), list,
                o.optInt("speed", 128), o.optInt("brightness", 100),
                o.optLong("createdAt", 0)
            )
        }
    }
}

/**
 * Слот сохранённого цвета (нижний ряд на вкладке «Цвет»).
 * ВАЖНО: сериализуем через явный флаг hasRgb, потому что Int-цвет
 * вида 0xFFFFFFFF (белый) даёт -1, что коллидирует с прежним sentinel -1.
 */
data class ColorSlot(
    val index: Int,
    val rgb: Int? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("index", index)
        put("hasRgb", rgb != null)
        if (rgb != null) put("rgb", rgb)
    }
    companion object {
        fun fromJson(o: JSONObject): ColorSlot {
            val has = o.optBoolean("hasRgb", false)
            return ColorSlot(
                index = o.optInt("index", 0),
                rgb = if (has) o.optInt("rgb", 0) else null
            )
        }
    }
}