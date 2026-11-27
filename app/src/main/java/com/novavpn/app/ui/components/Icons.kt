package com.novavpn.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Single chevron. [left] mirrors it. */
@Composable
fun ChevronIcon(
    color: Color,
    modifier: Modifier = Modifier.size(20.dp),
    left: Boolean = false,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.36f, h * 0.24f)
            lineTo(w * 0.66f, h * 0.5f)
            lineTo(w * 0.36f, h * 0.76f)
        }
        scale(if (left) -1f else 1f, 1f) {
            drawPath(
                path,
                color,
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** Three tight, thin chevrons with individual alphas – the "swipe this way" hint. */
@Composable
fun TripleChevron(color: Color, alphas: List<Float>, left: Boolean) {
    Canvas(Modifier.size(32.dp, 18.dp)) {
        val step = 10.dp.toPx()
        val cw = 6.5.dp.toPx()
        val ch = size.height * 0.9f
        val top = (size.height - ch) / 2f
        repeat(3) { i ->
            val x0 = 1.5.dp.toPx() + i * step
            val path = Path().apply {
                if (left) {
                    moveTo(x0 + cw, top); lineTo(x0, top + ch / 2f); lineTo(x0 + cw, top + ch)
                } else {
                    moveTo(x0, top); lineTo(x0 + cw, top + ch / 2f); lineTo(x0, top + ch)
                }
            }
            drawPath(
                path,
                color.copy(alpha = alphas[i]),
                style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
fun CheckIcon(color: Color, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.72f)
            lineTo(w * 0.78f, h * 0.30f)
        }
        drawPath(
            path,
            color,
            style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** The "(•)" broadcast glyph used for pings and the map marker. */
@Composable
fun SignalIcon(color: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier) {
        val c = center
        val w = size.width
        drawCircle(color, radius = w * 0.11f, center = c)
        val stroke = Stroke(w * 0.07f, cap = StrokeCap.Round)
        for (r in listOf(w * 0.27f, w * 0.44f)) {
            val tl = Offset(c.x - r, c.y - r)
            val sz = Size(r * 2, r * 2)
            drawArc(color, -42f, 84f, false, tl, sz, style = stroke)
            drawArc(color, 138f, 84f, false, tl, sz, style = stroke)
        }
    }
}

/** 3x2 dot grid shown on the slider knob. */
@Composable
fun DotsGridIcon(color: Color, modifier: Modifier = Modifier.size(30.dp, 20.dp)) {
    Canvas(modifier) {
        val r = size.height * 0.065f
        for (row in 0..1) {
            for (col in 0..2) {
                drawCircle(
                    color,
                    radius = r,
                    center = Offset(
                        size.width * (0.2f + col * 0.3f),
                        size.height * (0.3f + row * 0.4f),
                    ),
                )
            }
        }
    }
}
