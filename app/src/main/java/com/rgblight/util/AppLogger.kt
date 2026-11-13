package com.rgblight.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val timeMs: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    fun formatted(): String {
        val df = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        return "[${df.format(Date(timeMs))}] [${level.name.padEnd(5)}] [$tag] $message"
    }
}

object AppLogger {
    private const val MAX = 5000
    private const val ADB = "RGBLight"

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun d(tag: String, msg: String) = add(LogLevel.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = add(LogLevel.INFO, tag, msg)
    fun w(tag: String, msg: String) = add(LogLevel.WARN, tag, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) {
        val full = if (t != null) "$msg: ${t.message}\n${t.stackTraceToString()}" else msg
        add(LogLevel.ERROR, tag, full)
    }
    fun bytes(tag: String, prefix: String, data: ByteArray) {
        add(LogLevel.DEBUG, tag, "$prefix: ${data.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }}")
    }
    fun clear() { _entries.value = emptyList() }
    fun export(): String {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val head = "RGB light — журнал\nСформирован: ${df.format(Date())}\nЗаписей: ${_entries.value.size}\n\n"
        return head + _entries.value.joinToString("\n") { it.formatted() }
    }
    private fun add(level: LogLevel, tag: String, msg: String) {
        val e = LogEntry(System.currentTimeMillis(), level, tag, msg)
        when (level) {
            LogLevel.DEBUG -> Log.d(ADB, "[$tag] $msg")
            LogLevel.INFO  -> Log.i(ADB, "[$tag] $msg")
            LogLevel.WARN  -> Log.w(ADB, "[$tag] $msg")
            LogLevel.ERROR -> Log.e(ADB, "[$tag] $msg")
        }
        _entries.update { it.plus(e).let { l -> if (l.size > MAX) l.takeLast(MAX) else l } }
    }
}