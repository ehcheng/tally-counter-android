package com.echeng.tally.app.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import com.echeng.tally.app.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AutoBackup {
    private const val MAX_BACKUPS = 10
    private const val INTERNAL_DIR = "backups"
    private const val DOWNLOADS_SUBDIR = "tally-auto-backups"
    private const val FILE_PREFIX = "tally-"
    private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")

    /** Result of a backup attempt — no more silent failures. */
    sealed class BackupResult {
        data class Success(val filename: String, val counters: Int, val entries: Int) : BackupResult()
        data class Failure(val error: Throwable) : BackupResult()
        data object Skipped : BackupResult() // empty database, nothing to back up
    }

    /**
     * Perform a backup. Pass [settings] from SettingsViewModel.currentSettings()
     * to avoid duplicating SharedPreferences reads.
     */
    suspend fun performBackup(context: Context, settings: ExportHelper.AppSettings? = null): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Check if backups are enabled (fall back to prefs if settings not provided)
            val backupEnabled = settings?.backupEnabled
                ?: context.getSharedPreferences("tally_prefs", Context.MODE_PRIVATE)
                    .getBoolean("backup_enabled", true)
            if (!backupEnabled) {
                return@withContext BackupResult.Skipped
            }

            val db = AppDatabase.getInstance(context)

            val counters = db.counterDao().getAllCounters().first()
            if (counters.isEmpty()) return@withContext BackupResult.Skipped

            val entries = db.counterEntryDao().getAllEntries()
            val json = ExportHelper.toJson(counters, entries, settings)
            val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT)
            val filename = "$FILE_PREFIX$timestamp.json"

            // 1. Internal backup
            saveInternal(context, filename, json)

            // 2. Downloads/tally-auto-backups/
            saveToDownloads(context, filename, json)

            android.util.Log.i("AutoBackup", "Backup saved: $filename (${counters.size} counters, ${entries.size} entries)")
            BackupResult.Success(filename, counters.size, entries.size)
        } catch (e: Exception) {
            android.util.Log.e("AutoBackup", "Backup failed", e)
            BackupResult.Failure(e)
        }
    }

    /** Match both old (taptick-backup-*) and new (tally-*) backup filenames. */
    private fun isBackupFile(f: File) =
        f.name.endsWith(".json") && (f.name.startsWith(FILE_PREFIX) || f.name.startsWith("taptick-backup-"))

    private fun saveInternal(context: Context, filename: String, json: String) {
        val backupDir = File(context.filesDir, INTERNAL_DIR)
        backupDir.mkdirs()
        File(backupDir, filename).writeText(json)

        backupDir.listFiles { f -> isBackupFile(f) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_BACKUPS)
            ?.forEach { it.delete() }
    }

    private fun saveToDownloads(context: Context, filename: String, json: String) {
        try {
            // Use legacy file access as fallback + MediaStore
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupDir = File(downloadsDir, DOWNLOADS_SUBDIR)
            backupDir.mkdirs()
            val file = File(backupDir, filename)
            file.writeText(json)

            // Scan all files in backup dir so they show in file pickers
            val allFiles = backupDir.listFiles { f -> isBackupFile(f) }
                ?.map { it.absolutePath }?.toTypedArray() ?: arrayOf(file.absolutePath)
            val mimeTypes = Array(allFiles.size) { "application/json" }
            MediaScannerConnection.scanFile(context, allFiles, mimeTypes, null)

            // Prune old downloads backups too
            backupDir.listFiles { f -> isBackupFile(f) }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(MAX_BACKUPS)
                ?.forEach { it.delete() }

            android.util.Log.i("AutoBackup", "Saved to Downloads: ${file.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("AutoBackup", "Downloads save failed, trying MediaStore", e)
            saveToDownloadsMediaStore(context, filename, json)
        }
    }

    private fun saveToDownloadsMediaStore(context: Context, filename: String, json: String) {
        // Fallback for scoped storage (Android 11+)
        try {
            val resolver = context.contentResolver

            // Delete existing file with same name
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val args = arrayOf(filename, "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_SUBDIR/")
            resolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, args)

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_SUBDIR")
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { os -> os.write(json.toByteArray()) }
                android.util.Log.i("AutoBackup", "Saved via MediaStore: $uri")
            }
        } catch (e: Exception) {
            android.util.Log.e("AutoBackup", "MediaStore fallback also failed", e)
        }
    }

    fun getInternalBackups(context: Context): List<File> {
        val backupDir = File(context.filesDir, INTERNAL_DIR)
        return backupDir.listFiles { f -> isBackupFile(f) }
            ?.sortedByDescending { it.lastModified() }
            ?.toList()
            ?: emptyList()
    }

    fun getLatestInternalBackup(context: Context): File? = getInternalBackups(context).firstOrNull()
}
