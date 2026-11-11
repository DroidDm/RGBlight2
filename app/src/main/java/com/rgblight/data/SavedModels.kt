package com.rgblight.data

import org.json.JSONArray
import org.json.JSONObject

enum class MixMode(val label: String) {
    FADE("Fade"), JUMP("Jump"), BREATHE("Breathe"), STROBE("Strobe"), WAVE("Wave")
}

data class SavedColor(
    val id: String,
    val name: String,
    val r: Int, val g: Int, val b: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("r", r); put("g", g); put("b", b)
        put("createdAt", createdAt)
    }
    companion object {
        fun fromJson(o: JSONObject): SavedColor = SavedColor(
            id = o.optString("id"),
            name = o.optString("name"),
            r = o.optInt("r", 255), g = o.optInt("g", 255), b = o.optInt("b", 255),
            createdAt = o.optLong("createdAt", 0)
        )
    }
}

data class SavedEffect(
    val id: String,
    val name: String,
    val effectId: Int,
    val speed: Int,
    val brightness: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("effectId", effectId); put("speed", speed); put("brightness", brightness)
        put("createdAt", createdAt)
    }
    companion object {
        fun fromJson(o: JSONObject): SavedEffect = SavedEffect(
            id = o.optString("id"), name = o.optString("name"),
            effectId = o.optInt("effectId", 0),
            speed = o.optInt("speed", 128), brightness = o.optInt("brightness", 100),
            createdAt = o.optLong("createdAt", 0)
        )
    }
}

data class SavedMix(
    val id: String,
    val name: String,
    val colors: List<Int>,
    val mode: MixMode,
    val speed: Int,
    val brightness: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("colors", JSONArray(colors))
        put("mode", mode.name); put("speed", speed); put("brightness", brightness)
        put("createdAt", createdAt)
    }
    companion object {
        fun fromJson(o: JSONObject): SavedMix {
            val arr = o.optJSONArray("colors") ?: JSONArray()
            val list = mutableListOf<Int>()
            for (i in 0 until arr.length()) list.add(arr.getInt(i))
            return SavedMix(
                id = o.optString("id"), name = o.optString("name"),
                colors = list,
                mode = runCatching { MixMode.valueOf(o.optString("mode", "FADE")) }.getOrDefault(MixMode.FADE),
                speed = o.optInt("speed", 128), brightness = o.optInt("brightness", 100),
                createdAt = o.optLong("createdAt", 0)
            )
        }
    }
}

/** Активное (несохранённое) состояние микса — для восстановления между запусками */
data class ActiveMixState(
    val colors: List<Int> = emptyList(),
    val mode: MixMode = MixMode.FADE,
    val speed: Int = 128,
    val brightness: Int = 100
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("colors", JSONArray(colors))
        put("mode", mode.name); put("speed", speed); put("brightness", brightness)
    }
    companion object {
        fun fromJson(o: JSONObject): ActiveMixState {
            val arr = o.optJSONArray("colors") ?: JSONArray()
            val list = mutableListOf<Int>()
            for (i in 0 until arr.length()) list.add(arr.getInt(i))
            return ActiveMixState(
                colors = list,
                mode = runCatching { MixMode.valueOf(o.optString("mode", "FADE")) }.getOrDefault(MixMode.FADE),
                speed = o.optInt("speed", 128), brightness = o.optInt("brightness", 100)
            )
        }
    }
}