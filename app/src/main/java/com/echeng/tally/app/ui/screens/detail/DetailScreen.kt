package com.echeng.tally.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echeng.tally.app.data.entity.Counter
import com.echeng.tally.app.data.entity.CounterEntry
import com.echeng.tally.app.ui.components.CounterButton
import com.echeng.tally.app.ui.components.TapBurst
import com.echeng.tally.app.ui.components.createBurst
import com.echeng.tally.app.ui.components.rememberFeedbackController
import com.echeng.tally.app.ui.screens.home.formatCount
import com.echeng.tally.app.ui.theme.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    counter: Counter?,
    entries: List<CounterEntry>,
    totalCount: Int,
    todayCount: Int,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onUpdateEntry: (CounterEntry, Int) -> Unit,
    onBurst: (TapBurst) -> Unit,
    onBack: () -> Unit
) {
    val feedback = rememberFeedbackController(soundEnabled)

    var incrementBtnCenter by remember { mutableStateOf(Offset.Zero) }

    var editingEntry by remember { mutableStateOf<CounterEntry?>(null) }
    var editText by remember { mutableStateOf("") }

    if (editingEntry != null) {
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text("Edit Entry") },
            text = {
                Column {
                    Text(formatRelativeDate(editingEntry!!.date), fontSize = 14.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it.filter { c -> c.isDigit() } },
                        label = { Text("Count") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateEntry(editingEntry!!, maxOf(0, editText.toIntOrNull() ?: 0))
                    editingEntry = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) { Text("Cancel") }
            }
        )
    }

    if (counter == null) {
        Box(Modifier.fillMaxSize().background(DarkBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val counterColor = remember(counter.colorHex) { counter.colorHex.toComposeColor() }
    var chartRange by remember { mutableIntStateOf(30) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(counterColor, counterColor.copy(alpha = 0.7f))
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${counter.icon} ${counter.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        Text(
                            "Total: ${formatCount(totalCount)}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decrement
                CounterButton(
                    text = "−${counter.stepValue}",
                    color = counterColor,
                    filled = false,
                    onClick = {
                        feedback.onDecrement(hapticEnabled)
                        onDecrement()
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Total count
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val currentTotal = counter.startingCount + entries.sumOf { it.count }
                    Text(
                        formatCount(currentTotal),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (counter.targetCount != null && counter.targetCount > 0) {
                        val remaining = counter.targetCount - currentTotal
                        Text(
                            "(${if (remaining > 0) "-" else "+"}${formatCount(kotlin.math.abs(remaining))})",
                            fontSize = 14.sp,
                            color = if (remaining > 0) TextTertiary else counterColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Increment
                CounterButton(
                    text = "+${counter.stepValue}",
                    color = counterColor,
                    filled = true,
                    onClick = {
                        feedback.onIncrement(hapticEnabled)
                        onBurst(createBurst(incrementBtnCenter, counterColor, todayCount + counter.stepValue))
                        onIncrement()
                    },
                    modifier = Modifier.weight(1f).height(56.dp)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            incrementBtnCenter = Offset(
                                pos.x + coords.size.width / 2f,
                                pos.y + coords.size.height / 2f
                            )
                        },
                    fontSize = 20.sp
                )
            }
        }
    ) { padding ->
        // Memoized computed values — outside LazyColumn (composable scope)
        val sortedEntries = remember(entries) {
            entries.sortedByDescending { it.date }
        }
        val runningTotals = remember(entries, counter.startingCount) {
            val chronological = entries.sortedBy { it.date }
            buildMap {
                var cumulative = counter.startingCount
                chronological.forEach { e ->
                    cumulative += e.count
                    put(e.date, cumulative)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Stats ribbon
            item {
                StatsRibbon(entries = entries, counterColor = counterColor, startingCount = counter.startingCount, startDate = counter.startDate, targetCount = counter.targetCount, deadlineDate = counter.deadlineDate)
            }

            // Chart range selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(7 to "7d", 30 to "30d", 90 to "90d", 0 to "All").forEach { (days, label) ->
                        FilterChip(
                            selected = chartRange == days,
                            onClick = { chartRange = days },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = counterColor.copy(alpha = 0.2f),
                                selectedLabelColor = counterColor,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                enabled = true,
                                selected = false
                            ),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

            // Chart
            item {
                ChartSection(entries = entries, counterColor = counterColor, rangeDays = chartRange, startingCount = counter.startingCount)
            }

            // Min/Max/Avg
            if (entries.isNotEmpty()) {
                item {
                    val counts = entries.map { it.count }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniStatCard("Min", formatCount(counts.minOrNull() ?: 0), counterColor)
                        MiniStatCard("Today", formatCount(todayCount), counterColor)
                        MiniStatCard("Max", formatCount(counts.maxOrNull() ?: 0), counterColor)
                    }
                }
            }

            // History header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("History", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = TextPrimary)
                    Text("Tap to edit", fontSize = 12.sp, color = TextTertiary)
                }
            }

            // Entry list with running totals
            items(sortedEntries) { entry ->
                val runningTotal = runningTotals[entry.date] ?: 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingEntry = entry
                            editText = entry.count.toString()
                        }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(counterColor)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        formatRelativeDate(entry.date),
                        fontSize = 15.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "+${formatCount(entry.count)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = counterColor,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "= ${formatCount(runningTotal)}",
                        fontSize = 13.sp,
                        color = TextTertiary,
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 31.dp, end = 16.dp),
                    thickness = 0.5.dp,
                    color = DarkCardElevated
                )
            }

            // Starting count row
            if (counter.startingCount > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(counterColor.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Starting count",
                            fontSize = 15.sp,
                            color = TextTertiary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatCount(counter.startingCount),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

private data class RibbonStats(
    val totalCount: Int,
    val perDay: Float,
    val targetPerDay: Float? = null, // required pace to hit target by deadline
    val targetCount: Int? = null
)

@Composable
fun StatsRibbon(entries: List<CounterEntry>, counterColor: Color, startingCount: Int, startDate: String?, targetCount: Int? = null, deadlineDate: String? = null) {
    val stats = remember(entries, startingCount, startDate, targetCount, deadlineDate) {
        val totalCount = startingCount + entries.sumOf { it.count }
        if (totalCount == 0) return@remember null

        val daysSinceStart = try {
            val start = when {
                !startDate.isNullOrBlank() -> LocalDate.parse(startDate)
                entries.isNotEmpty() -> LocalDate.parse(entries.minBy { it.date }.date)
                else -> LocalDate.now()
            }
            maxOf(1, ChronoUnit.DAYS.between(start, LocalDate.now()).toInt() + 1)
        } catch (_: Exception) { 1 }

        val perDay = totalCount.toFloat() / daysSinceStart

        // Calculate required pace: target / total days in period (start to deadline)
        val targetPerDay = if (targetCount != null && targetCount > 0 && !deadlineDate.isNullOrBlank()) {
            try {
                val start = when {
                    !startDate.isNullOrBlank() -> LocalDate.parse(startDate)
                    entries.isNotEmpty() -> LocalDate.parse(entries.minBy { it.date }.date)
                    else -> LocalDate.now()
                }
                val deadline = LocalDate.parse(deadlineDate)
                val totalDaysInPeriod = maxOf(1, ChronoUnit.DAYS.between(start, deadline).toInt())
                targetCount.toFloat() / totalDaysInPeriod
            } catch (_: Exception) { null }
        } else null

        RibbonStats(totalCount, perDay, targetPerDay, targetCount)
    } ?: return

    val totalCount = stats.totalCount
    val perDay = stats.perDay
    val cap = stats.targetCount ?: Int.MAX_VALUE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard)
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        // Actual pace row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn("Per Day", formatCount(minOf(perDay.toInt(), cap)), counterColor, modifier = Modifier.weight(1f))
            StatColumn("Per Week", formatCount(minOf((perDay * 7).toInt(), cap, totalCount)), counterColor, modifier = Modifier.weight(1f))
            StatColumn("Per Month", formatCount(minOf((perDay * 30).toInt(), cap, totalCount)), counterColor, modifier = Modifier.weight(1f))
            StatColumn("Per Year", formatCount(minOf((perDay * 365).toInt(), cap, totalCount)), counterColor, modifier = Modifier.weight(1f))
        }

        // Pace diff row — only when target + deadline both exist
        if (stats.targetPerDay != null && stats.targetCount != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val tpd = stats.targetPerDay
                // Diff = actual pace - required pace (positive = ahead, negative = behind)
                // Each period is capped by target count
                val multipliers = listOf(1f, 7f, 30f, 365f)
                multipliers.forEach { mult ->
                    val actualPace = minOf((perDay * mult).toInt(), cap, totalCount)
                    val requiredPace = minOf((tpd * mult).toInt(), cap)
                    val diff = actualPace - requiredPace
                    val text = if (diff >= 0) "(+${formatCount(diff)})" else "(-${formatCount(-diff)})"
                    val color = if (diff >= 0) counterColor.copy(alpha = 0.7f) else TextTertiary
                    Text(
                        text,
                        fontSize = 12.sp,
                        color = color,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color, maxLines = 1)
        Spacer(modifier = Modifier.height(3.dp))
        Text(label, fontSize = 12.sp, color = TextTertiary)
    }
}

@Composable
fun MiniStatCard(label: String, value: String, color: Color) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 13.sp, color = TextTertiary)
        }
    }
}

@Composable
/**
 * Line chart showing cumulative total count over time.
 * X-axis: day labels (M/d). Y-axis: running total.
 * [rangeDays]: 7/30/90/0(all). Shows only days with entries within the range.
 */
fun ChartSection(entries: List<CounterEntry>, counterColor: Color, rangeDays: Int, startingCount: Int) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📊", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Start tapping to see your chart", fontSize = 14.sp, color = TextTertiary)
            }
        }
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    // Compute cumulative totals for the filtered date range
    data class ChartPoint(val date: String, val cumulativeTotal: Int)

    val chartData = remember(entries, rangeDays, startingCount) {
        // Aggregate entries by date, then build cumulative totals
        val dailyTotals = entries.groupBy { it.date }
            .mapValues { (_, dayEntries) -> dayEntries.sumOf { it.count } }
            .toSortedMap()

        val cumulativeByDate = mutableListOf<ChartPoint>()
        var cumulative = startingCount
        dailyTotals.forEach { (date, dayTotal) ->
            cumulative += dayTotal
            cumulativeByDate.add(ChartPoint(date, cumulative))
        }

        // Filter to the requested range
        if (rangeDays > 0) {
            val cutoff = LocalDate.now().minusDays(rangeDays.toLong()).toString()
            cumulativeByDate.filter { it.date >= cutoff }
        } else {
            cumulativeByDate
        }
    }

    // Date labels for x-axis
    val dateLabels = remember(chartData) {
        val formatter = DateTimeFormatter.ofPattern("M/d")
        chartData.map { LocalDate.parse(it.date).format(formatter) }
    }

    LaunchedEffect(chartData) {
        if (chartData.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries { series(chartData.map { it.cumulativeTotal }) }
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, value, _ ->
                    val idx = value.toInt()
                    if (idx in dateLabels.indices) dateLabels[idx] else " "
                }
            ),
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp)
    )
}

fun formatRelativeDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(date, today).toInt()
        when (days) {
            0 -> "Today"
            1 -> "Yesterday"
            in 2..6 -> "$days days ago"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    } catch (e: Exception) { dateStr }
}
