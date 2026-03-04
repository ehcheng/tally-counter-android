package com.echeng.tally.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared increment/decrement button used on both Home and Detail screens.
 * [filled] = true for increment (solid color), false for decrement (translucent).
 */
@Composable
fun CounterButton(
    text: String,
    color: Color,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp
) {
    val containerColor = if (filled) color else color.copy(alpha = 0.15f)
    val contentColor = if (filled) Color.White else color

    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = fontSize)
    }
}
