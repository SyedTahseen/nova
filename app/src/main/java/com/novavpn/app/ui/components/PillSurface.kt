package com.novavpn.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.novavpn.app.ui.theme.Nova

/**
 * Shared pill surface: Nova.Pill fill + Nova.PillBorder outline.
 * Pass [fillAlpha] < 1 for pills that float over the map (menu pill, map label) to make them slightly translucent.
 */
fun Modifier.chipSurface(shape: Shape, selected: Boolean = false, fillAlpha: Float = 1f): Modifier = this
    .clip(shape)
    .background(if (selected) Nova.Lime.copy(alpha = 0.10f) else Nova.Pill.copy(alpha = fillAlpha))
    .border(1.dp, if (selected) Nova.Lime.copy(alpha = 0.85f) else Nova.PillBorder, shape)

/** Fill opacity for pills that float over the map. */
const val PILL_OVERLAY_ALPHA = 0.9f
