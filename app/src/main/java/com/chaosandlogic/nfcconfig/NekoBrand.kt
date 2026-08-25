package com.chaosandlogic.nfcconfig

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tokens sampled from Bedow's Neko Health identity:
 * oatmeal clinic surfaces, terracotta/citrus optimism, sage/teal calm,
 * and the shifted geometric wordmark plus scan-dot cloud.
 */
object NekoColors {
    val Oatmeal = Color(0xFFF3EFE4)
    val Porcelain = Color(0xFFFFFCF6)
    val Ink = Color(0xFF141311)
    val Stone = Color(0xFF6F6A60)
    val Line = Color(0xFFE4DFD4)
    val Terracotta = Color(0xFFE15A45)
    val Sage = Color(0xFFC5D4A3)
    val Teal = Color(0xFF3F7F7A)
    val Citrus = Color(0xFFEDE04A)
    val ScanBlue = Color(0xFF5BA9D4)
    val Danger = Color(0xFF9B3B32)
    val Placeholder = Color(0xFFB4AFA4)
    val OnAccent = Color(0xFFF8F4EA)
}

@Composable
fun NekoWordmark(
    modifier: Modifier = Modifier,
    color: Color = NekoColors.Ink,
    height: Dp = 44.dp,
) {
    Canvas(modifier.size(width = height * 4.6f, height = height)) {
        val stroke = Stroke(width = size.height * 0.11f, cap = StrokeCap.Round)
        val top = size.height * 0.16f
        val bot = size.height * 0.84f
        val mid = size.height * 0.50f
        val unit = size.width / 4.15f

        fun letterOrigin(index: Int): Float = unit * index + unit * 0.06f * index

        val n0 = letterOrigin(0)
        val nShift = size.height * 0.04f
        drawLine(color, Offset(n0 + unit * 0.08f, top), Offset(n0 + unit * 0.08f, bot), stroke.width, StrokeCap.Round)
        drawLine(
            color,
            Offset(n0 + unit * 0.18f, top + nShift),
            Offset(n0 + unit * 0.78f, bot - nShift),
            stroke.width,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(n0 + unit * 0.88f, top - nShift),
            Offset(n0 + unit * 0.88f, bot - nShift * 2f),
            stroke.width,
            StrokeCap.Round,
        )

        val e0 = letterOrigin(1)
        drawLine(color, Offset(e0 + unit * 0.10f, top), Offset(e0 + unit * 0.86f, top), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(e0 + unit * 0.10f, mid), Offset(e0 + unit * 0.64f, mid), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(e0 + unit * 0.10f, bot), Offset(e0 + unit * 0.86f, bot), stroke.width, StrokeCap.Round)

        val k0 = letterOrigin(2)
        drawLine(color, Offset(k0 + unit * 0.10f, top), Offset(k0 + unit * 0.10f, bot), stroke.width, StrokeCap.Round)
        drawLine(
            color,
            Offset(k0 + unit * 0.38f, top),
            Offset(k0 + unit * 0.86f, mid - size.height * 0.04f),
            stroke.width,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(k0 + unit * 0.42f, bot),
            Offset(k0 + unit * 0.90f, mid + size.height * 0.04f),
            stroke.width,
            StrokeCap.Round,
        )

        val o0 = letterOrigin(3)
        val oval = Size(unit * 0.78f, size.height * 0.72f)
        val oTop = top - size.height * 0.04f
        drawArc(
            color = color,
            startAngle = 28f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = Offset(o0 + unit * 0.04f, oTop),
            size = oval,
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = 208f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = Offset(o0 + unit * 0.22f, oTop + size.height * 0.08f),
            size = oval,
            style = stroke,
        )
    }
}

@Composable
fun NekoMonogram(
    modifier: Modifier = Modifier,
    color: Color = NekoColors.Ink,
    size: Dp = 28.dp,
) {
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = this.size.minDimension * 0.12f, cap = StrokeCap.Round)
        val glyph = this.size.minDimension * 0.46f
        val origin = Offset(this.size.width * 0.08f, this.size.height * 0.10f)
        drawNekoN(color, stroke, origin, glyph)
        withTransform({
            rotate(180f, pivot = center)
        }) {
            drawNekoN(color, stroke, origin, glyph)
        }
    }
}

private fun DrawScope.drawNekoN(color: Color, stroke: Stroke, origin: Offset, glyph: Float) {
    drawLine(color, origin, Offset(origin.x, origin.y + glyph * 1.7f), stroke.width, StrokeCap.Round)
    drawLine(
        color,
        Offset(origin.x + glyph * 0.18f, origin.y),
        Offset(origin.x + glyph * 0.92f, origin.y + glyph * 1.7f),
        stroke.width,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(origin.x + glyph * 0.96f, origin.y),
        Offset(origin.x + glyph * 0.96f, origin.y + glyph * 1.7f),
        stroke.width,
        StrokeCap.Round,
    )
}

/**
 * Patterned scan-dot cloud from the identity: density falls off in a wave,
 * mixing charcoal with a few optimistic terracotta and citrus points.
 */
@Composable
fun NekoScanCloud(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth()) {
        val cols = 22
        val rows = 7
        val dx = size.width / (cols - 1)
        val dy = size.height / (rows + 0.5f)
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val nx = col / (cols - 1f)
                val ny = row / (rows - 1f)
                val wave = 0.55f + 0.45f * sin((nx * 2.4f + ny * 0.8f) * PI).toFloat()
                val envelope = (1f - ny * 0.55f) * (0.35f + 0.65f * sin(nx * PI).toFloat())
                val weight = wave * envelope
                if (weight < 0.18f) continue
                val radius = 1.4f + weight * 3.2f
                val x = col * dx + sin((row + col).toFloat()) * 2.4f
                val y = row * dy + 8f + cos(col * 0.7f) * 3.2f
                val color = when {
                    (col + row * 3) % 17 == 0 -> NekoColors.Terracotta.copy(alpha = 0.85f)
                    (col + row) % 13 == 0 -> NekoColors.Citrus.copy(alpha = 0.9f)
                    (col * 2 + row) % 11 == 0 -> NekoColors.Teal.copy(alpha = 0.7f)
                    (col + row * 5) % 19 == 0 -> NekoColors.ScanBlue.copy(alpha = 0.75f)
                    (col * 3 + row) % 15 == 0 -> NekoColors.Sage.copy(alpha = 0.9f)
                    else -> NekoColors.Ink.copy(alpha = 0.18f + weight * 0.35f)
                }
                drawCircle(color, radius, Offset(x, y))
            }
        }
    }
}

@Composable
fun NekoBrandHeader(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(72.dp)) {
            NekoScanCloud(Modifier.matchParentSize())
        }
        Spacer(Modifier.height(8.dp))
        NekoWordmark()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3EFE4)
@Composable
private fun NekoBrandHeaderPreview() {
    Column(
        Modifier
            .background(NekoColors.Oatmeal)
            .padding(24.dp),
    ) {
        NekoBrandHeader()
    }
}
