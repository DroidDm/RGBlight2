package com.rgblight.ble

import java.util.UUID

object LotusProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    val WRITE_UUID:   UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    val NOTIFY_UUID:  UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")
    val CCCD_UUID:    UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private const val HEAD = 0x7E
    private const val TAIL = 0xEF

    private val ELK_KEY = intArrayOf(0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35)

    // 7E [LEN] [CMD] [P1..Pn] [EF], LEN = 2 + n

    fun color(r: Int, g: Int, b: Int): ByteArray = frame(0x05, 0x03, r, g, b, 0x10)

    fun powerOn():  ByteArray = frame(0x04, 0xFF, 0x00, 0x01, 0x02, 0x01)
    fun powerOff(): ByteArray = frame(0x04, 0x00, 0x00, 0x00, 0x02, 0x01)

    fun brightness(level: Int): ByteArray = frame(0x01, level.coerceIn(0, 0xFF), 0x01)

    fun effect(mode: Int, speed: Int): ByteArray =
        frame(0x03, mode and 0xFF, speed.coerceIn(0, 0xFF), 0x00, 0x00, 0x00)

    fun music(on: Boolean): ByteArray =
        frame(0x06, if (on) 0x01 else 0x00, 0x00, 0x00, 0x00)

    fun timeSync(): ByteArray {
        val cal = java.util.Calendar.getInstance()
        return frame(
            0x83,
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE),
            cal.get(java.util.Calendar.SECOND),
            cal.get(java.util.Calendar.YEAR) - 2000,
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
            cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
        )
    }

    fun encryptElk(data: ByteArray): ByteArray {
        val out = data.copyOf()
        val end = minOf(7, out.size - 2)
        for (i in 1..end) out[i] = (out[i].toInt() xor ELK_KEY[i - 1]).toByte()
        return out
    }

    private fun frame(cmd: Int, vararg params: Int): ByteArray {
        val n = params.size
        val out = ByteArray(4 + n)
        out[0] = HEAD.toByte()
        out[1] = (2 + n).toByte()
        out[2] = cmd.toByte()
        params.forEachIndexed { i, v -> out[3 + i] = (v and 0xFF).toByte() }
        out[out.size - 1] = TAIL.toByte()
        return out
    }
}