package com.echeng.tally.app.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echeng.tally.app.ui.theme.TextSecondary
import com.echeng.tally.app.util.AutoBackup
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    themeMode: String,
    backupEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onBackupToggle: (Boolean) -> Unit,
    onExportCsv: ((String) -> Unit) -> Unit,
    onExportJson: ((String) -> Unit) -> Unit,
    onRestoreFromUri: (Uri, (Boolean, String) -> Unit) -> Unit,
    onRestoreFromLatestBackup: ((Boolean, String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showAutoRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Backup info — refreshTick forces recomputation after restore/backup operations
    var refreshTick by remember { mutableIntStateOf(0) }
    val backups = remember(refreshTick) { AutoBackup.getInternalBackups(context) }
    val lastBackup = backups.firstOrNull()
    val lastBackupText = if (lastBackup != null) {
        val instant = Instant.ofEpochMilli(lastBackup.lastModified())
        DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a").withZone(ZoneId.systemDefault()).format(instant)
    } else "Never"

    // File picker for restore
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirm = true
        }
    }

    // File creator for CSV export
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            onExportCsv { csv ->
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                snackbarMessage = "CSV exported successfully"
            }
        }
    }

    // File creator for JSON export
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportJson { json ->
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                snackbarMessage = "Backup exported successfully"
            }
        }
    }

    // Restore confirmation dialog
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirm = false
                pendingRestoreUri = null
            },
            title = { Text("Restore from Backup") },
            text = {
                Text("This will replace all current counters and history with the backup data. This cannot be undone.\n\nAre you sure?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        pendingRestoreUri?.let { uri ->
                            onRestoreFromUri(uri) { _, message ->
                                snackbarMessage = message
                                refreshTick++
                            }
                        }
                        pendingRestoreUri = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Replace & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    pendingRestoreUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Auto-restore confirmation dialog — offers latest backup or browse for file
    if (showAutoRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showAutoRestoreConfirm = false },
            title = { Text("Restore") },
            text = {
                Text("Restore from the latest auto-backup ($lastBackupText)?\n\nThis will replace all current counters, history, and settings. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAutoRestoreConfirm = false
                        onRestoreFromLatestBackup { _, message ->
                            snackbarMessage = message
                            refreshTick++
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore Latest")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAutoRestoreConfirm = false
                    restoreLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                }) {
                    Text("Browse for File")
                }
            }
        )
    }

    // Snackbar
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Feedback
            Text("Feedback", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextSecondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 8.dp))

            ListItem(
                headlineContent = { Text("Sound") },
                supportingContent = { Text("Play tick sound on tap") },
                trailingContent = { Switch(checked = soundEnabled, onCheckedChange = onSoundToggle) }
            )
            ListItem(
                headlineContent = { Text("Haptics") },
                supportingContent = { Text("Vibrate on tap") },
                trailingContent = { Switch(checked = hapticEnabled, onCheckedChange = onHapticToggle) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Appearance
            Text("Appearance", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextSecondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))

            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        RadioButton(selected = themeMode == value, onClick = { onThemeChange(value) })
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Data
            Text("Data", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextSecondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))

            ListItem(
                headlineContent = { Text("Auto-Backup") },
                supportingContent = {
                    Column {
                        if (backupEnabled) {
                            Text("Saves daily to Downloads/tally-auto-backups/")
                            Text("Last: $lastBackupText · ${backups.size} stored (keeps 7)", fontSize = 12.sp, color = TextSecondary)
                            Text("Includes counters, history, and settings", fontSize = 12.sp, color = TextSecondary)
                        } else {
                            Text("Automatic backups are disabled")
                        }
                    }
                },
                trailingContent = {
                    Switch(checked = backupEnabled, onCheckedChange = onBackupToggle)
                }
            )

            ListItem(
                headlineContent = { Text("Export CSV") },
                supportingContent = { Text("Share counter history as spreadsheet") },
                trailingContent = {
                    TextButton(onClick = { csvExportLauncher.launch("tally-export.csv") }) { Text("Export") }
                }
            )

            ListItem(
                headlineContent = { Text("Backup to File") },
                supportingContent = { Text("Save full backup (counters + settings) as JSON") },
                trailingContent = {
                    TextButton(onClick = { jsonExportLauncher.launch("tally-backup.json") }) { Text("Backup") }
                }
            )

            ListItem(
                headlineContent = { Text("Restore") },
                supportingContent = {
                    if (lastBackup != null) {
                        Text("Latest auto-backup: $lastBackupText")
                    } else {
                        Text("Browse for a backup file to restore")
                    }
                },
                trailingContent = {
                    TextButton(onClick = {
                        if (lastBackup != null) {
                            // Auto-backup exists → prompt to restore it or browse
                            showAutoRestoreConfirm = true
                        } else {
                            // No auto-backup → go straight to file browser
                            restoreLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                        }
                    }) { Text("Restore") }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Tally v1.0.7",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)
            )
        }
    }
}
