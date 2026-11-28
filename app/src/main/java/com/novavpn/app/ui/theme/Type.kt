package com.novavpn.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Every text style in the app. Use these instead of typing sizes and weights in a screen:
 *
 *     Text("Hello", style = NovaType.Body, color = Nova.White)
 *
 * Colour is deliberately not part of a style; pass it at the call site.
 */
object NovaType {
    val Display = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.SemiBold)   // big numbers (speed)
    val Timer = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Bold)         // connection timer
    val Headline = TextStyle(fontSize = 29.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)  // promo banner title
    val ScreenTitle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    val Title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)     // card titles
    val Status = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium)      // "Connected"
    val Heading = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold)   // section headings
    val Button = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val ButtonBold = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
    val Slider = TextStyle(fontSize = 17.sp)                                      // slide-to-connect label
    val Body = TextStyle(fontSize = 15.sp)
    val BodyStrong = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)  // row titles
    val Secondary = TextStyle(fontSize = 14.sp)                                   // chips, pills, hints
    val SecondaryStrong = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val Caption = TextStyle(fontSize = 13.sp)
    val Small = TextStyle(fontSize = 12.sp)
    val Label = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)     // menu section labels
}
