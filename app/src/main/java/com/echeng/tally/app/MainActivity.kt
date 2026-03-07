package com.echeng.tally.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.echeng.tally.app.ui.screens.detail.DetailScreen
import com.echeng.tally.app.ui.screens.edit.EditScreen
import com.echeng.tally.app.ui.components.TapBurst
import com.echeng.tally.app.ui.components.TapEffectOverlay
import com.echeng.tally.app.ui.screens.home.HomeScreen
import com.echeng.tally.app.ui.screens.settings.SettingsScreen
import com.echeng.tally.app.ui.theme.TallyTheme
import com.echeng.tally.app.util.AutoBackup
import com.echeng.tally.app.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val themeMode by settingsVm.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            TallyTheme(darkTheme = darkTheme) {
                var globalBursts by remember { mutableStateOf<List<TapBurst>>(emptyList()) }

                LaunchedEffect(globalBursts.size) {
                    if (globalBursts.isNotEmpty()) {
                        kotlinx.coroutines.delay(700)
                        globalBursts = globalBursts.filter { it.isAlive }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    TallyApp(settingsVm, onBurst = { burst ->
                        globalBursts = (globalBursts + burst).takeLast(8)
                    })
                    TapEffectOverlay(bursts = globalBursts)
                }
            }
        }
    }
}

@Composable
fun TallyApp(settingsVm: SettingsViewModel, onBurst: (TapBurst) -> Unit = {}) {
    val navController = rememberNavController()
    val soundEnabled by settingsVm.soundEnabled.collectAsState()
    val hapticEnabled by settingsVm.hapticEnabled.collectAsState()
    val context = LocalContext.current

    // Auto-backup on launch
    LaunchedEffect(Unit) {
        AutoBackup.performBackup(context)
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel()
            val counters by vm.counters.collectAsState(initial = emptyList())
            val todayCounts by vm.todayCounts.collectAsState()
            val totalCounts by vm.totalCounts.collectAsState()

            // Backup when app goes to background
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        vm.backupNow()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            HomeScreen(
                counters = counters,
                todayCounts = todayCounts,
                totalCounts = totalCounts,
                soundEnabled = soundEnabled,
                hapticEnabled = hapticEnabled,
                onIncrement = { vm.increment(it) },
                onDecrement = { vm.decrement(it) },
                onDelete = { vm.deleteCounter(it) },
                onMoveUp = { counter -> vm.moveCounterUp(counter.id) },
                onBurst = { burst -> onBurst(burst) },
                onMoveDown = { counter -> vm.moveCounterDown(counter.id) },
                onCounterClick = { navController.navigate("detail/$it") },
                onEditClick = { navController.navigate("edit/$it") },
                onAddClick = { navController.navigate("edit") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable("edit") {
            val vm: EditViewModel = viewModel()
            EditScreen(
                existingCounter = null,
                isNewCounter = true,
                onSave = { name, icon, color, step, starting, startDate ->
                    vm.saveCounter(name, icon, color, step, starting, startDate) { navController.popBackStack() }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "edit/{counterId}",
            arguments = listOf(navArgument("counterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val counterId = backStackEntry.arguments?.getLong("counterId") ?: return@composable
            val vm: EditViewModel = viewModel()
            LaunchedEffect(counterId) { vm.loadCounter(counterId) }
            val counter by vm.counter.collectAsState()
            EditScreen(
                existingCounter = counter,
                onSave = { name, icon, color, step, starting, startDate ->
                    vm.saveCounter(name, icon, color, step, starting, startDate) { navController.popBackStack() }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "detail/{counterId}",
            arguments = listOf(navArgument("counterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val counterId = backStackEntry.arguments?.getLong("counterId") ?: return@composable
            val vm: DetailViewModel = viewModel()
            LaunchedEffect(counterId) { vm.loadCounter(counterId) }
            val counter by vm.counter.collectAsState()
            val entries by vm.entries.collectAsState()
            val totalCount by vm.totalCount.collectAsState()
            val todayCount by vm.todayCount.collectAsState()

            DetailScreen(
                counter = counter,
                entries = entries,
                totalCount = totalCount,
                todayCount = todayCount,
                soundEnabled = soundEnabled,
                hapticEnabled = hapticEnabled,
                onIncrement = { vm.increment() },
                onDecrement = { vm.decrement() },
                onUpdateEntry = { entry, count -> vm.updateEntry(entry, count) },
                onBurst = { burst -> onBurst(burst) },
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            val themeMode by settingsVm.themeMode.collectAsState()
            val backupEnabled by settingsVm.backupEnabled.collectAsState()
            SettingsScreen(
                soundEnabled = soundEnabled,
                hapticEnabled = hapticEnabled,
                themeMode = themeMode,
                backupEnabled = backupEnabled,
                onSoundToggle = { settingsVm.setSoundEnabled(it) },
                onHapticToggle = { settingsVm.setHapticEnabled(it) },
                onThemeChange = { settingsVm.setThemeMode(it) },
                onBackupToggle = { settingsVm.setBackupEnabled(it) },
                onExportCsv = { callback -> settingsVm.exportCsv(callback) },
                onExportJson = { callback -> settingsVm.exportJson(callback) },
                onRestoreFromUri = { uri, callback -> settingsVm.restoreFromUri(uri, callback) },
                onRestoreFromLatestBackup = { callback -> settingsVm.restoreFromLatestBackup(callback) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
