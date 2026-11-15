package dev.sumi.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

enum class CrtStyle(val label: String) {
    /** Dark lines every 3px and a soft vignette. Drawn over the content. */
    Scanlines("Scanlines"),
    /** Vertical red/green/blue phosphor stripes plus scanlines, multiplied into the content. */
    ApertureGrille("Grille"),
    /** Bulging glass: barrel distortion, color fringing, rounded corners. Re-samples the content. */
    Curved("Curved"),
}

/**
 * A CRT monitor look. The first two styles are overlays drawn on top; [CrtStyle.Curved]
 * is a render effect that reads the finished content back and warps it, so it is the
 * most expensive of the three.
 *
 * Desktop only, like [paperGrain].
 */
fun Modifier.crt(style: CrtStyle): Modifier = when (style) {
    CrtStyle.Scanlines -> overlay(scanlines, BlendMode.SrcOver)
    CrtStyle.ApertureGrille -> overlay(grille, BlendMode.Multiply)
    CrtStyle.Curved -> graphicsLayer {
        val builder = RuntimeShaderBuilder(curved).apply { uniform("size", size.width, size.height) }
        renderEffect = ImageFilter.makeRuntimeShader(builder, "content", null).asComposeRenderEffect()
        clip = true
    }
}

private fun Modifier.overlay(effect: RuntimeEffect, blend: BlendMode): Modifier = drawWithCache {
    val shader = RuntimeShaderBuilder(effect).apply { uniform("size", size.width, size.height) }.makeShader()
    val brush = ShaderBrush(shader)
    onDrawWithContent {
        drawContent()
        drawRect(brush, blendMode = blend)
    }
}

// ---------------------------------------------------------------- SkSL

// Tube imperfections shared by all three: no two scanlines, regions or pixels are
// quite the same, which is what sells it.
private const val TUBE = """
uniform float2 size;

// Each scanline gets its own strength, and a slow "hum bar" drifts down the screen.
float scanline(float y) {
    float row = floor(y / 3.0);
    float wave = 0.5 + 0.5 * sin(y * 6.2832 / 3.0);
    float jitter = 0.8 + 0.4 * hash(float2(row, 17.0));
    float hum = 0.85 + 0.3 * noise(float2(y * 0.004, 3.0));
    return wave * jitter * hum;
}

// Uneven tube brightness: bright center, irregular falloff, a darker smudge or two.
float tubeLight(float2 xy) {
    float2 c = xy / size * 2.0 - 1.0;
    float r = length(c * float2(1.0, 1.1)) + (fbm(xy * 0.004) - 0.5) * 0.35;
    return 1.0 - smoothstep(0.55, 1.55, r) * 0.6 - smoothstep(0.6, 0.85, warped(xy * 0.002 + 4.0)) * 0.08;
}

// Static: per-pixel snow, clumped so it isn't a perfect uniform hiss.
float snow(float2 xy) {
    return (hash(floor(xy) + 0.37) - 0.5) * (0.5 + noise(xy * 0.05));
}
"""

private val scanlines = RuntimeEffect.makeForShader(
    SKSL_LIB + TUBE + """
    half4 main(float2 xy) {
        float a = scanline(xy.y) * 0.26;
        float dark = 1.0 - tubeLight(xy);
        a = a + dark - a * dark;
        float s = snow(xy) * 0.09;
        // Snow lightens or darkens, on top of the line darkening.
        half4 o = half4(0, 0, 0, a);
        if (s > 0.0) return half4(half3(s), s) + o * (1.0 - s);
        return half4(0, 0, 0, a - s * (1.0 - a));
    }
    """.trimIndent()
)

private val grille = RuntimeEffect.makeForShader(
    SKSL_LIB + TUBE + """
    // Output is a multiplier, applied with BlendMode.Multiply: 1 keeps the color, 0 kills it.
    half4 main(float2 xy) {
        float col = mod(floor(xy.x), 3.0);
        // The phosphor mask is uneven: some regions show it strongly, some barely.
        float depth = 0.18 + 0.2 * fbm(xy * 0.006 + 2.0);
        half3 mask = col < 1.0 ? half3(1.0, 1.0 - depth, 1.0 - depth)
                   : col < 2.0 ? half3(1.0 - depth, 1.0, 1.0 - depth)
                   :             half3(1.0 - depth, 1.0 - depth, 1.0);
        float scan = 1.0 - scanline(xy.y) * 0.2;
        float light = tubeLight(xy) * (1.0 + snow(xy) * 0.12);
        return half4(mask * scan * light, 1.0);
    }
    """.trimIndent()
)

private val curved = RuntimeEffect.makeForShader(
    SKSL_LIB + TUBE + """
    uniform shader content;

    half4 main(float2 xy) {
        // Work in -1..1 so the center is (0,0).
        float2 c = xy / size * 2.0 - 1.0;
        // Barrel distortion: sample further out the further we are from the center,
        // which pushes the picture into a bulge.
        c *= 1.0 + 0.07 * dot(c, c);

        // Rounded-corner screen mask.
        float r = 0.06;
        float2 q = abs(c) - (1.0 - r);
        float edge = 1.0 - smoothstep(0.85, 1.0, length(max(q, 0.0)) / r);
        if (abs(c.x) > 1.0 || abs(c.y) > 1.0) return half4(0, 0, 0, 1);

        float2 p = (c * 0.5 + 0.5) * size;
        // Unstable horizontal sync: every line sits a little off, with the odd bigger slip.
        float row = floor(xy.y / 3.0);
        float slip = (noise(float2(xy.y * 0.03, 1.0)) - 0.5) * 1.6;
        slip += step(0.993, hash(float2(row, 5.0))) * (hash(float2(row, 6.0)) - 0.5) * 14.0;
        p.x += slip;

        // Color fringing that grows toward the edges, plus a soft glow from the phosphor.
        float2 ca = float2(1.0 + 0.6 * noise(xy * 0.01), 0.0) * (0.4 + length(c));
        half3 col = half3(content.eval(p + ca).r, content.eval(p).g, content.eval(p - ca).b);
        half3 glow = (content.eval(p + float2(3, 0)).rgb + content.eval(p - float2(3, 0)).rgb
                    + content.eval(p + float2(0, 3)).rgb + content.eval(p - float2(0, 3)).rgb) * 0.25;
        col = col + glow * 0.22;

        float scan = 1.0 - scanline(xy.y) * 0.16;
        float light = tubeLight(xy) * (1.0 + snow(xy) * 0.14);
        return half4(col * scan * light * edge, 1.0);
    }
    """.trimIndent()
)
