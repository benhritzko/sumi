//! Fonts and the type scale.

use raylib::prelude::*;

#[derive(Clone, Copy)]
pub enum Face {
    Serif,
    Grotesk,
    GroteskMedium,
    Mono,
}

/// A text style token. raylib draws single lines, so there's no line height here;
/// layout code decides vertical rhythm.
#[derive(Clone, Copy)]
pub struct TextStyle {
    pub face: Face,
    pub size: f32,
    /// Letter spacing in em (fraction of `size`), like CSS `letter-spacing: 0.12em`.
    pub tracking: f32,
    pub uppercase: bool,
}

const fn style(face: Face, size: f32, tracking: f32, uppercase: bool) -> TextStyle {
    TextStyle { face, size, tracking, uppercase }
}

pub const DISPLAY: TextStyle = style(Face::Serif, 80.0, -0.02, false);
pub const HEADLINE: TextStyle = style(Face::Serif, 56.0, -0.01, false);
pub const TITLE: TextStyle = style(Face::GroteskMedium, 19.0, -0.01, false);
pub const BODY: TextStyle = style(Face::Grotesk, 16.0, 0.0, false);
pub const BODY_SMALL: TextStyle = style(Face::Grotesk, 14.0, 0.0, false);
pub const CAPTION: TextStyle = style(Face::Grotesk, 12.0, 0.0, false);
pub const LABEL: TextStyle = style(Face::GroteskMedium, 11.0, 0.12, true);
pub const EYEBROW: TextStyle = style(Face::GroteskMedium, 10.5, 0.22, true);
pub const MONO: TextStyle = style(Face::Mono, 12.0, 0.0, false);

/// Every glyph we rasterize. raylib bakes fonts into a texture atlas at load time,
/// so characters outside this set render as '?'.
const CHARSET: &str = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~·—";

pub struct Fonts {
    serif: Font,
    grotesk: Font,
    grotesk_medium: Font,
    mono: Font,
}

impl Fonts {
    pub fn load(rl: &mut RaylibHandle, thread: &RaylibThread) -> Fonts {
        // Atlas size = the largest size we'll draw that face at. Drawing smaller
        // scales the atlas down; bilinear filtering keeps that reasonably smooth.
        let mut load = |file: &str, atlas_px: i32| {
            let path = format!("{}/assets/fonts/{}", env!("CARGO_MANIFEST_DIR"), file);
            let font = rl
                .load_font_ex(thread, &path, atlas_px, Some(CHARSET))
                .unwrap_or_else(|e| panic!("failed to load {path}: {e:?}"));
            font.texture().set_texture_filter(thread, TextureFilter::TEXTURE_FILTER_BILINEAR);
            font
        };
        Fonts {
            serif: load("Fraunces-Regular.ttf", 96),
            grotesk: load("SpaceGrotesk-Regular.ttf", 32),
            grotesk_medium: load("SpaceGrotesk-Medium.ttf", 32),
            mono: load("SpaceMono-Regular.ttf", 32),
        }
    }

    pub fn get(&self, face: Face) -> &Font {
        match face {
            Face::Serif => &self.serif,
            Face::Grotesk => &self.grotesk,
            Face::GroteskMedium => &self.grotesk_medium,
            Face::Mono => &self.mono,
        }
    }

    /// Width and height `text` will occupy when drawn in `style`.
    pub fn measure(&self, style: TextStyle, text: &str) -> Vector2 {
        let text = prepare(style, text);
        self.get(style.face).measure_text(&text, style.size, style.tracking * style.size)
    }
}

/// Apply text-transform. Returns an owned String because uppercasing allocates.
pub fn prepare(style: TextStyle, text: &str) -> String {
    if style.uppercase { text.to_uppercase() } else { text.to_string() }
}
