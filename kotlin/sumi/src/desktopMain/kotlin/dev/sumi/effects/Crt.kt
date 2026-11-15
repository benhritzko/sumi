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

private val scanlines = RuntimeEffect.makeForShader(
    """
    uniform float2 size;

    half4 main(float2 xy) {
        // A sine with a 3px period: one dark band per three pixel rows.
        float line = 0.5 + 0.5 * sin(xy.y * 6.2832 / 3.0);
        float a = line * 0.28;
        // Darken toward the corners, like the edge of a tube.
        float2 c = xy / size * 2.0 - 1.0;
        float vig = smoothstep(0.6, 1.6, length(c)) * 0.45;
        a = a + vig - a * vig;
        return half4(0, 0, 0, a);
    }
    """.trimIndent()
)

private val grille = RuntimeEffect.makeForShader(
    """
    uniform float2 size;

    // Output is a multiplier, applied with BlendMode.Multiply: 1 keeps the color, 0 kills it.
    half4 main(float2 xy) {
        float col = mod(floor(xy.x), 3.0);
        half3 mask = col < 1.0 ? half3(1.0, 0.7, 0.7)
                   : col < 2.0 ? half3(0.7, 1.0, 0.7)
                   :             half3(0.7, 0.7, 1.0);
        float scan = 0.82 + 0.18 * sin(xy.y * 6.2832 / 3.0);
        float2 c = xy / size * 2.0 - 1.0;
        float vig = 1.0 - 0.35 * smoothstep(0.5, 1.5, dot(c, c));
        return half4(mask * scan * vig, 1.0);
    }
    """.trimIndent()
)

private val curved = RuntimeEffect.makeForShader(
    """
    uniform shader content;
    uniform float2 size;

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

        // Color fringing: red and blue sampled a little apart.
        float2 p = (c * 0.5 + 0.5) * size;
        float2 ca = float2(1.2, 0.0) * (0.4 + length(c));
        half3 col = half3(content.eval(p + ca).r, content.eval(p).g, content.eval(p - ca).b);

        float scan = 0.86 + 0.14 * sin(xy.y * 6.2832 / 3.0);
        float vig = 1.0 - 0.45 * smoothstep(0.3, 1.8, dot(c, c));
        return half4(col * scan * vig * edge, 1.0);
    }
    """.trimIndent()
)
