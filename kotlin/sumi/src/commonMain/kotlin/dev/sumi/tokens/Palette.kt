package dev.sumi.tokens

import androidx.compose.ui.graphics.Color

/**
 * Tier 1: primitive tokens.
 *
 * Raw color values with no meaning attached. Nothing outside the tokens package
 * should reference these directly; use [SumiColors] (semantic tokens) instead.
 *
 * Each "edition" is a hue family: a near-black paper, a warm or cool ink ramp,
 * and one signal color. Values derived from ryoku.dev.
 */
object Palette {
    // Ember (default): pure black paper, bone ink, burnt-orange signal
    val EmberPaper = Color(0xFF000000)
    val EmberPaperRaised = Color(0xFF0A0A0A)
    val EmberInk = Color(0xFFCDC4BA)
    val EmberInkDim = Color(0xFFB0A9A0)
    val EmberInkMuted = Color(0xFF958F87)
    val EmberInkFaint = Color(0xFF807A73)
    val EmberSignal = Color(0xFFCE5E32)

    // Moss: green-black paper, sage signal
    val MossPaper = Color(0xFF05110A)
    val MossPaperRaised = Color(0xFF0B1A12)
    val MossInk = Color(0xFFC1CAC4)
    val MossInkDim = Color(0xFFA6AFAA)
    val MossInkMuted = Color(0xFF8D9590)
    val MossInkFaint = Color(0xFF7C8580)
    val MossSignal = Color(0xFF6DAF8C)

    // Tide: blue-black paper, steel-blue signal
    val TidePaper = Color(0xFF030F19)
    val TidePaperRaised = Color(0xFF081824)
    val TideInk = Color(0xFFBFC9D1)
    val TideInkDim = Color(0xFFA5AEB5)
    val TideInkMuted = Color(0xFF8B949B)
    val TideInkFaint = Color(0xFF7B848B)
    val TideSignal = Color(0xFF6EA5CF)

    // Dusk: indigo-black paper, lavender signal
    val DuskPaper = Color(0xFF0B0C1B)
    val DuskPaperRaised = Color(0xFF131527)
    val DuskInk = Color(0xFFC4C6D2)
    val DuskInkDim = Color(0xFFAAACB6)
    val DuskInkMuted = Color(0xFF90929D)
    val DuskInkFaint = Color(0xFF80828D)
    val DuskSignal = Color(0xFF9299D3)
}
