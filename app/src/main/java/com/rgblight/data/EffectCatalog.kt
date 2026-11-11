package com.rgblight.data

/**
 * Каталог встроенных эффектов протокола Lotus/ELK.
 * ID соответствуют команде effect(mode, speed) в LotusProtocol.
 * В открытых источниках подтверждены ~50–80 ID; производители
 * варьируют прошивки, поэтому часть ID может игнорироваться
 * конкретным контроллером — это нормально.
 *
 * Полный каталог 213 анимаций можно расширить, добавив строки
 * в список effects — остальная логика не изменится.
 */
object EffectCatalog {

    enum class Category(val label: String) {
        STATIC("Статика"),
        DYNAMIC("Динамика"),
        STROBE("Стробоскоп"),
        RAINBOW("Радуга"),
        WAVE("Волна"),
        MUSIC("Музыка"),
        OTHER("Другое")
    }

    data class Effect(val id: Int, val name: String, val cat: Category)

    val effects: List<Effect> = listOf(
        Effect(0x00, "Статика", Category.STATIC),
        Effect(0x01, "Прыжок цветов", Category.DYNAMIC),
        Effect(0x02, "Плавный переход", Category.DYNAMIC),
        Effect(0x03, "Вспышка", Category.STROBE),
        Effect(0x04, "Дыхание", Category.DYNAMIC),
        Effect(0x05, "Радуга", Category.RAINBOW),
        Effect(0x06, "Режим микрофона", Category.MUSIC),
        Effect(0x07, "Волна", Category.WAVE),
        Effect(0x08, "Стробоскоп", Category.STROBE),
        Effect(0x09, "Радужный поток", Category.RAINBOW),
        Effect(0x0A, "Пульсация", Category.DYNAMIC),
        Effect(0x0B, "Мерцание", Category.STROBE),
        Effect(0x0C, "Бегущий огонь", Category.WAVE),
        Effect(0x0D, "Свеча", Category.DYNAMIC),
        Effect(0x0E, "Рассвет", Category.DYNAMIC),
        Effect(0x0F, "Закат", Category.DYNAMIC),
        Effect(0x10, "Гроза", Category.STROBE),
        Effect(0x11, "Волна цветов", Category.WAVE),
        Effect(0x12, "Круговорот", Category.RAINBOW),
        Effect(0x13, "Плавный градиент", Category.DYNAMIC),
        Effect(0x14, "Мигание", Category.STROBE),
        Effect(0x15, "Сердцебиение", Category.DYNAMIC),
        Effect(0x16, "Романтика", Category.DYNAMIC),
        Effect(0x17, "Звёздная ночь", Category.STROBE),
        Effect(0x18, "Пожар", Category.DYNAMIC),
        Effect(0x19, "Океан", Category.WAVE),
        Effect(0x1A, "Лес", Category.DYNAMIC),
        Effect(0x1B, "Вечеринка", Category.DYNAMIC),
        Effect(0x1C, "Дискотека", Category.STROBE),
        Effect(0x1D, "Праздник", Category.STROBE),
        Effect(0x1E, "Новый год", Category.STROBE),
        Effect(0x1F, "Хэллоуин", Category.STROBE),
        Effect(0x20, "Снегопад", Category.DYNAMIC),
        Effect(0x21, "Дыхание радуги", Category.RAINBOW),
        Effect(0x22, "Строб радуги", Category.STROBE),
        Effect(0x23, "Прыжок радуги", Category.RAINBOW),
        Effect(0x24, "Волна цвета", Category.WAVE),
        Effect(0x25, "Плазма", Category.RAINBOW),
        Effect(0x26, "Метель", Category.STROBE),
        Effect(0x27, "Лава", Category.DYNAMIC),
        Effect(0x28, "Неон", Category.DYNAMIC),
        Effect(0x29, "Спектр", Category.RAINBOW),
        Effect(0x2A, "Акварель", Category.DYNAMIC),
        Effect(0x2B, "Комета", Category.WAVE),
        Effect(0x2C, "Метеор", Category.WAVE),
        Effect(0x2D, "Водоворот", Category.WAVE),
        Effect(0x2E, "Спираль", Category.WAVE),
        Effect(0x2F, "Сетка", Category.OTHER),
        Effect(0x30, "Тай-дай", Category.RAINBOW),
        Effect(0x31, "Пиксели", Category.OTHER),
        Effect(0x32, "Каскад", Category.WAVE),
        Effect(0x33, "Лабиринт", Category.OTHER),
        Effect(0x34, "Пилинг", Category.STROBE),
        Effect(0x35, "Растворение", Category.DYNAMIC),
        Effect(0x36, "Глитч", Category.STROBE),
        Effect(0x37, "Шум", Category.OTHER),
        Effect(0x38, "Дрожание", Category.OTHER),
        Effect(0x39, "Тени", Category.DYNAMIC),
        Effect(0x3A, "Мираж", Category.DYNAMIC),
        Effect(0x3B, "Феникс", Category.RAINBOW),
        Effect(0x3C, "Змейка", Category.WAVE),
        Effect(0x3D, "Кольцо", Category.WAVE),
        Effect(0x3E, "Кубик Рубика", Category.OTHER),
        Effect(0x3F, "Матрица", Category.OTHER)
    )

    val categories: List<Category> get() = Category.values().toList()

    fun byCategory(cat: Category?): List<Effect> =
        if (cat == null) effects else effects.filter { it.cat == cat }

    fun byId(id: Int): Effect? = effects.firstOrNull { it.id == id }

    fun search(query: String): List<Effect> {
        if (query.isBlank()) return effects
        val q = query.lowercase()
        return effects.filter { it.name.lowercase().contains(q) }
    }
}