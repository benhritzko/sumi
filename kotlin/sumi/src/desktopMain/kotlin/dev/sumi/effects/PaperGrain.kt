package dev.sumi.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

/**
 * A paper texture drawn over the content: fine grain, faint fibers and a slow
 * mottle, as if the ink were printed on real stock.
 *
 * It's an overlay, not a background, so text picks up the texture too. Positive
 * values lighten toward [ink], negative values darken toward black, so it works
 * on any edition.
 *
 * Desktop only: it runs an SkSL shader through Skia, which Compose Desktop uses
 * to draw. On Android 13+ the same SkSL would run through RuntimeShader.
 */
fun Modifier.paperGrain(ink: Color, strength: Float = 0.05f): Modifier = drawWithCache {
    val shader = RuntimeShaderBuilder(effect).apply {
        uniform("ink", ink.red, ink.green, ink.blue)
        uniform("strength", strength)
    }.makeShader()
    val brush = ShaderBrush(shader)
    onDrawWithContent {
        drawContent()
        drawRect(brush)
    }
}

private val effect = RuntimeEffect.makeForShader(
    """
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

    half4 main(float2 xy) {
        // 1. Tooth: per-pixel grain.
        float grain = hash(floor(xy)) - 0.5;

        // 2. Fibers: noise stretched along two angles, kept only at its peaks
        //    so it reads as sparse thin strands rather than streaks.
        float2 a = float2(xy.x * 0.80 + xy.y * 0.60, -xy.x * 0.60 + xy.y * 0.80);
        float2 b = float2(xy.x * 0.94 - xy.y * 0.34,  xy.x * 0.34 + xy.y * 0.94);
        float fibers = smoothstep(0.78, 0.98, noise(a * float2(0.012, 0.45)))
                     + smoothstep(0.80, 0.98, noise(b * float2(0.010, 0.40) + 17.0));

        // 3. Mottle: slow, uneven density across the sheet.
        float mottle = noise(xy * 0.004) * 0.6 + noise(xy * 0.011 + 31.0) * 0.4 - 0.5;

        float v = (grain * 0.9 + fibers * 0.7 + mottle * 0.8) * strength;

        // Premultiplied output: lighten toward ink, or darken toward black.
        if (v > 0.0) return half4(half3(ink) * v, v);
        return half4(0, 0, 0, -v);
    }
    """.trimIndent()
)
