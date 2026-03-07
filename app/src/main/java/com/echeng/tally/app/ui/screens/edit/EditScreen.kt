package com.echeng.tally.app.ui.screens.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echeng.tally.app.data.entity.Counter
import com.echeng.tally.app.ui.theme.CounterColorOptions
import com.echeng.tally.app.ui.theme.DarkBackground
import com.echeng.tally.app.ui.theme.findCounterColor

private val EmojiOptions = listOf(
    // Fitness
    "💪", "🏋️", "🤸", "🧎", "🏃", "🚴", "🏊", "🧘",
    // Sports
    "⚽", "🏀", "🎾", "🏓", "🥊", "🏈", "⚾", "🏐",
    // Health & Wellness
    "💧", "🍎", "💊", "🥗", "😴", "🧠", "❤️", "🩺",
    // Productivity
    "📚", "📝", "✅", "🎯", "💻", "📧", "☎️", "📊",
    // Daily life
    "☕", "🎵", "🐕", "🚶", "🧹", "🛒", "🙏", "😈",
    // Misc
    "⭐", "🔥", "🎮", "🔢", "🏆", "💰", "🌱", "🎨"
)

// Colors now sourced from CounterColorOptions in Theme.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    existingCounter: Counter?,
    isNewCounter: Boolean = false,
    onSave: (name: String, icon: String, colorHex: String, stepValue: Int, startingCount: Int, startDate: String?) -> Unit,
    onBack: () -> Unit
) {
    val isEdit = !isNewCounter
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf("💪") }
    var selectedColorHex by rememberSaveable { mutableStateOf("#FFFF3B5C") }
    var stepText by rememberSaveable { mutableStateOf("1") }
    var startingCountText by rememberSaveable { mutableStateOf("0") }
    var startDateText by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) } // not saveable — derived from existingCounter

    // Snapshot original values AFTER loading completes
    var originalName by remember { mutableStateOf(existingCounter?.name ?: "") }
    var originalIcon by remember { mutableStateOf(existingCounter?.icon ?: "💪") }
    var originalColorHex by remember { mutableStateOf(existingCounter?.let { findCounterColor(it.colorHex)?.hex ?: it.colorHex } ?: "#FFFF3B5C") }
    var originalStep by remember { mutableStateOf(existingCounter?.stepValue?.toString() ?: "1") }
    var originalStartingCount by remember { mutableStateOf(existingCounter?.startingCount?.toString() ?: "0") }
    var originalStartDate by remember { mutableStateOf(existingCounter?.startDate ?: "") }

    val hasChanges = loaded && (name != originalName || icon != originalIcon || selectedColorHex != originalColorHex ||
            stepText != originalStep || startingCountText != originalStartingCount || startDateText != originalStartDate)

    // For new counters, track changes once the user modifies anything from defaults
    val hasNewCounterChanges = !isEdit && name.isNotBlank()

    // Intercept back button when there are unsaved changes
    BackHandler(enabled = hasChanges || hasNewCounterChanges) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    LaunchedEffect(existingCounter) {
        if (existingCounter != null && !loaded) {
            name = existingCounter.name
            icon = existingCounter.icon
            selectedColorHex = findCounterColor(existingCounter.colorHex)?.hex ?: existingCounter.colorHex
            stepText = existingCounter.stepValue.toString()
            startingCountText = existingCounter.startingCount.toString()
            startDateText = existingCounter.startDate ?: ""
            // Snapshot originals for change detection
            originalName = name
            originalIcon = icon
            originalColorHex = selectedColorHex
            originalStep = stepText
            originalStartingCount = startingCountText
            originalStartDate = startDateText
            loaded = true
        }
        // Only mark loaded for genuinely new counters, not edit-mode loading
        if (existingCounter == null && isNewCounter) loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Counter" else "New Counter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges || hasNewCounterChanges) showDiscardDialog = true else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val showSaveButton = name.isNotBlank() && (hasChanges || hasNewCounterChanges)
                    val saveButtonColor by animateColorAsState(
                        targetValue = if (showSaveButton) {
                            try { Color(android.graphics.Color.parseColor(selectedColorHex)) }
                            catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        } else Color.Transparent,
                        animationSpec = tween(300),
                        label = "saveColor"
                    )
                    if (showSaveButton) {
                        FilledTonalButton(
                            onClick = {
                                val step = stepText.toIntOrNull() ?: 1
                                val starting = startingCountText.toIntOrNull() ?: 0
                                val date = startDateText.ifBlank { null }
                                onSave(name.trim(), icon, selectedColorHex, maxOf(1, step), maxOf(0, starting), date)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = saveButtonColor.copy(alpha = 0.85f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val step = stepText.toIntOrNull() ?: 1
                                val starting = startingCountText.toIntOrNull() ?: 0
                                val date = startDateText.ifBlank { null }
                                if (name.isNotBlank()) onSave(name.trim(), icon, selectedColorHex, maxOf(1, step), maxOf(0, starting), date)
                            },
                            enabled = name.isNotBlank()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Counter Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = stepText,
                    onValueChange = { stepText = it.filter { c -> c.isDigit() } },
                    label = { Text("Step Value") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = startingCountText,
                    onValueChange = { startingCountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Starting Count") },
                    supportingText = { Text("Initial value before any taps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // Start date (for migration from other apps)
            val displayDate = if (startDateText.isNotBlank()) {
                try {
                    LocalDate.parse(startDateText).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                } catch (_: Exception) { startDateText }
            } else null

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Start Date (optional)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayDate ?: "Tap to select date",
                        color = if (displayDate != null) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                    if (displayDate != null) {
                        Text(
                            text = "✕",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { startDateText = "" }
                        )
                    }
                }
                Text(
                    "When you started tracking",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = if (startDateText.isNotBlank()) {
                        try {
                            LocalDate.parse(startDateText)
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                                .toEpochMilli()
                        } catch (_: Exception) { null }
                    } else null
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                startDateText = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .toString()
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Icon picker
            Text("Icon", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(EmojiOptions) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (emoji == icon) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable { icon = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }

            // Color picker
            Text("Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(CounterColorOptions) { option ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .then(
                                if (selectedColorHex == option.hex) Modifier.border(3.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedColorHex = option.hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
