package dev.sumi.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

/** Paper stocks. Each one is its own SkSL shader; [strength] is the default intensity. */
enum class PaperStyle(val label: String, val strength: Float) {
    /** Fine grain, a few faint fibers, slow mottle. Quiet enough for any screen. */
    Fine("Fine", 0.05f),
    /** Japanese mulberry paper: long visible fibers at many angles, cloudy pulp. */
    Washi("Washi", 0.06f),
    /** Watercolor stock: an embossed tooth, lit from the top left. */
    ColdPress("Cold press", 0.08f),
}

/**
 * A paper texture drawn over the content, as if the ink were printed on real stock.
 *
 * It's an overlay, not a background, so text picks up the texture too. Positive
 * values lighten toward [ink], negative values darken toward black, so it works
 * on any edition.
 *
 * Desktop only: it runs SkSL through Skia, which Compose Desktop draws with.
 */
fun Modifier.paperGrain(
    ink: Color,
    style: PaperStyle = PaperStyle.Fine,
    strength: Float = style.strength,
): Modifier = drawWithCache {
    val shader = RuntimeShaderBuilder(effects.getValue(style)).apply {
        uniform("ink", ink.red, ink.green, ink.blue)
        uniform("strength", strength)
    }.makeShader()
    val brush = ShaderBrush(shader)
    onDrawWithContent {
        drawContent()
        drawRect(brush)
    }
}

// ---------------------------------------------------------------- SkSL

private const val UNIFORMS = """
uniform float3 ink;
uniform float strength;

// Premultiplied output: v > 0 lightens toward ink, v < 0 darkens toward black.
half4 tone(float v) {
    v *= strength;
    if (v > 0.0) return half4(half3(ink) * v, v);
    return half4(0, 0, 0, -v);
}

// Grain whose density drifts across the sheet instead of being uniform static.
float tooth(float2 xy, float scale) {
    float g = hash(floor(xy / scale)) - 0.5;
    return g * (0.45 + 1.1 * noise(xy * 0.02 + 9.0));
}
"""

private const val FINE = """
half4 main(float2 xy) {
    float grain = tooth(xy, 1.0) * 0.8 + tooth(xy + 50.0, 2.0) * 0.4;
    // Short fibers, each with its own angle and length; a few light, a few dark.
    float light = segments(xy, 70.0, 8.0, 46.0, 0.6, 0.45, 0.0);
    float dark  = segments(xy, 90.0, 6.0, 30.0, 0.5, 0.30, 40.0);
    float mottle = warped(xy * 0.0035) - 0.5;
    return tone(grain + light * 0.8 - dark * 0.5 + mottle * 1.1);
}
"""

private const val WASHI = """
half4 main(float2 xy) {
    float grain = tooth(xy, 1.0) * 0.35;
    // Long mulberry fibers at every angle, plus a finer layer underneath.
    float longF = segments(xy, 110.0, 60.0, 200.0, 0.9, 0.65, 1.0);
    float midF  = segments(xy, 55.0, 18.0, 80.0, 0.6, 0.55, 2.0);
    float fine  = segments(xy, 28.0, 6.0, 30.0, 0.45, 0.50, 3.0);
    // Clumps of pulp, then large cloudy differences in thickness.
    float clumps = smoothstep(0.55, 0.85, warped(xy * 0.012 + 5.0));
    float mottle = warped(xy * 0.0025) - 0.5;
    return tone(grain + longF * 1.1 + midF * 0.7 + fine * 0.45 + clumps * 0.55 + mottle * 1.5);
}
"""

private const val COLD_PRESS = """
// Height of the paper surface: warped noise, so the tooth has no grid.
float height(float2 p) {
    float2 q = float2(noise(p * 0.03), noise(p * 0.03 + 7.7)) * 14.0;
    return fbm(p * 0.075 + q * 0.075) * 0.75 + noise(p * 0.45 + 13.0) * 0.25;
}

half4 main(float2 xy) {
    // Slope of the surface, lit from the top left: facing the light lightens,
    // facing away darkens. That reads as an embossed tooth.
    float dx = height(xy + float2(1, 0)) - height(xy - float2(1, 0));
    float dy = height(xy + float2(0, 1)) - height(xy - float2(0, 1));
    float light = -(dx + dy) * 5.0;
    float grain = tooth(xy, 1.0) * 0.5;
    float mottle = warped(xy * 0.003) - 0.5;
    return tone(light + grain + mottle * 0.9);
}
"""

private val effects: Map<PaperStyle, RuntimeEffect> = mapOf(
    PaperStyle.Fine to FINE,
    PaperStyle.Washi to WASHI,
    PaperStyle.ColdPress to COLD_PRESS,
).mapValues { (_, main) -> RuntimeEffect.makeForShader(SKSL_LIB + UNIFORMS + main) }
