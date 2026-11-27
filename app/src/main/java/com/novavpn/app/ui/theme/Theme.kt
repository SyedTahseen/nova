package com.novavpn.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Nova {
    val Black = Color(0xFF000000)
    val Card = Color(0xFF141416)
    val CardRaised = Color(0xFF1C1C1F)
    val CardBorder = Color(0xFF26262A)

    // Pills (region chips, menu pill, map label pill) – one token so they all match.
    val Pill = Color(0xFF141416)
    val PillBorder = Color(0xFF26262A)

    val Lime = Color(0xFFE4FF4F)
    val White = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFF8C8C92)
    val Red = Color(0xFFFF5A5F)
    val Orange = Color(0xFFFF8A4C)
    val Green = Color(0xFF9BE15D)
    val Gray = Color(0xFF5A5A60)

    // Map
    val Ocean = Color(0xFF0A0A0B)
    val Land = Color(0xFF1D1D20)
    val LandBorder = Color(0xFF3A3A40)
    val Grid = Color(0xFF151517)
}

@Composable
fun NovaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Nova.Lime,
            onPrimary = Nova.Black,
            background = Nova.Black,
            onBackground = Nova.White,
            surface = Nova.Card,
            onSurface = Nova.White,
        ),
        content = content,
    )
}
