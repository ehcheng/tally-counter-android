package com.echeng.tally.app.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.echeng.tally.app.data.entity.Counter
import com.echeng.tally.app.ui.components.CounterButton
import com.echeng.tally.app.ui.components.TapBurst
import com.echeng.tally.app.ui.components.createBurst
import com.echeng.tally.app.ui.components.rememberFeedbackController
import com.echeng.tally.app.ui.theme.*
import java.text.NumberFormat

fun formatCount(count: Int): String = NumberFormat.getIntegerInstance().format(count)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    counters: List<Counter>,
    todayCounts: Map<Long, Int>,
    totalCounts: Map<Long, Int>,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    onIncrement: (Counter) -> Unit,
    onDecrement: (Counter) -> Unit,
    onCounterClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDelete: (Counter) -> Unit,
    onMoveUp: (Counter) -> Unit,
    onMoveDown: (Counter) -> Unit,
    onBurst: (TapBurst) -> Unit,
) {
    val feedback = rememberFeedbackController(soundEnabled)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tally",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                actions = {
                    IconButton(onClick = onAddClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add counter")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = TextSecondary
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (counters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎯", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No counters yet", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap + to start tracking something", fontSize = 14.sp, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dynamic spacer: push cards to center around lower 1/3
                item {
                    Spacer(modifier = Modifier.fillParentMaxHeight(
                        fraction = when {
                            counters.size >= 5 -> 0.10f
                            counters.size == 4 -> 0.20f
                            counters.size == 3 -> 0.33f
                            counters.size == 2 -> 0.40f
                            else -> 0.50f                  // Single card: center at ~lower 1/3
                        }
                    ))
                }
                items(counters, key = { it.id }) { counter ->
                    val counterColor = remember(counter.colorHex) {
                        counter.colorHex.toComposeColor()
                    }

                    // Stable lambda refs — avoid recreating on every composition
                    val onIncrementCb = remember(counter.id, counter.stepValue) {
                        {
                            feedback.onIncrement(hapticEnabled)
                            onIncrement(counter)
                        }
                    }
                    // Not remembered — reads todayCounts at tap time for accurate running total
                    val onBurstCb = { pos: Offset ->
                        val newTodayCount = (todayCounts[counter.id] ?: 0) + counter.stepValue
                        onBurst(createBurst(pos, counterColor, newTodayCount))
                    }
                    val onDecrementCb = remember(counter.id) {
                        {
                            feedback.onDecrement(hapticEnabled)
                            onDecrement(counter)
                        }
                    }

                    CounterCard(
                        counter = counter,
                        counterColor = counterColor,
                        todayCount = todayCounts[counter.id] ?: 0,
                        totalCount = totalCounts[counter.id] ?: 0,
                        onIncrement = onIncrementCb,
                        onBurst = onBurstCb,
                        onDecrement = onDecrementCb,
                        onDelete = { onDelete(counter) },
                        onEdit = { onEditClick(counter.id) },
                        onMoveUp = { onMoveUp(counter) },
                        onMoveDown = { onMoveDown(counter) },
                        isFirst = counter == counters.firstOrNull(),
                        isLast = counter == counters.lastOrNull(),
                        onClick = { onCounterClick(counter.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CounterCard(
    counter: Counter,
    counterColor: Color,
    todayCount: Int,
    totalCount: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onBurst: (Offset) -> Unit = {}
) {

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Count bump animation
    var animTrigger by remember { mutableIntStateOf(0) }
    val scale by animateFloatAsState(
        targetValue = if (animTrigger > 0) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 800f),
        label = "countBump"
    )
    LaunchedEffect(animTrigger) {
        if (animTrigger > 0) {
            kotlinx.coroutines.delay(150)
            animTrigger = 0
        }
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Counter") },
            text = { Text("Delete \"${counter.name}\"? All history will be lost.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            // Color accent stripe on left
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(120.dp)
                    .background(counterColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .align(Alignment.CenterStart)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 18.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon + Name + Today
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(counter.icon, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            counter.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "±${counter.stepValue} · Today: ${formatCount(todayCount)}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Decrement button
                CounterButton(
                    text = "−${counter.stepValue}",
                    color = counterColor,
                    filled = false,
                    onClick = onDecrement,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Count badge
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(14.dp))
                        .background(counterColor)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        formatCount(totalCount),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Increment button
                var incBtnPos by remember { mutableStateOf(Offset.Zero) }
                CounterButton(
                    text = "+${counter.stepValue}",
                    color = counterColor,
                    filled = true,
                    onClick = {
                        animTrigger++
                        onBurst(incBtnPos)
                        onIncrement()
                    },
                    modifier = Modifier.size(56.dp)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            incBtnPos = Offset(
                                pos.x + coords.size.width / 2f,
                                pos.y + coords.size.height / 2f
                            )
                        }
                )
            }

            // Context menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (!isFirst) {
                    DropdownMenuItem(
                        text = { Text("Move Up") },
                        onClick = { showMenu = false; onMoveUp() },
                        leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) }
                    )
                }
                if (!isLast) {
                    DropdownMenuItem(
                        text = { Text("Move Down") },
                        onClick = { showMenu = false; onMoveDown() },
                        leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { showMenu = false; onEdit() },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; showDeleteDialog = true },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}
