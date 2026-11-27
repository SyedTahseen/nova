package com.novavpn.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.novavpn.app.ui.theme.Nova

/**
 * Frosted-glass look: dark translucent tint, a soft top-to-bottom sheen, and a hairline border
 * that catches light on the top-left edge. (No backdrop blur – Compose has none built in.)
 */
fun Modifier.glassSurface(shape: Shape): Modifier = this
    .clip(shape)
    .background(Nova.Card.copy(alpha = 0.55f))
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.03f)),
        ),
    )
    .border(
        1.dp,
        Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.06f)),
            start = Offset(0f, 0f),
            end = Offset(120f, 120f),
        ),
        shape,
    )
