package dev.sumi.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

enum class WearLevel(val label: String, val amount: Float) {
    Light("Light", 0.55f),
    Heavy("Heavy", 1.0f),
}

/**
 * Age and handling damage, like a manual that has lived on a workbench since 1984.
 *
 * Ink loss is faked by painting [paper] back over the content in speckled patches,
 * so it erases text and lines without needing to read them. On top of that:
 * scratches, foxing (small brown age spots), a faint yellowing, and, when asked,
 * chewed edges, rubbed corners and a fold crease.
 *
 * Use it once on the page (`edges = true, crease = true`) and on the odd section
 * with a different [seed] so each damaged patch is unique.
 *
 * Draw [paperGrain] *outside* this modifier so the worn patches get paper texture too.
 */
fun Modifier.wear(
    paper: Color,
    ink: Color,
    amount: Float = WearLevel.Light.amount,
    seed: Float = 0f,
    edges: Boolean = false,
    crease: Boolean = false,
): Modifier = drawWithCache {
    val shader = RuntimeShaderBuilder(effect).apply {
        uniform("size", size.width, size.height)
        uniform("paper", paper.red, paper.green, paper.blue)
        uniform("ink", ink.red, ink.green, ink.blue)
        uniform("seed", seed)
        uniform("amount", amount)
        uniform("edges", if (edges) 1f else 0f)
        uniform("crease", if (crease) 1f else 0f)
    }.makeShader()
    val brush = ShaderBrush(shader)
    onDrawWithContent {
        drawContent()
        drawRect(brush)
    }
}

private val effect = RuntimeEffect.makeForShader(
    SKSL_LIB + """
    uniform float2 size;
    uniform float3 paper;
    uniform float3 ink;
    uniform float seed;
    uniform float amount;
    uniform float edges;
    uniform float crease;

    half4 main(float2 xy) {
        float2 p = xy + seed * 173.0;
        float speck = hash(floor(xy) + seed * 7.0);
        half4 o = half4(0);

        // 1. Yellowing: an uneven warm cast, heavier in some regions. Page only;
        //    the page's own layer already covers every section.
        float age = warped(p * 0.002 + 11.0);
        o = over(o, float3(0.62, 0.48, 0.26), (0.02 + 0.05 * age) * amount * edges);

        // 2. Scuffs: patches where the ink has rubbed off. The patch shape is
        //    domain-warped noise; inside it the loss is speckled, never solid.
        float scuffField = warped(p * 0.0045);
        float scuff = smoothstep(0.70 - 0.14 * amount, 0.80 - 0.08 * amount, scuffField);
        float speckle = smoothstep(0.30, 0.85, speck * 0.55 + noise(p * 0.3) * 0.35 + noise(p * 0.06) * 0.3);
        o = over(o, paper, scuff * (0.25 + 0.7 * speckle));
        o = over(o, ink, scuff * 0.035);                    // rubbed surface catches light

        // 3. Pinholes: tiny isolated spots where ink didn't take or flaked off.
        float pin = step(1.0 - 0.0025 * amount, hash(floor(xy * 0.5) + seed + 3.3));
        o = over(o, paper, pin * 0.9);

        // 4. Scratches: sparse, long, thin, erase ink and catch a little light.
        float sc = segments(p, 160.0, 40.0, 240.0, 0.7, 0.22 * amount, 11.0);
        o = over(o, paper, sc * 0.85);
        o = over(o, ink, sc * 0.06);

        // 5. Foxing: brown age spots, clustered in a few regions. Mixed from the
        //    paper so they read as stains on dark stock, not glowing dots.
        float cluster = smoothstep(0.45, 0.7, fbm(p * 0.003 + 21.0));
        float fox = spots(p, 70.0, 1.5, 6.0, (0.05 + 0.3 * cluster) * amount, 23.0);
        float3 stain = mix(paper, float3(0.30, 0.20, 0.10), 0.55);
        o = over(o, stain, fox * 0.35);

        // 6. Edges: chewed, rubbed, dirty. The ragged width varies along the edge.
        if (edges > 0.5) {
            float d = min(min(xy.x, xy.y), min(size.x - xy.x, size.y - xy.y));
            float ragged = (6.0 + 34.0 * fbm(p * 0.012) + 12.0 * noise(p * 0.12)) * amount;
            float band = 1.0 - smoothstep(0.0, ragged, d);
            float chew = smoothstep(0.35, 0.75, band + (speck - 0.5) * 0.6);
            o = over(o, paper, chew * 0.85);              // ink worn off near the edge
            o = over(o, ink, band * 0.07);                // rubbed lighter
            float grime = 1.0 - smoothstep(0.0, ragged * 0.3, d);
            o = over(o, float3(0.0), grime * 0.6 * (0.5 + 0.5 * noise(p * 0.08)));

            // Corners take the most handling.
            float2 cd = min(xy, size - xy);
            float corner = 1.0 - smoothstep(0.0, (60.0 + 80.0 * hash(float2(seed, 9.0))) * amount, length(cd) + (noise(p * 0.05) - 0.5) * 40.0);
            o = over(o, paper, corner * (0.4 + 0.5 * speck));
            o = over(o, ink, corner * 0.06);
        }

        // 7. A fold crease running roughly top to bottom, wandering slightly.
        if (crease > 0.5) {
            float2 a = float2(size.x * (0.3 + 0.4 * hash(float2(seed, 1.0))), 0.0);
            float2 b = float2(size.x * (0.3 + 0.4 * hash(float2(seed, 2.0))), size.y);
            float2 dir = normalize(b - a);
            float2 n = float2(-dir.y, dir.x);
            float t = dot(xy - a, dir);
            float s = dot(xy - a, n) + (fbm(float2(t * 0.004, seed)) - 0.5) * 18.0;
            float ridge = exp(-s * s / 2.0);
            float shadow = smoothstep(0.0, 6.0, s) * (1.0 - smoothstep(6.0, 46.0, s));
            float cracked = smoothstep(0.35, 0.7, noise(float2(t * 0.06, seed + 3.0)) + speck * 0.25);
            o = over(o, paper, ridge * cracked * 0.95 * amount);   // ink cracked along the fold
            o = over(o, ink, ridge * 0.12 * amount);
            o = over(o, float3(0.0), shadow * 0.14 * amount);
        }

        // Without edge wear this is a patch on part of a page (a section), so fade
        // it out raggedly before the element's bounds: no visible rectangle.
        if (edges < 0.5) {
            float d = min(min(xy.x, xy.y), min(size.x - xy.x, size.y - xy.y));
            o *= smoothstep(0.0, 48.0, d + (fbm(p * 0.02) - 0.5) * 60.0);
        }
        return o;
    }
    """
)
