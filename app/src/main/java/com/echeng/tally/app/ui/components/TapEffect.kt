package com.echeng.tally.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.*
import kotlin.random.Random

data class TapBurst(
    val id: Int,
    val origin: Offset,
    val color: Color,
    val stepText: String,
    val particles: List<Particle>,
    val startTimeMs: Long = System.currentTimeMillis()
) {
    val isAlive: Boolean get() = System.currentTimeMillis() - startTimeMs < DURATION_MS

    companion object {
        const val DURATION_MS = 900L
        const val PARTICLE_COUNT = 16
        const val PARTICLE_FADE_MS = 550f
        const val MIN_SPEED = 400f
        const val SPEED_RANGE = 300f
        const val MIN_SIZE = 4f
        const val SIZE_RANGE = 5f
        const val MIN_DECAY = 0.85f
        const val DECAY_RANGE = 0.3f
        const val TEXT_RISE_DP = 180f
        const val TEXT_BASE_SIZE_SP = 26f
        const val TEXT_SCALE_MIN = 1.2f
        const val TEXT_SCALE_RANGE = 0.5f
    }
}

data class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val decay: Float,
)

/**
 * Create a particle burst. [displayCount] is the number shown in the floating text —
 * typically today's running total (post-increment) so users can track daily progress
 * from the home screen without opening detail view.
 */
fun createBurst(origin: Offset, color: Color, displayCount: Int): TapBurst {
    val particles = (0 until TapBurst.PARTICLE_COUNT).map {
        val baseAngle = (it.toFloat() / TapBurst.PARTICLE_COUNT) * 2f * PI.toFloat()
        Particle(
            angle = baseAngle + Random.nextFloat() * 0.4f - 0.2f,
            speed = TapBurst.MIN_SPEED + Random.nextFloat() * TapBurst.SPEED_RANGE,
            size = TapBurst.MIN_SIZE + Random.nextFloat() * TapBurst.SIZE_RANGE,
            decay = TapBurst.MIN_DECAY + Random.nextFloat() * TapBurst.DECAY_RANGE
        )
    }
    return TapBurst(
        id = Random.nextInt(),
        origin = origin,
        color = color,
        stepText = "+$displayCount",
        particles = particles
    )
}

@Composable
fun TapEffectOverlay(
    bursts: List<TapBurst>,
    modifier: Modifier = Modifier
) {
    // Skip entirely when no bursts — zero overhead when idle
    if (bursts.isEmpty()) return

    val density = LocalDensity.current
    val dpToPx = density.density

    // Cached paint — reused across frames, only color/size change per burst
    val textPaint = remember {
        android.graphics.Paint().apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    // Frame tick drives draw invalidation — only runs while bursts are alive.
    // Uses Compose's withFrameNanos (ties into the animation clock) instead of
    // a raw Choreographer that fires every vsync indefinitely.
    var frameTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(bursts) {
        // Loop until every burst in this snapshot has expired.
        // ~60fps via 16ms delay — only runs while bursts are active.
        while (bursts.any { it.isAlive }) {
            frameTick = System.nanoTime()
            delay(16L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()

                // Reading frameTick inside drawWithContent forces the draw phase
                // to re-execute each frame — critical for Samsung devices where
                // Compose draw-phase optimization skips redraws otherwise.
                @Suppress("UNUSED_VARIABLE")
                val tick = frameTick
                val now = System.currentTimeMillis()

                for (burst in bursts) {
                    val elapsed = now - burst.startTimeMs
                    if (elapsed > TapBurst.DURATION_MS || elapsed < 0) continue

                    // Particles fade in 550ms
                    val particleProgress = (elapsed.toFloat() / TapBurst.PARTICLE_FADE_MS).coerceAtMost(1f)
                    val particleEase = 1f - (1f - particleProgress).pow(3)

                    // Text uses full duration
                    val progress = elapsed.toFloat() / TapBurst.DURATION_MS
                    val easeOut = 1f - (1f - progress).pow(3)

                    // Particles
                    for (particle in burst.particles) {
                        val distance = particle.speed * particleEase * dpToPx * 0.3f
                        val alpha = ((1f - particleProgress) * particle.decay).coerceIn(0f, 1f)
                        val radius = particle.size * dpToPx * (1f - particleProgress * 0.4f)

                        val x = burst.origin.x + cos(particle.angle) * distance
                        val y = burst.origin.y + sin(particle.angle) * distance

                        drawCircle(
                            color = burst.color.copy(alpha = alpha),
                            radius = radius,
                            center = Offset(x, y)
                        )
                    }

                    // Floating "+N" — bigger, rises higher, outlasts particles
                    val textAlpha = (1f - progress).coerceIn(0f, 1f)
                    val textY = burst.origin.y - (TapBurst.TEXT_RISE_DP * dpToPx * easeOut)
                    val textScale = TapBurst.TEXT_SCALE_MIN + easeOut * TapBurst.TEXT_SCALE_RANGE

                    drawIntoCanvas { canvas ->
                        textPaint.apply {
                            color = android.graphics.Color.argb(
                                (textAlpha * 255).toInt(),
                                (burst.color.red * 255).toInt(),
                                (burst.color.green * 255).toInt(),
                                (burst.color.blue * 255).toInt()
                            )
                            textSize = TapBurst.TEXT_BASE_SIZE_SP * dpToPx * textScale
                            setShadowLayer(8f * dpToPx, 0f, 3f, android.graphics.Color.argb(180, 0, 0, 0))
                        }
                        canvas.nativeCanvas.drawText(burst.stepText, burst.origin.x, textY, textPaint)
                    }
                }
            }
    )
}
