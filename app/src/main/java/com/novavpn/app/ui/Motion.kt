@file:OptIn(ExperimentalFoundationApi::class)

package com.novavpn.app.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * One motion language for the whole app (ported from Outrun's Motion.kt).
 *
 * Rules of thumb:
 *  - Things that appear use [Enter] (decelerate), things that leave use [Exit] (accelerate),
 *    things that move or resize use [Standard].
 *  - Leaving is always faster than arriving, so the UI never feels like it is waiting.
 *  - Never use the Compose default animation specs for UI (they are springs and feel floaty).
 */
object Motion {
    const val Fast = 120
    const val Short = 200
    const val Medium = 300
    const val Long = 450

    /** Popup menu morph (transitions.dev "Menu dropdown"): grows from its trigger, closes faster. */
    const val MenuOpen = 250
    const val MenuClose = 150
    /** Closing menus shrink only slightly. */
    const val MenuCloseScale = 0.97f
    /** A menu never starts smaller than this fraction of its final size. */
    const val MenuMinStart = 0.25f
    const val Overlap = 90

    /** Screen-to-screen fade. Outrun uses 700; lower this if it feels slow. */
    const val Axis = 700

    val Standard = FastOutSlowInEasing
    /** The easing of the menu morph; also used for the search field so they feel the same. */
    val Smooth = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val Enter = LinearOutSlowInEasing
    val Exit = FastOutLinearInEasing
}

/** How far a pressed element shrinks. 1f = ripple only. */
object Press {
    const val Pill = 0.97f
    const val Card = 0.985f
    const val Small = 0.92f
}

/**
 * Click handling with a springy press-in and a ripple.
 *
 * It must be the FIRST modifier of an element (before background / border / padding), so that the
 * scale and the ripple apply to the whole element:
 *
 *     Modifier.pressable(NovaShapes.Pill, Press.Pill, onClick = ...).background(...).border(...)
 */
fun Modifier.pressable(
    shape: Shape = RectangleShape,
    pressedScale: Float = Press.Pill,
    enabled: Boolean = true,
    label: String? = null,
    role: Role? = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(shape)
        .combinedClickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            onClickLabel = label,
            role = role,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

// ---- transitions ---------------------------------------------------------------------------

/**
 * A screen that slides over the current one and slides back out when closed.
 * [fromLeft] = Settings (opened from the hamburger at the top left); otherwise it rises from the bottom (Regions).
 *
 * Entering: the new screen is drawn on top and the old one stays put underneath.
 * Leaving: the screen being closed stays on top (zIndex) while it slides away to reveal the one below.
 */
fun panelTransition(entering: Boolean, fromLeft: Boolean): ContentTransform =
    if (entering) {
        val enter = if (fromLeft) slideInHorizontally(tween(Motion.Long, easing = Motion.Standard)) { -it }
        else slideInVertically(tween(Motion.Long, easing = Motion.Standard)) { it }
        ContentTransform(enter, ExitTransition.None, 1f, SizeTransform(clip = false))
    } else {
        val exit = if (fromLeft) slideOutHorizontally(tween(Motion.Medium, easing = Motion.Standard)) { -it }
        else slideOutVertically(tween(Motion.Medium, easing = Motion.Standard)) { it }
        ContentTransform(EnterTransition.None, exit, -1f, SizeTransform(clip = false))
    }

/**
 * Navigation between Home / Settings / Regions. Same as Outrun's axisTransition: the old screen and
 * the new one cross-fade over [Motion.Axis]. Nothing is stacked, one screen exists at a time.
 */
fun axisTransition(): ContentTransform = ContentTransform(
    fadeIn(tween(Motion.Axis)), fadeOut(tween(Motion.Axis)), 0f, SizeTransform(clip = false),
)

/** Swap between Home and Connected (same level, so a shorter fade). */
fun screenTransition(): ContentTransform = ContentTransform(
    fadeIn(tween(Motion.Medium, delayMillis = Motion.Overlap, easing = Motion.Enter)),
    fadeOut(tween(Motion.Short, easing = Motion.Exit)),
    0f,
    SizeTransform(clip = false),
)

/** Small content swap (a label, a header, a status line). */
fun fadeTransition(): ContentTransform = ContentTransform(
    fadeIn(tween(Motion.Short, delayMillis = Motion.Overlap / 2, easing = Motion.Enter)),
    fadeOut(tween(Motion.Fast)),
    0f,
    SizeTransform(clip = false),
)

/** Text that changes in place but should not resize its neighbours. */
fun statusTransition(): ContentTransform = ContentTransform(
    fadeIn(tween(Motion.Medium, delayMillis = Motion.Overlap / 2, easing = Motion.Enter)),
    fadeOut(tween(Motion.Fast)),
    0f,
    SizeTransform(clip = false) { _, _ -> snap() },
)

/**
 * Page change inside a popup menu: the new page slides a few dp in from the side it came from,
 * the old one fades out, and the menu card animates to the new size.
 */
fun menuPageTransition(forward: Boolean): ContentTransform {
    val dir = if (forward) 1 else -1
    return ContentTransform(
        fadeIn(tween(Motion.Short, delayMillis = Motion.Overlap / 2, easing = Motion.Enter)) +
            slideInHorizontally(tween(Motion.Medium, easing = Motion.Standard)) { it / 6 * dir },
        fadeOut(tween(Motion.Fast)) +
            slideOutHorizontally(tween(Motion.Short, easing = Motion.Standard)) { -it / 8 * dir },
        0f,
        SizeTransform(clip = true) { _, _ -> tween(Motion.Medium, easing = Motion.Standard) },
    )
}

/**
 * Title row <-> search field. Same feel as the menu morph: the search field grows out of the search
 * button (from the right edge) over Motion.MenuOpen and shrinks back into it over Motion.MenuClose, while the title row
 * simply fades.
 */
fun searchTransition(opening: Boolean): ContentTransform = if (opening) {
    ContentTransform(
        fadeIn(tween(Motion.Short, delayMillis = Motion.Overlap / 2, easing = Motion.Enter)) +
            expandHorizontally(tween(Motion.MenuOpen, easing = Motion.Smooth), expandFrom = Alignment.End),
        fadeOut(tween(Motion.Fast)),
        1f,
        SizeTransform(clip = false),
    )
} else {
    ContentTransform(
        fadeIn(tween(Motion.Short, delayMillis = Motion.Overlap / 2, easing = Motion.Enter)),
        fadeOut(tween(Motion.Short)) +
            shrinkHorizontally(tween(Motion.MenuClose, easing = Motion.Smooth), shrinkTowards = Alignment.End),
        -1f,
        SizeTransform(clip = false),
    )
}
