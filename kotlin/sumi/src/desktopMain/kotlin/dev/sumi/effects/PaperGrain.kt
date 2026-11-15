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

/** Shared helpers: hashing, value noise, rotation, and the lighten/darken output. */
private const val COMMON = """
uniform float3 ink;
uniform float strength;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// Smooth value noise, 0..1.
float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1, 0)), u.x),
               mix(hash(i + float2(0, 1)), hash(i + float2(1, 1)), u.x), u.y);
}

float2 rot(float2 p, float a) {
    float c = cos(a), s = sin(a);
    return float2(c * p.x - s * p.y, s * p.x + c * p.y);
}

// Premultiplied output: v > 0 lightens toward ink, v < 0 darkens toward black.
half4 tone(float v) {
    v *= strength;
    if (v > 0.0) return half4(half3(ink) * v, v);
    return half4(0, 0, 0, -v);
}
"""

private const val FINE = """
half4 main(float2 xy) {
    float grain = hash(floor(xy)) - 0.5;
    // Noise stretched along two angles, kept only at its peaks: sparse thin strands.
    float fibers = smoothstep(0.78, 0.98, noise(rot(xy, 0.64) * float2(0.012, 0.45)))
                 + smoothstep(0.80, 0.98, noise(rot(xy, -0.35) * float2(0.010, 0.40) + 17.0));
    float mottle = noise(xy * 0.004) * 0.6 + noise(xy * 0.011 + 31.0) * 0.4 - 0.5;
    return tone(grain * 0.9 + fibers * 0.7 + mottle * 0.8);
}
"""

private const val WASHI = """
half4 main(float2 xy) {
    float grain = hash(floor(xy)) - 0.5;
    // Longer, more numerous fibers at three angles.
    float f = smoothstep(0.80, 0.97, noise(rot(xy, 0.5) * float2(0.006, 0.50)))
            + smoothstep(0.82, 0.97, noise(rot(xy, -0.9) * float2(0.005, 0.45) + 11.0))
            + smoothstep(0.84, 0.98, noise(rot(xy, 2.1) * float2(0.007, 0.55) + 23.0));
    // Soft clumps of pulp, and big cloudy density changes.
    float clumps = smoothstep(0.55, 0.90, noise(xy * 0.02 + 5.0)) * 0.5;
    float mottle = noise(xy * 0.003) * 0.7 + noise(xy * 0.009 + 31.0) * 0.3 - 0.5;
    return tone(grain * 0.4 + f + clumps * 0.6 + mottle * 1.2);
}
"""

private const val COLD_PRESS = """
// Height of the paper surface at p.
float height(float2 p) {
    return noise(p * 0.09) * 0.55 + noise(p * 0.23 + 7.0) * 0.30 + noise(p * 0.6 + 13.0) * 0.15;
}

half4 main(float2 xy) {
    // Slope of the surface, then light it from the top left: facing the light
    // lightens, facing away darkens. That reads as an embossed tooth.
    float dx = height(xy + float2(1, 0)) - height(xy - float2(1, 0));
    float dy = height(xy + float2(0, 1)) - height(xy - float2(0, 1));
    float light = -(dx + dy) * 4.2;
    float grain = hash(floor(xy)) - 0.5;
    float mottle = noise(xy * 0.004) - 0.5;
    return tone(light + grain * 0.5 + mottle * 0.6);
}
"""

private val effects: Map<PaperStyle, RuntimeEffect> = mapOf(
    PaperStyle.Fine to FINE,
    PaperStyle.Washi to WASHI,
    PaperStyle.ColdPress to COLD_PRESS,
).mapValues { (_, main) -> RuntimeEffect.makeForShader(COMMON + main) }
