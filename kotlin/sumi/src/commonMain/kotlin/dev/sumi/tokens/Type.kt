package dev.sumi.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.sumi.resources.Res
import dev.sumi.resources.fraunces_regular
import dev.sumi.resources.space_grotesk_medium
import dev.sumi.resources.space_grotesk_regular
import dev.sumi.resources.space_mono_regular
import org.jetbrains.compose.resources.Font

/**
 * Three voices:
 *  - Serif (Fraunces) for big editorial moments. Regular weight, tight tracking.
 *  - Grotesk (Space Grotesk) for everything you read or click.
 *  - Mono (Space Mono) for code and commands.
 *
 * Small uppercase labels (eyebrow, label) carry wide tracking; Compose has no
 * text-transform, so components uppercase the string themselves.
 */
@Immutable
data class SumiType(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
    val eyebrow: TextStyle,
    val mono: TextStyle,
)

@Composable
fun sumiType(): SumiType {
    val serif = FontFamily(Font(Res.font.fraunces_regular, FontWeight.Normal))
    val grotesk = FontFamily(
        Font(Res.font.space_grotesk_regular, FontWeight.Normal),
        Font(Res.font.space_grotesk_medium, FontWeight.Medium),
    )
    val mono = FontFamily(Font(Res.font.space_mono_regular, FontWeight.Normal))

    return SumiType(
        display = TextStyle(fontFamily = serif, fontSize = 80.sp, lineHeight = 74.sp, letterSpacing = (-0.02).em),
        headline = TextStyle(fontFamily = serif, fontSize = 56.sp, lineHeight = 55.sp, letterSpacing = (-0.01).em),
        title = TextStyle(fontFamily = grotesk, fontWeight = FontWeight.Medium, fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em),
        body = TextStyle(fontFamily = grotesk, fontSize = 16.sp, lineHeight = 26.sp),
        bodySmall = TextStyle(fontFamily = grotesk, fontSize = 14.sp, lineHeight = 23.sp),
        caption = TextStyle(fontFamily = grotesk, fontSize = 12.sp, lineHeight = 17.sp),
        label = TextStyle(fontFamily = grotesk, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 18.sp, letterSpacing = 0.12.em),
        eyebrow = TextStyle(fontFamily = grotesk, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, lineHeight = 17.sp, letterSpacing = 0.22.em),
        mono = TextStyle(fontFamily = mono, fontSize = 12.sp, lineHeight = 17.sp),
    )
}
