package com.driveclass.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 48dp 그리드의 단순 아이콘. Pleos 공식 아이콘 세트(Figma)를 받기 전까지 쓰는 대체 도형이다.
 */
enum class Glyph { Play, Pause, Next, Prev, Rewind, Forward, Arrow, Chevron, Lock, Check, Close, Back, Gear }

@Composable
fun GlyphIcon(glyph: Glyph, color: Color, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val u = w / 48f
        val stroke = Stroke(width = 4.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (glyph) {
            Glyph.Play -> {
                val p = Path().apply { moveTo(15 * u, 9 * u); lineTo(39 * u, 24 * u); lineTo(15 * u, 39 * u); close() }
                drawPath(p, color)
                drawPath(p, color, style = Stroke(4 * u, join = StrokeJoin.Round))
            }
            Glyph.Pause -> {
                drawRoundRect(color, Offset(12 * u, 9 * u), Size(8 * u, 30 * u), CornerRadius(2.5f * u))
                drawRoundRect(color, Offset(28 * u, 9 * u), Size(8 * u, 30 * u), CornerRadius(2.5f * u))
            }
            Glyph.Next -> {
                val p = Path().apply { moveTo(11 * u, 11 * u); lineTo(31 * u, 24 * u); lineTo(11 * u, 37 * u); close() }
                drawPath(p, color)
                drawPath(p, color, style = Stroke(3.5f * u, join = StrokeJoin.Round))
                drawRoundRect(color, Offset(33 * u, 11 * u), Size(5 * u, 26 * u), CornerRadius(2 * u))
            }
            Glyph.Prev -> {
                val p = Path().apply { moveTo(37 * u, 11 * u); lineTo(17 * u, 24 * u); lineTo(37 * u, 37 * u); close() }
                drawPath(p, color)
                drawPath(p, color, style = Stroke(3.5f * u, join = StrokeJoin.Round))
                drawRoundRect(color, Offset(10 * u, 11 * u), Size(5 * u, 26 * u), CornerRadius(2 * u))
            }
            Glyph.Rewind, Glyph.Forward -> {
                // 원호(시계 반대/시계 방향)와 위쪽 끝의 화살촉. 숫자는 SkipGlyph가 가운데 얹는다.
                val fwd = glyph == Glyph.Forward
                val thin = Stroke(width = 3.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawArc(color, if (fwd) -60f else -120f, if (fwd) 300f else -300f, false, Offset(7 * u, 7 * u), Size(34 * u, 34 * u), style = thin)
                val tipX = if (fwd) 32.5f else 15.5f
                val dir = if (fwd) -1f else 1f
                drawPath(
                    Path().apply {
                        moveTo((tipX + dir * 7f) * u, 4.5f * u); lineTo(tipX * u, 9.3f * u); lineTo((tipX + dir * 6f) * u, 15f * u)
                    },
                    color, style = thin,
                )
            }
            Glyph.Arrow -> {
                val p = Path().apply { moveTo(24 * u, 6 * u); lineTo(39 * u, 41 * u); lineTo(24 * u, 33 * u); lineTo(9 * u, 41 * u); close() }
                drawPath(p, color)
                drawPath(p, color, style = Stroke(3 * u, join = StrokeJoin.Round))
            }
            Glyph.Chevron -> drawPath(
                Path().apply { moveTo(18 * u, 11 * u); lineTo(31 * u, 24 * u); lineTo(18 * u, 37 * u) }, color, style = stroke,
            )
            Glyph.Back -> drawPath(
                Path().apply { moveTo(30 * u, 11 * u); lineTo(17 * u, 24 * u); lineTo(30 * u, 37 * u) }, color, style = stroke,
            )
            Glyph.Lock -> {
                drawRoundRect(color, Offset(11 * u, 21 * u), Size(26 * u, 20 * u), CornerRadius(4 * u))
                drawArc(color, 180f, 180f, false, Offset(16 * u, 9 * u), Size(16 * u, 22 * u), style = stroke)
                drawLine(color, Offset(16 * u, 20 * u), Offset(16 * u, 21 * u), 4.5f * u)
                drawLine(color, Offset(32 * u, 20 * u), Offset(32 * u, 21 * u), 4.5f * u)
            }
            Glyph.Check -> drawPath(
                Path().apply { moveTo(11 * u, 25 * u); lineTo(20 * u, 34 * u); lineTo(37 * u, 15 * u) }, color, style = stroke,
            )
            Glyph.Gear -> {
                // 디자인 시스템 미리보기의 설정 대체 도형(원 + 8방향 살)을 48 그리드로 옮긴 것.
                drawCircle(color, 6.4f * u, Offset(24 * u, 24 * u), style = stroke)
                val spokes = listOf(
                    Offset(24f, 5.6f) to Offset(24f, 11.6f), Offset(24f, 36.4f) to Offset(24f, 42.4f),
                    Offset(5.6f, 24f) to Offset(11.6f, 24f), Offset(36.4f, 24f) to Offset(42.4f, 24f),
                    Offset(11f, 11f) to Offset(15.2f, 15.2f), Offset(32.8f, 32.8f) to Offset(37f, 37f),
                    Offset(11f, 37f) to Offset(15.2f, 32.8f), Offset(32.8f, 15.2f) to Offset(37f, 11f),
                )
                spokes.forEach { (a, b) -> drawLine(color, a * u, b * u, 4.5f * u, StrokeCap.Round) }
            }
            Glyph.Close -> {
                drawLine(color, Offset(13 * u, 13 * u), Offset(35 * u, 35 * u), 4.5f * u, StrokeCap.Round)
                drawLine(color, Offset(35 * u, 13 * u), Offset(13 * u, 35 * u), 4.5f * u, StrokeCap.Round)
            }
        }
    }
}
