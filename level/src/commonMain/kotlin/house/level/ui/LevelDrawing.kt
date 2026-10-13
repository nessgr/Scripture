package house.level.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import house.level.domain.Tilt
import house.level.domain.bubbleFraction
import house.level.domain.isLevel

/*
 * The level, drawn in one pass from theme colors (the ForeEdge precedent:
 * colors are read in the composable and passed in as plain values).
 * Tracks first, bubbles last, so no bubble ever paints under a track where
 * the cross overlaps.
 */

/** Vial geometry: the bubble's share of the tube, and the drawn weights. */
private val VIAL_BUBBLE_FRACTION = 0.28f
private val VIAL_STROKE = 2.dp
private val BULLSEYE_STROKE = 2.dp
/** The 0.5° tolerance ring's share of the bullseye's full-scale radius. */
private const val BULLSEYE_TOLERANCE_FRACTION = 0.5 / 8.0

/** Colors the level draws with, resolved once per composition. */
class LevelColors(
    val trackFill: Color,
    val trackStroke: Color,
    val bubble: Color,
    val bubbleLevel: Color,
    val ticks: Color,
    val ticksLevel: Color,
)

/**
 * The whole instrument: a horizontal vial along the top, a vertical vial
 * at right angles down the left, and the bullseye in the middle. [tilt]
 * may be null (no sensor yet) — then only the tracks are drawn: an empty,
 * quiet instrument.
 */
fun DrawScope.drawLevel(tilt: Tilt?, colors: LevelColors) {
    val g = LevelGeometry(size)
    drawVialTrack(g.horizontal, colors)
    drawVialTrack(g.vertical, colors)
    drawBullseyeTrack(g, colors)
    if (tilt == null) return
    // Each vial judges only its own axis; the bullseye judges both.
    val rollLevel = isLevel(Tilt(0.0, tilt.rollDeg))
    val pitchLevel = isLevel(Tilt(tilt.pitchDeg, 0.0))
    drawVialTicks(g.horizontal, horizontal = true, level = rollLevel, colors)
    drawVialTicks(g.vertical, horizontal = false, level = pitchLevel, colors)
    drawVialBubble(g.horizontal, fraction = bubbleFraction(tilt.rollDeg), level = rollLevel, colors = colors)
    drawVialBubble(g.vertical, fraction = bubbleFraction(tilt.pitchDeg), level = pitchLevel, colors = colors, vertical = true)
    drawBullseyeBubble(g, bubbleFraction(tilt.rollDeg), bubbleFraction(tilt.pitchDeg), isLevel(tilt), colors)
}

/** One tube of the spirit level: pill rect with thickness and travel. */
private class Vial(
    val topLeft: Offset,
    val size: Size,
) {
    val center = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f)
    val thickness = minOf(size.width, size.height)
    val radius = CornerRadius(thickness / 2f)
}

/** Where each part sits in the square content area. */
private class LevelGeometry(content: Size) {
    private val inset = content.minDimension * 0.10f
    private val tube = content.minDimension * 0.12f

    /** Horizontal vial: full-width tube across the top. */
    val horizontal = Vial(
        topLeft = Offset(inset, inset),
        size = Size(content.width - 2 * inset, tube),
    )

    /** Vertical vial at right angles: full-height tube down the left. */
    val vertical = Vial(
        topLeft = Offset(inset, inset + tube + inset),
        size = Size(tube, content.height - inset - (inset + tube + inset)),
    )

    val bullseyeCenter = Offset(content.width / 2f, vertical.center.y)
    val bullseyeRadius = content.minDimension * 0.28f
}

/** One pill-shaped tube: surfaceVariant fill, outline stroke. */
private fun DrawScope.drawVialTrack(vial: Vial, colors: LevelColors) {
    drawRoundRect(color = colors.trackFill, topLeft = vial.topLeft, size = vial.size, cornerRadius = vial.radius)
    drawRoundRect(
        color = colors.trackStroke,
        topLeft = vial.topLeft,
        size = vial.size,
        cornerRadius = vial.radius,
        style = Stroke(VIAL_STROKE.toPx()),
    )
}

