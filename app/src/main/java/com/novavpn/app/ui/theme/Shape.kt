package com.novavpn.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Corner radii. Use these instead of RoundedCornerShape(N.dp) in a screen. */
object NovaShapes {
    val Card = RoundedCornerShape(28.dp)     // big cards: empty states, banners, dialogs
    val Pill = RoundedCornerShape(24.dp)     // rows, pills, buttons, location cards
    val Control = RoundedCornerShape(18.dp)  // small buttons inside rows, selectable chips
    val Field = RoundedCornerShape(16.dp)    // text fields
    val Small = RoundedCornerShape(9.dp)     // speed bars and other tiny elements
}

/** Spacing scale for paddings and gaps. */
object NovaSpace {
    val Xs = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp     // screen edge padding
    val Xl = 20.dp    // card inner padding
    val Xxl = 24.dp
}
