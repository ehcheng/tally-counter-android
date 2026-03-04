package com.echeng.tally.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echeng.tally.app.data.db.AppDatabase
import com.echeng.tally.app.data.repository.CounterRepository
import com.echeng.tally.app.util.AutoBackup
import com.echeng.tally.app.util.ExportHelper
import androidx.room.withTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
    private val db = AppDatabase.getInstance(app)
    private val repo = CounterRepository(db.counterDao(), db.counterEntryDao())

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(PrefsKeys.SOUND_ENABLED, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled

    private val _hapticEnabled = MutableStateFlow(prefs.getBoolean(PrefsKeys.HAPTIC_ENABLED, true))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled

    private val _themeMode = MutableStateFlow(prefs.getString(PrefsKeys.THEME_MODE, "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode

    private val _backupEnabled = MutableStateFlow(prefs.getBoolean(PrefsKeys.BACKUP_ENABLED, true))
    val backupEnabled: StateFlow<Boolean> = _backupEnabled

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        prefs.edit().putBoolean(PrefsKeys.SOUND_ENABLED, enabled).apply()
    }

    fun setHapticEnabled(enabled: Boolean) {
        _hapticEnabled.value = enabled
        prefs.edit().putBoolean(PrefsKeys.HAPTIC_ENABLED, enabled).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString(PrefsKeys.THEME_MODE, mode).apply()
    }

    fun setBackupEnabled(enabled: Boolean) {
        _backupEnabled.value = enabled
        prefs.edit().putBoolean(PrefsKeys.BACKUP_ENABLED, enabled).apply()
    }

    /** Snapshot current settings for inclusion in backups. */
    fun currentSettings() = ExportHelper.AppSettings(
        soundEnabled = _soundEnabled.value,
        hapticEnabled = _hapticEnabled.value,
        themeMode = _themeMode.value,
        backupEnabled = _backupEnabled.value,
    )

    /** Apply restored settings to prefs + in-memory state. */
    private fun applySettings(settings: ExportHelper.AppSettings) {
        setSoundEnabled(settings.soundEnabled)
        setHapticEnabled(settings.hapticEnabled)
        setThemeMode(settings.themeMode)
        setBackupEnabled(settings.backupEnabled)
    }

    /** SharedPreferences key constants — single source of truth. */
    object PrefsKeys {
        const val PREFS_NAME = "tally_prefs"
        const val SOUND_ENABLED = "sound_enabled"
        const val HAPTIC_ENABLED = "haptic_enabled"
        const val THEME_MODE = "theme_mode"
        const val BACKUP_ENABLED = "backup_enabled"
    }

    /** Shared data load for export — eliminates duplication between CSV and JSON. */
    private suspend fun loadExportData(): Pair<List<com.echeng.tally.app.data.entity.Counter>, List<com.echeng.tally.app.data.entity.CounterEntry>> {
        val counters = db.counterDao().getAllCounters().first()
        val entries = repo.getAllEntries()
        return counters to entries
    }

    fun exportCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val (counters, entries) = loadExportData()
            onResult(ExportHelper.toCsv(counters, entries))
        }
    }

    fun exportJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val (counters, entries) = loadExportData()
            onResult(ExportHelper.toJson(counters, entries, currentSettings()))
        }
    }

    fun restoreFromUri(uri: Uri, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: throw Exception("Could not read file")
                restoreFromJson(json, onDone)
            } catch (e: Exception) {
                onDone(false, "Error: ${e.message}")
            }
        }
    }

    /** Restore from the latest auto-backup file. */
    fun restoreFromLatestBackup(onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val backupFile = AutoBackup.getLatestInternalBackup(context)
                    ?: return@launch onDone(false, "No auto-backup found")
                val json = backupFile.readText()
                restoreFromJson(json, onDone)
            } catch (e: Exception) {
                onDone(false, "Error: ${e.message}")
            }
        }
    }

    fun restoreFromJson(json: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = ExportHelper.fromJson(json)

                // Atomic: clear + import in a single transaction.
                // If anything fails, the DB rolls back (no data loss).
                db.withTransaction {
                    db.clearAllTables()
                    repo.importCounters(data.counters)
                    repo.importEntries(data.entries)
                }

                // Restore settings if present (v2+ backups) — outside transaction (prefs, not DB)
                data.settings?.let { applySettings(it) }

                val settingsNote = if (data.settings != null) " + settings" else ""
                onDone(true, "Restored ${data.counters.size} counters with ${data.entries.size} entries$settingsNote")
            } catch (e: Exception) {
                onDone(false, "Error: ${e.message}")
            }
        }
    }
}
