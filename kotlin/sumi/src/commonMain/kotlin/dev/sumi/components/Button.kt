package dev.sumi.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.sumi.SumiTheme
import dev.sumi.tokens.Motion
import dev.sumi.tokens.Shape

enum class ButtonVariant {
    /** Bone fill, paper text. The main action on a screen; usually only one. */
    Solid,
    /** Hairline border, ink text. Secondary actions. */
    Outline,
    /** Raised-paper fill, soft border, ink text. Quiet options in a group, like a picker. */
    Raised,
}

enum class ButtonSize(val padding: PaddingValues) {
    Md(PaddingValues(horizontal = 18.dp, vertical = 10.dp)),
    Lg(PaddingValues(horizontal = 24.dp, vertical = 14.dp)),
}

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Solid,
    size: ButtonSize = ButtonSize.Md,
) {
    val c = SumiTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()

    // Each variant maps (state) -> (fill, border, text). This table is the component's spec.
    val (fill, border, ink) = when (variant) {
        ButtonVariant.Solid -> Triple(
            when { pressed -> c.bonePressed; hovered -> c.boneHover; else -> c.bone },
            c.bone,
            c.onBone,
        )
        ButtonVariant.Outline -> Triple(
            when { pressed -> c.tint10; hovered -> c.tint5; else -> Color.Transparent },
            if (hovered || pressed) c.lineStrong else c.line,
            c.ink,
        )
        // Tints are translucent, so composite them onto paperRaised to keep the fill opaque.
        ButtonVariant.Raised -> Triple(
            when { pressed -> c.tint10; hovered -> c.tint5; else -> Color.Transparent }.compositeOver(c.paperRaised),
            if (hovered || pressed) c.line else c.lineSoft,
            c.ink,
        )
    }

    val snap = tween<Color>(Motion.SNAP_MS, easing = Motion.easeOut)
    val animFill by animateColorAsState(fill, snap)
    val animBorder by animateColorAsState(border, snap)

    Box(
        modifier
            .background(animFill, Shape.small)
            .border(Shape.stroke, animBorder, Shape.small)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(size.padding),
    ) {
        Text(text.uppercase(), style = SumiTheme.type.label, color = ink)
    }
}
