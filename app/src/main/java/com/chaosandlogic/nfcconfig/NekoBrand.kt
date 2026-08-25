package com.chaosandlogic.nfcconfig

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tokens sampled from Bedow's Neko Health identity.
 * Wordmark and mark are Neko's published PNG assets (press logo + site icon).
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
    height: Dp = 40.dp,
) {
    Image(
        painter = painterResource(R.drawable.neko_wordmark),
        contentDescription = "Neko",
        modifier = modifier
            .height(height)
            .fillMaxWidth(),
        contentScale = ContentScale.Fit,
        alignment = Alignment.CenterStart,
    )
}

@Composable
fun NekoMonogram(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    Image(
        painter = painterResource(R.drawable.neko_mark),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
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
