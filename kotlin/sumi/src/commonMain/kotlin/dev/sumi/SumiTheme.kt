package dev.sumi

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.sumi.tokens.Edition
import dev.sumi.tokens.Motion
import dev.sumi.tokens.SumiColors
import dev.sumi.tokens.SumiType
import dev.sumi.tokens.sumiType

private val LocalSumiColors = staticCompositionLocalOf { Edition.Ember.colors }
private val LocalSumiType = staticCompositionLocalOf<SumiType> { error("Wrap your UI in SumiTheme { }") }

/**
 * Provides Sumi's tokens to everything inside it. Read them with
 * `SumiTheme.colors` and `SumiTheme.type`.
 *
 * Changing [edition] cross-fades every color over [Motion.SWAP_MS].
 */
@Composable
fun SumiTheme(edition: Edition = Edition.Ember, content: @Composable () -> Unit) {
    val t = edition.colors
    val colors = SumiColors(
        paper = swap(t.paper),
        paperRaised = swap(t.paperRaised),
        ink = swap(t.ink),
        inkDim = swap(t.inkDim),
        inkMuted = swap(t.inkMuted),
        inkFaint = swap(t.inkFaint),
        signal = swap(t.signal),
        onSignal = swap(t.onSignal),
    )

    CompositionLocalProvider(
        LocalSumiColors provides colors,
        LocalSumiType provides sumiType(),
        LocalTextSelectionColors provides TextSelectionColors(colors.signal, colors.signal.copy(alpha = 0.3f)),
        content = content,
    )
}

@Composable
private fun swap(target: Color): Color =
    animateColorAsState(target, tween(Motion.SWAP_MS, easing = Motion.easeOut)).value

object SumiTheme {
    val colors: SumiColors
        @Composable @ReadOnlyComposable get() = LocalSumiColors.current
    val type: SumiType
        @Composable @ReadOnlyComposable get() = LocalSumiType.current
}
