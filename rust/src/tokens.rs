//! Design tokens. Mirrors `kotlin/sumi/src/commonMain/kotlin/dev/sumi/tokens`.
//!
//! Two tiers, same as the Kotlin side:
//!   1. Primitives: raw hex values, private to this module.
//!   2. Semantic: the `Colors` struct, which is what UI code reads.

use raylib::prelude::Color;

/// `const` so palettes can be built at compile time.
const fn hex(rgb: u32) -> Color {
    Color { r: (rgb >> 16) as u8, g: (rgb >> 8) as u8, b: rgb as u8, a: 255 }
}

// ---------------------------------------------------------------- semantic colors

#[derive(Clone, Copy, Debug)]
pub struct Colors {
    pub paper: Color,
    pub paper_raised: Color,
    pub ink: Color,
    pub ink_dim: Color,
    pub ink_muted: Color,
    pub ink_faint: Color,
    pub signal: Color,
    pub on_signal: Color,
}

impl Colors {
    // Lines and tints are ink at fixed opacities, so they follow the edition.
    pub fn line_soft(&self) -> Color { with_alpha(self.ink, 0.13) }
    pub fn line(&self) -> Color { with_alpha(self.ink, 0.26) }
    pub fn line_strong(&self) -> Color { with_alpha(self.ink, 0.42) }
    pub fn tint5(&self) -> Color { with_alpha(self.ink, 0.05) }
    pub fn tint10(&self) -> Color { with_alpha(self.ink, 0.10) }

    // Solid buttons are "bone": ink-colored fill, paper-colored text.
    pub fn bone(&self) -> Color { self.ink }
    pub fn bone_hover(&self) -> Color { self.ink_dim }
    pub fn bone_pressed(&self) -> Color { self.ink_muted }
    pub fn on_bone(&self) -> Color { self.paper }

    /// Blend every field from `a` to `b`. Used to cross-fade editions.
    pub fn lerp(a: &Colors, b: &Colors, t: f32) -> Colors {
        Colors {
            paper: mix(a.paper, b.paper, t),
            paper_raised: mix(a.paper_raised, b.paper_raised, t),
            ink: mix(a.ink, b.ink, t),
            ink_dim: mix(a.ink_dim, b.ink_dim, t),
            ink_muted: mix(a.ink_muted, b.ink_muted, t),
            ink_faint: mix(a.ink_faint, b.ink_faint, t),
            signal: mix(a.signal, b.signal, t),
            on_signal: mix(a.on_signal, b.on_signal, t),
        }
    }
}

// ---------------------------------------------------------------- editions (themes)

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Edition {
    Ember,
    Moss,
    Tide,
    Dusk,
}

impl Edition {
    pub const ALL: [Edition; 4] = [Edition::Ember, Edition::Moss, Edition::Tide, Edition::Dusk];

    pub fn name(self) -> &'static str {
        match self {
            Edition::Ember => "Ember",
            Edition::Moss => "Moss",
            Edition::Tide => "Tide",
            Edition::Dusk => "Dusk",
        }
    }

    pub fn colors(self) -> Colors {
        // (paper, raised, ink, dim, muted, faint, signal)
        let p = match self {
            Edition::Ember => [0x000000, 0x0A0A0A, 0xCDC4BA, 0xB0A9A0, 0x958F87, 0x807A73, 0xCE5E32],
            Edition::Moss => [0x031A0D, 0x082616, 0xC1CDC5, 0xA5B3AA, 0x8A9A90, 0x7A8B80, 0x3FA36D],
            Edition::Tide => [0x030F19, 0x081824, 0xBFC9D1, 0xA5AEB5, 0x8B949B, 0x7B848B, 0x6EA5CF],
            Edition::Dusk => [0x0B0C1B, 0x131527, 0xC4C6D2, 0xAAACB6, 0x90929D, 0x80828D, 0x9299D3],
        };
        Colors {
            paper: hex(p[0]),
            paper_raised: hex(p[1]),
            ink: hex(p[2]),
            ink_dim: hex(p[3]),
            ink_muted: hex(p[4]),
            ink_faint: hex(p[5]),
            signal: hex(p[6]),
            on_signal: hex(p[0]),
        }
    }
}

// ---------------------------------------------------------------- space, shape, motion

/// 4px base scale.
pub mod space {
    pub const S1: f32 = 4.0;
    pub const S2: f32 = 8.0;
    pub const S3: f32 = 12.0;
    pub const S4: f32 = 16.0;
    pub const S5: f32 = 24.0;
    pub const S6: f32 = 32.0;
    pub const S7: f32 = 48.0;
    pub const S8: f32 = 64.0;
    pub const ALL: [(&str, f32); 8] =
        [("s1", S1), ("s2", S2), ("s3", S3), ("s4", S4), ("s5", S5), ("s6", S6), ("s7", S7), ("s8", S8)];
}

pub mod shape {
    pub const RADIUS: f32 = 2.0;
    pub const STROKE: f32 = 1.0;
}

pub mod motion {
    /// Seconds.
    pub const SNAP: f32 = 0.090;
    pub const MOVE: f32 = 0.170;
    pub const SWAP: f32 = 0.210;

    /// CSS `cubic-bezier(.215,.61,.355,1)`: fast start, gentle stop.
    pub fn ease_out(t: f32) -> f32 {
        cubic_bezier(0.215, 0.61, 0.355, 1.0, t)
    }

    /// Evaluate a CSS-style cubic bezier easing curve at progress `x` (0..=1).
    /// The curve is parametric, so we solve for the parameter `t` whose x equals
    /// our input with a few Newton-Raphson steps, then return y(t).
    pub fn cubic_bezier(x1: f32, y1: f32, x2: f32, y2: f32, x: f32) -> f32 {
        let x = x.clamp(0.0, 1.0);
        let bez = |a: f32, b: f32, t: f32| {
            let u = 1.0 - t;
            3.0 * u * u * t * a + 3.0 * u * t * t * b + t * t * t
        };
        let d_bez = |a: f32, b: f32, t: f32| {
            let u = 1.0 - t;
            3.0 * u * u * a + 6.0 * u * t * (b - a) + 3.0 * t * t * (1.0 - b)
        };
        let mut t = x;
        for _ in 0..8 {
            let dx = d_bez(x1, x2, t);
            if dx.abs() < 1e-6 {
                break;
            }
            t = (t - (bez(x1, x2, t) - x) / dx).clamp(0.0, 1.0);
        }
        bez(y1, y2, t)
    }
}

// ---------------------------------------------------------------- color helpers

pub fn with_alpha(c: Color, alpha: f32) -> Color {
    Color { a: (alpha.clamp(0.0, 1.0) * 255.0).round() as u8, ..c }
}

pub fn mix(a: Color, b: Color, t: f32) -> Color {
    let l = |x: u8, y: u8| (x as f32 + (y as f32 - x as f32) * t).round() as u8;
    Color { r: l(a.r, b.r), g: l(a.g, b.g), b: l(a.b, b.b), a: l(a.a, b.a) }
}