/** The two short rules at vial center — the "level" marks the bubble nests between. */
private fun DrawScope.drawVialTicks(vial: Vial, horizontal: Boolean, level: Boolean, colors: LevelColors) {
    val ink = if (level) colors.ticksLevel else colors.ticks
    val gap = vial.thickness * 0.5f
    val across = vial.thickness * 0.6f // tick length spans 60% of the tube
    val stroke = VIAL_STROKE.toPx()
    if (horizontal) {
        listOf(-1f, 1f).forEach { side ->
            val x = vial.center.x + side * gap
            drawLine(ink, Offset(x, vial.center.y - across / 2f), Offset(x, vial.center.y + across / 2f), stroke)
        }
    } else {
        listOf(-1f, 1f).forEach { side ->
            val y = vial.center.y + side * gap
            drawLine(ink, Offset(vial.center.x - across / 2f, y), Offset(vial.center.x + across / 2f, y), stroke)
        }
    }
}

/**
 * The bubble: a small pill riding the tube, toward the high side. Accent
 * when calm, ember when that axis sits within tolerance.
 */
private fun DrawScope.drawVialBubble(vial: Vial, fraction: Double, level: Boolean, colors: LevelColors, vertical: Boolean = false) {
    val color = if (level) colors.bubbleLevel else colors.bubble
    val bubbleShort = vial.thickness * VIAL_BUBBLE_FRACTION
    val bubbleLong = bubbleShort * 2f
    val travel = (if (vertical) vial.size.height else vial.size.width) / 2f - bubbleLong / 2f - vial.thickness * 0.1f
    val offset = (fraction * travel).toFloat()
    val bubbleSize = if (vertical) Size(bubbleShort, bubbleLong) else Size(bubbleLong, bubbleShort)
    val bubbleTopLeft = if (vertical) {
        Offset(vial.center.x - bubbleShort / 2f, vial.center.y + offset - bubbleLong / 2f)
    } else {
        Offset(vial.center.x - offset - bubbleLong / 2f, vial.center.y - bubbleShort / 2f)
    }
    drawRoundRect(color = color, topLeft = bubbleTopLeft, size = bubbleSize, cornerRadius = CornerRadius(bubbleShort / 2f))
}

/** The bullseye: fill first, then the two stroke rings on top — outer full scale, inner 0.5° tolerance. */
private fun DrawScope.drawBullseyeTrack(g: LevelGeometry, colors: LevelColors) {
    drawCircle(colors.trackFill, radius = g.bullseyeRadius, center = g.bullseyeCenter)
    drawCircle(colors.trackStroke, radius = g.bullseyeRadius, center = g.bullseyeCenter, style = Stroke(BULLSEYE_STROKE.toPx()))
    drawCircle(
        colors.trackStroke,
        radius = (g.bullseyeRadius * BULLSEYE_TOLERANCE_FRACTION).toFloat(),
        center = g.bullseyeCenter,
        style = Stroke(BULLSEYE_STROKE.toPx()),
    )
}

/** The bullseye bubble: one filled circle at the combined offset of both axes. */
private fun DrawScope.drawBullseyeBubble(
    g: LevelGeometry,
    rollFraction: Double,
    pitchFraction: Double,
    level: Boolean,
    colors: LevelColors,
) {
    val color = if (level) colors.bubbleLevel else colors.bubble
    val travel = g.bullseyeRadius * (1f - BULLSEYE_TOLERANCE_FRACTION.toFloat())
    val center = Offset(
        x = g.bullseyeCenter.x - (rollFraction * travel).toFloat(),
        y = g.bullseyeCenter.y + (pitchFraction * travel).toFloat(),
    )
    drawCircle(color = color, radius = g.bullseyeRadius * BULLSEYE_TOLERANCE_FRACTION.toFloat() * 0.8f, center = center)
}