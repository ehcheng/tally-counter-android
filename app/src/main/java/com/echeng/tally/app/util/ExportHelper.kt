package com.echeng.tally.app.util

import com.echeng.tally.app.data.entity.Counter
import com.echeng.tally.app.data.entity.CounterEntry
import org.json.JSONArray
import org.json.JSONObject

object ExportHelper {
    /** Current backup format version. */
    private const val FORMAT_VERSION = 2

    fun toCsv(counters: List<Counter>, entries: List<CounterEntry>): String {
        val sb = StringBuilder()
        sb.appendLine("counter_name,date,count")
        val counterMap = counters.associateBy { it.id }
        entries.sortedWith(compareBy({ it.counterId }, { it.date })).forEach { entry ->
            val name = counterMap[entry.counterId]?.name ?: "Unknown"
            sb.appendLine("\"${name.replace("\"", "\"\"")}\",${entry.date},${entry.count}")
        }
        return sb.toString()
    }

    /**
     * Full JSON backup including counters, entries, and app settings.
     * V2 adds: startDate on counters, settings object.
     */
    fun toJson(
        counters: List<Counter>,
        entries: List<CounterEntry>,
        settings: AppSettings? = null
    ): String {
        val root = JSONObject()
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val countersArray = JSONArray()
        counters.forEach { c ->
            countersArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("icon", c.icon)
                put("colorHex", c.colorHex)
                put("stepValue", c.stepValue)
                put("startingCount", c.startingCount)
                if (c.startDate != null) put("startDate", c.startDate)
                put("sortOrder", c.sortOrder)
                put("createdAt", c.createdAt)
            })
        }
        root.put("counters", countersArray)

        val entriesArray = JSONArray()
        entries.forEach { e ->
            entriesArray.put(JSONObject().apply {
                put("counterId", e.counterId)
                put("date", e.date)
                put("count", e.count)
            })
        }
        root.put("entries", entriesArray)

        // Settings (v2+)
        if (settings != null) {
            root.put("settings", JSONObject().apply {
                put("soundEnabled", settings.soundEnabled)
                put("hapticEnabled", settings.hapticEnabled)
                put("themeMode", settings.themeMode)
                put("backupEnabled", settings.backupEnabled)
            })
        }

        return root.toString(2)
    }

    /** Snapshot of all app settings for backup/restore. */
    data class AppSettings(
        val soundEnabled: Boolean = true,
        val hapticEnabled: Boolean = true,
        val themeMode: String = "system",
        val backupEnabled: Boolean = true,
    )

    data class ImportData(
        val counters: List<Counter>,
        val entries: List<CounterEntry>,
        val settings: AppSettings? = null // null for v1 backups
    )

    /**
     * Parse JSON backup. Backward-compatible with v1 (no settings, no startDate).
     */
    fun fromJson(json: String): ImportData {
        val root = JSONObject(json)
        val countersArray = root.getJSONArray("counters")
        val entriesArray = root.getJSONArray("entries")

        val counters = (0 until countersArray.length()).map { i ->
            val c = countersArray.getJSONObject(i)
            Counter(
                id = c.getLong("id"),
                name = c.getString("name"),
                icon = c.optString("icon", "🔢"),
                colorHex = c.optString("colorHex", "#00F0FF"),
                stepValue = c.optInt("stepValue", 1),
                startingCount = c.optInt("startingCount", 0),
                startDate = c.optString("startDate", "").takeIf { it.isNotEmpty() && it != "null" },
                sortOrder = c.optInt("sortOrder", 0),
                createdAt = c.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val entries = (0 until entriesArray.length()).map { i ->
            val e = entriesArray.getJSONObject(i)
            CounterEntry(
                counterId = e.getLong("counterId"),
                date = e.getString("date"),
                count = e.getInt("count")
            )
        }

        // Parse settings if present (v2+)
        val settings = if (root.has("settings")) {
            val s = root.getJSONObject("settings")
            AppSettings(
                soundEnabled = s.optBoolean("soundEnabled", true),
                hapticEnabled = s.optBoolean("hapticEnabled", true),
                themeMode = s.optString("themeMode", "system"),
                backupEnabled = s.optBoolean("backupEnabled", true),
            )
        } else null

        return ImportData(counters, entries, settings)
    }
}
