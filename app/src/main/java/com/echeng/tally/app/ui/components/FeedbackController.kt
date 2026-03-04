package com.echeng.tally.app.ui.components

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.echeng.tally.app.util.HapticHelper
import com.echeng.tally.app.util.SoundManager

/**
 * Shared feedback controller for sound + haptics.
 * Replaces duplicated SoundManager init + HapticHelper calls in Home/Detail screens.
 */
class FeedbackController(
    private val soundManager: SoundManager,
    private val context: Context,
) {
    fun onIncrement(hapticEnabled: Boolean) {
        soundManager.playTick()
        HapticHelper.heavyClick(context, hapticEnabled)
    }

    fun onDecrement(hapticEnabled: Boolean) {
        soundManager.playTick()
        HapticHelper.lightClick(context, hapticEnabled)
    }
}

/**
 * Remember a FeedbackController scoped to composition lifetime.
 * Handles SoundManager lifecycle (create/release) and sound enabled state.
 */
@Composable
fun rememberFeedbackController(soundEnabled: Boolean): FeedbackController {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    DisposableEffect(Unit) { onDispose { soundManager.release() } }
    LaunchedEffect(soundEnabled) { soundManager.enabled = soundEnabled }
    return remember(soundManager, context) { FeedbackController(soundManager, context) }
}
