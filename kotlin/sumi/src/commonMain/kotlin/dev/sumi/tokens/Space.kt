package dev.sumi.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Spacing scale on a 4dp base. Use these instead of literal dp values. */
object Space {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 24.dp
    val s6 = 32.dp
    val s7 = 48.dp
    val s8 = 64.dp
}

/** Corners are nearly square: crisp, printed, not bubbly. */
object Shape {
    val radius = 2.dp
    val small = RoundedCornerShape(radius)

    /** Hairline border width. */
    val stroke = 1.dp
}

/** Motion is fast and decelerating: things snap into place, they don't float. */
object Motion {
    /** Hover and press feedback. */
    const val SNAP_MS = 90
    /** Small movements: a panel sliding, an indicator moving. */
    const val MOVE_MS = 170
    /** Content swaps: changing edition, switching tabs. */
    const val SWAP_MS = 210

    /** Default: fast start, gentle stop. */
    val easeOut = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
    val easeQuad = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)
}
