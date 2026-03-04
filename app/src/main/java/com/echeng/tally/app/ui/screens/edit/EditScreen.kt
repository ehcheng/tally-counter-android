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
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
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
    onSave: (name: String, icon: String, colorHex: String, stepValue: Int, startingCount: Int, startDate: String?) -> Unit,
    onBack: () -> Unit
) {
    val isEdit = existingCounter != null
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf("💪") }
    var selectedColorHex by rememberSaveable { mutableStateOf("#FFFF3B5C") }
    var stepText by rememberSaveable { mutableStateOf("1") }
    var startingCountText by rememberSaveable { mutableStateOf("0") }
    var startDateText by rememberSaveable { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) } // not saveable — derived from existingCounter

    // Date validation
    val dateError = remember(startDateText) {
        if (startDateText.isBlank()) null
        else try { LocalDate.parse(startDateText); null }
        catch (_: DateTimeParseException) { "Invalid date (use YYYY-MM-DD)" }
    }

    LaunchedEffect(existingCounter) {
        if (existingCounter != null && !loaded) {
            name = existingCounter.name
            icon = existingCounter.icon
            // Normalize via centralized color lookup
            selectedColorHex = findCounterColor(existingCounter.colorHex)?.hex ?: existingCounter.colorHex
            stepText = existingCounter.stepValue.toString()
            startingCountText = existingCounter.startingCount.toString()
            startDateText = existingCounter.startDate ?: ""
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Counter" else "New Counter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val step = stepText.toIntOrNull() ?: 1
                            val starting = startingCountText.toIntOrNull() ?: 0
                            val date = startDateText.ifBlank { null }
                            if (name.isNotBlank()) onSave(name.trim(), icon, selectedColorHex, maxOf(1, step), maxOf(0, starting), date)
                        },
                        enabled = name.isNotBlank() && dateError == null
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
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
            OutlinedTextField(
                value = startDateText,
                onValueChange = { startDateText = it },
                label = { Text("Start Date (optional)") },
                supportingText = {
                    Text(
                        dateError ?: "When you started tracking (YYYY-MM-DD)",
                        color = if (dateError != null) MaterialTheme.colorScheme.error
                               else LocalContentColor.current
                    )
                },
                isError = dateError != null,
                singleLine = true,
                placeholder = { Text("e.g. 2024-06-15") },
                modifier = Modifier.fillMaxWidth()
            )

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
