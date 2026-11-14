package dev.sumi.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Tier 2: semantic color tokens.
 *
 * Named by job, not by hue. Components only ever read these, so swapping the
 * [Edition] re-themes everything without touching component code.
 */
@Immutable
data class SumiColors(
    /** Page background. */
    val paper: Color,
    /** Cards, panels, anything sitting one step above the page. */
    val paperRaised: Color,

    /** Primary text and the solid button fill. */
    val ink: Color,
    /** Body copy. */
    val inkDim: Color,
    /** Secondary text: eyebrows, labels, notes. */
    val inkMuted: Color,
    /** Least important text: group labels, disabled. */
    val inkFaint: Color,

    /** The one accent color. Use sparingly: highlights, active state, stars. */
    val signal: Color,
    /** Text drawn on top of [signal]. */
    val onSignal: Color,
) {
    // Lines and tints are ink at a fixed opacity, so they follow the edition automatically.
    val lineSoft: Color get() = ink.copy(alpha = 0.13f)
    val line: Color get() = ink.copy(alpha = 0.26f)
    val lineStrong: Color get() = ink.copy(alpha = 0.42f)

    val tint5: Color get() = ink.copy(alpha = 0.05f)
    val tint10: Color get() = ink.copy(alpha = 0.10f)
    val tint16: Color get() = ink.copy(alpha = 0.16f)

    // The solid button is "bone": an ink-colored fill with paper-colored text.
    val bone: Color get() = ink
    val boneHover: Color get() = inkDim
    val bonePressed: Color get() = inkMuted
    val onBone: Color get() = paper
}

/** Theme variants. Each one maps the semantic tokens to a different primitive family. */
enum class Edition(val colors: SumiColors) {
    Ember(
        SumiColors(
            paper = Palette.EmberPaper, paperRaised = Palette.EmberPaperRaised,
            ink = Palette.EmberInk, inkDim = Palette.EmberInkDim,
            inkMuted = Palette.EmberInkMuted, inkFaint = Palette.EmberInkFaint,
            signal = Palette.EmberSignal, onSignal = Palette.EmberPaper,
        )
    ),
    Moss(
        SumiColors(
            paper = Palette.MossPaper, paperRaised = Palette.MossPaperRaised,
            ink = Palette.MossInk, inkDim = Palette.MossInkDim,
            inkMuted = Palette.MossInkMuted, inkFaint = Palette.MossInkFaint,
            signal = Palette.MossSignal, onSignal = Palette.MossPaper,
        )
    ),
    Tide(
        SumiColors(
            paper = Palette.TidePaper, paperRaised = Palette.TidePaperRaised,
            ink = Palette.TideInk, inkDim = Palette.TideInkDim,
            inkMuted = Palette.TideInkMuted, inkFaint = Palette.TideInkFaint,
            signal = Palette.TideSignal, onSignal = Palette.TidePaper,
        )
    ),
    Dusk(
        SumiColors(
            paper = Palette.DuskPaper, paperRaised = Palette.DuskPaperRaised,
            ink = Palette.DuskInk, inkDim = Palette.DuskInkDim,
            inkMuted = Palette.DuskInkMuted, inkFaint = Palette.DuskInkFaint,
            signal = Palette.DuskSignal, onSignal = Palette.DuskPaper,
        )
    ),
}
