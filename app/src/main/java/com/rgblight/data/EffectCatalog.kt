package com.rgblight.data

/** 7 кастомных режимов, генерируемых приложением. */
object EffectCatalog {

    data class Meta(
        val mode: EffectMode,
        val label: String,
        val needColors: Boolean,
        val needOrder: Boolean
    )

    val modes: List<Meta> = listOf(
        Meta(EffectMode.FADE, "Плавный переход", false, false),
        Meta(EffectMode.FADE_PAUSE, "Плавный с паузой", false, false),
        Meta(EffectMode.FADE_CUSTOM, "Плавный · свои цвета", true, true),
        Meta(EffectMode.FADE_PULSE, "Плавный · пульс 50%", true, true),
        Meta(EffectMode.PULSE, "Пульсация одного цвета", true, false),
        Meta(EffectMode.JUMP, "Резкая смена", true, true),
        Meta(EffectMode.NEW_YEAR, "Новый год (гирлянда)", false, false)
    )

    /** 8 протокольных эффектов, работающих на обычной RGB. */
    val protocolEffects: List<Pair<Int, String>> = listOf(
        0x00 to "Статика",
        0x01 to "Прыжок",
        0x02 to "Плавный переход",
        0x03 to "Вспышка",
        0x04 to "Дыхание",
        0x05 to "Радуга",
        0x06 to "Музыка",
        0x07 to "Волна"
    )
}