package com.rgblight.ble

import java.util.UUID

object LotusProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    val WRITE_UUID:   UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    val NOTIFY_UUID:  UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")
    val CCCD_UUID:    UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private const val HEAD = 0x7E
    private const val TAIL = 0xEF

    // Кадр: 7E [LEN] [CMD] [P1..Pn] [EF], LEN = 2 + n, всего 4 + n байт

    /** Цвет: 7E 07 05 03 RR GG BB 10 EF — 9 байт */
    fun color(r: Int, g: Int, b: Int): ByteArray =
        frame(0x05, 0x03, r, g, b, 0x10)

    /** Включение: 7E 07 04 FF 00 01 02 01 EF — 9 байт */
    fun powerOn():  ByteArray = frame(0x04, 0xFF, 0x00, 0x01, 0x02, 0x01)

    /** Выключение: 7E 07 04 00 00 00 02 01 EF — 9 байт */
    fun powerOff(): ByteArray = frame(0x04, 0x00, 0x00, 0x00, 0x02, 0x01)

    /** Яркость: 7E 04 01 XX 01 EF — 6 байт */
    fun brightness(level: Int): ByteArray =
        frame(0x01, level.coerceIn(0, 0xFF), 0x01)

    /** Эффект: 7E 07 03 MODE SPEED 00 00 00 EF — 9 байт */
    fun effect(mode: Int, speed: Int): ByteArray =
        frame(0x03, mode and 0xFF, speed.coerceIn(0, 0xFF), 0x00, 0x00, 0x00)

    /** Music: 7E 07 06 01/00 00 00 00 00 EF — 9 байт */
    fun music(on: Boolean): ByteArray =
        frame(0x06, if (on) 0x01 else 0x00, 0x00, 0x00, 0x00)

    /** TimeSync (только для таймеров, может игнорироваться ревизией) */
    fun timeSync(): ByteArray {
        val cal = java.util.Calendar.getInstance()
        val hh = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val mm = cal.get(java.util.Calendar.MINUTE)
        val ss = cal.get(java.util.Calendar.SECOND)
        val yy = cal.get(java.util.Calendar.YEAR) - 2000
        val mo = cal.get(java.util.Calendar.MONTH) + 1
        val dd = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
        return frame(0x83, hh, mm, ss, yy, mo, dd, dow)
    }

    /** Правильный формат без хвостовых 0xFF-заполнителей. */
    private fun frame(cmd: Int, vararg params: Int): ByteArray {
        val n = params.size
        val len = 2 + n              // 1 (LEN сам) + 1 (CMD) + n (params)
        val out = ByteArray(4 + n)   // HEAD + LEN + CMD + n + TAIL
        out[0] = HEAD.toByte()
        out[1] = len.toByte()
        out[2] = cmd.toByte()
        params.forEachIndexed { i, v -> out[3 + i] = (v and 0xFF).toByte() }
        out[out.size - 1] = TAIL.toByte()
        return out
    }
}