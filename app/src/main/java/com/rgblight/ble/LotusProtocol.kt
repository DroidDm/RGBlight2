package com.rgblight.ble

import java.util.UUID

object LotusProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    val WRITE_UUID:   UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    val NOTIFY_UUID:  UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")
    val CCCD_UUID:    UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private const val HEAD = 0x7E
    private const val TAIL = 0xEF

    fun color(r: Int, g: Int, b: Int): ByteArray =
        frame(0x05, 0x03, r, g, b, 0x10)

    fun powerOn():  ByteArray = frame(0x04, 0xFF, 0x00, 0x01, 0x02, 0x01)
    fun powerOff(): ByteArray = frame(0x04, 0x00, 0x00, 0x00, 0x02, 0x01)
    fun brightness(level: Int): ByteArray =
        frame(0x01, level.coerceIn(0, 0xFF), 0x01, 0xFF, 0x02, 0x01)

    fun effect(mode: Int, speed: Int): ByteArray =
        frame(0x03, mode and 0xFF, speed.coerceIn(0, 0xFF), 0x00, 0x00, 0x00)

    fun music(on: Boolean): ByteArray =
        frame(0x06, if (on) 0x01 else 0x00, 0x00, 0x00, 0x00, 0x00)

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

    private fun frame(cmd: Int, vararg params: Int): ByteArray {
        val p = IntArray(8) { 0xFF }
        params.forEachIndexed { i, v -> if (i < 8) p[i] = v and 0xFF }
        val len = 1 + params.size
        return byteArrayOf(
            HEAD.toByte(),
            len.toByte(),
            cmd.toByte(),
            p[0].toByte(), p[1].toByte(), p[2].toByte(), p[3].toByte(),
            p[4].toByte(), p[5].toByte(), p[6].toByte(), p[7].toByte(),
            TAIL.toByte()
        )
    }
}