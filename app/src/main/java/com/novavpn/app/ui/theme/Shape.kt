package com.novavpn.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Corner radii. Use these instead of RoundedCornerShape(N.dp) in a screen. */
object NovaShapes {
    val Card = RoundedCornerShape(28.dp)     // big cards: empty states, banners, dialogs
    val Pill = RoundedCornerShape(24.dp)     // rows, pills, buttons, location cards
    val Control = RoundedCornerShape(18.dp)  // small buttons inside rows, selectable chips
    val Field = RoundedCornerShape(16.dp)    // text fields
    val Menu = RoundedCornerShape(22.dp)     // popup menus (three-dot, +, per-server)
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

/** Popup menu sizes (three-dot, +, per-server menus). Colours/corners: Nova.CardRaised, Nova.CardBorder, NovaShapes.Menu. */
object NovaMenu {
    val Width = 240.dp          // menus with several pages (three-dot) use this so the card does not resize sideways
    val MinWidth = 140.dp       // smallest a content-sized menu gets
    val ScreenMargin = 8.dp     // gap kept to the screen edge
    val Shadow = 12.dp
}
