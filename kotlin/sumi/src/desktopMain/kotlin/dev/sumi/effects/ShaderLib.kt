package dev.sumi.effects

/**
 * SkSL helpers shared by every effect. Prepended to each shader's source.
 *
 * The rule for believable textures: never let one pattern repeat at one scale.
 * Everything here is either hashed per cell (so each fiber, spot or scratch is
 * its own random thing) or built from several rotated octaves of noise and then
 * domain-warped, so blotches have organic, uneven edges.
 */
internal const val SKSL_LIB = """
// Hash without sine: stable across GPUs and free of visible banding.
float hash(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// Smooth value noise, 0..1.
float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1, 0)), u.x),
               mix(hash(i + float2(0, 1)), hash(i + float2(1, 1)), u.x), u.y);
}

// Four octaves, each rotated so the noise grid never lines up. Roughly 0..1.
float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = float2(1.6 * p.x + 1.2 * p.y, -1.2 * p.x + 1.6 * p.y) + 3.7;
        a *= 0.5;
    }
    return v / 0.9375;
}

// fbm whose input is pushed around by more fbm: organic, uneven shapes.
float warped(float2 p) {
    float2 q = float2(fbm(p), fbm(p + float2(5.2, 1.3)));
    return fbm(p + 1.8 * q);
}

// Random line segments, one chance per grid cell, each with its own position,
// angle, length, slight curve and strength. Returns 0..1 coverage at p.
float segments(float2 p, float cell, float minLen, float maxLen, float width, float density, float s) {
    float2 g = floor(p / cell);
    float best = 0.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            float2 c = g + float2(float(i), float(j));
            if (hash(c + s) < density) {
                float2 o = (c + float2(hash(c + s + 1.7), hash(c + s + 3.1))) * cell;
                float a = hash(c + s + 5.3) * 6.2832;
                float len = mix(minLen, maxLen, hash(c + s + 7.9));
                float2 dir = float2(cos(a), sin(a));
                float2 nrm = float2(-dir.y, dir.x);
                float2 d = p - o;
                float along = dot(d, dir);
                float bend = (hash(c + s + 9.1) - 0.5) * 6.0 / max(len, 1.0);
                float across = dot(d, nrm) - bend * along * along / max(len, 1.0) * 4.0;
                float w = width * mix(0.6, 1.4, hash(c + s + 11.3));
                float body = 1.0 - smoothstep(0.0, w, abs(across));
                // Taper the ends, and let strength flicker along the length.
                float ends = 1.0 - smoothstep(len * 0.3, len * 0.5, abs(along));
                float breakup = 0.55 + 0.45 * noise(float2(along * 0.15, c.x + c.y * 7.0 + s));
                best = max(best, body * ends * breakup * mix(0.35, 1.0, hash(c + s + 13.7)));
            }
        }
    }
    return best;
}

// Random round-ish spots with ragged edges. Returns 0..1 coverage at p.
float spots(float2 p, float cell, float rMin, float rMax, float density, float s) {
    float2 g = floor(p / cell);
    float best = 0.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            float2 c = g + float2(float(i), float(j));
            if (hash(c + s) < density) {
                float2 o = (c + float2(hash(c + s + 1.3), hash(c + s + 2.7))) * cell;
                float r = mix(rMin, rMax, hash(c + s + 4.1));
                float d = length(p - o) + (noise(p * 0.35 + c * 3.0) - 0.5) * r * 0.9;
                float core = 1.0 - smoothstep(r * 0.4, r, d);
                float halo = (1.0 - smoothstep(r, r * 2.6, d)) * 0.25;
                best = max(best, (core + halo) * mix(0.4, 1.0, hash(c + s + 6.0)));
            }
        }
    }
    return best;
}

// Composite a flat color at coverage a over a premultiplied result so far.
half4 over(half4 dst, float3 c, float a) {
    a = clamp(a, 0.0, 1.0);
    return half4(half3(c) * a, a) + dst * (1.0 - a);
}
"""
