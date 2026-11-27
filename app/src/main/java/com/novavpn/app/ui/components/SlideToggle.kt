package com.novavpn.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novavpn.app.ui.theme.Nova
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SlideDirection { Right, Left }

/**
 * Pill-shaped "slide to confirm" control.
 * Right: knob starts at the left (Connect). Left: knob starts at the right (Disconnect).
 */
@Composable
fun SlideToggle(
    label: String,
    labelColor: Color,
    direction: SlideDirection,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }

    val trackHeight = 78.dp
    val knobWidth = 82.dp
    val knobHeight = 62.dp
    val pad = 8.dp
    val shape = RoundedCornerShape(trackHeight / 2)

    val infinite = rememberInfiniteTransition(label = "hint")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val alphas = List(3) { i ->
        val idx = if (direction == SlideDirection.Right) i else 2 - i
        val a = ((phase - idx) % 3f + 3f) % 3f
        0.22f + 0.6f * (1f - a / 3f)
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(shape)
            .background(Nova.Card)
            .border(1.dp, Nova.CardBorder, shape),
    ) {
        val travelPx = with(density) { (maxWidth - knobWidth - pad * 2).toPx() }
        val padPx = with(density) { pad.toPx() }
        val p = progress.value
        val knobX = padPx + (if (direction == SlideDirection.Right) p else 1f - p) * travelPx

        // Label
        Text(
            text = label,
            color = labelColor,
            fontSize = 19.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.Center)
                .alpha((1f - p * 1.4f).coerceIn(0f, 1f)),
        )

        // Direction hint
        Box(
            Modifier
                .align(if (direction == SlideDirection.Right) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 32.dp)
                .alpha((1f - p * 1.6f).coerceIn(0f, 1f)),
        ) {
            TripleChevron(
                color = Nova.White,
                alphas = alphas,
                left = direction == SlideDirection.Left,
            )
        }

        // Knob
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(knobX.roundToInt(), 0) }
                .size(knobWidth, knobHeight)
                .shadow(6.dp, RoundedCornerShape(knobHeight / 2))
                .clip(RoundedCornerShape(knobHeight / 2))
                .background(Nova.White)
                .pointerInput(direction, travelPx, enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (progress.value > 0.85f) {
                                    progress.animateTo(1f, tween(120))
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onComplete()
                                    delay(500)
                                    progress.snapTo(0f)
                                } else {
                                    progress.animateTo(0f, spring())
                                }
                            }
                        },
                        onDragCancel = { scope.launch { progress.animateTo(0f, spring()) } },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val d = if (direction == SlideDirection.Right) dragAmount else -dragAmount
                            scope.launch {
                                progress.snapTo((progress.value + d / travelPx).coerceIn(0f, 1f))
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            DotsGridIcon(Color(0xFFB9B9BF))
        }
    }
}
