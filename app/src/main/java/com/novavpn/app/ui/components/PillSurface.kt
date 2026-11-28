package com.novavpn.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.novavpn.app.ui.Motion
import com.novavpn.app.ui.theme.Nova

/**
 * Shared pill surface: Nova.Pill fill + Nova.PillBorder outline.
 * Pass [fillAlpha] < 1 for pills that float over the map (menu pill, map label) to make them slightly translucent.
 * Changing [selected] cross-fades the fill and the outline instead of snapping.
 */
fun Modifier.chipSurface(shape: Shape, selected: Boolean = false, fillAlpha: Float = 1f): Modifier = composed {
    val fill by animateColorAsState(
        if (selected) Nova.Lime.copy(alpha = 0.10f) else Nova.Pill.copy(alpha = fillAlpha),
        tween(Motion.Short), label = "chipFill",
    )
    val outline by animateColorAsState(
        if (selected) Nova.Lime.copy(alpha = 0.85f) else Nova.PillBorder,
        tween(Motion.Short), label = "chipBorder",
    )
    this
        .clip(shape)
        .background(fill)
        .border(1.dp, outline, shape)
}

/** Fill opacity for pills that float over the map. */
const val PILL_OVERLAY_ALPHA = 0.9f
