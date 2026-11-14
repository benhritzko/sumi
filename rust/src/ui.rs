//! A tiny immediate-mode UI layer on top of raylib.
//!
//! "Immediate mode" means there are no widget objects: every frame you call
//! `ui.button(...)` again, it draws itself and tells you if it was clicked.
//! The only state that survives between frames lives in `Anim`, keyed by id.

use std::collections::HashMap;

use raylib::prelude::*;

use crate::tokens::{mix, motion, shape, with_alpha, Colors};
use crate::typography::{self, Fonts, TextStyle};

/// Input sampled once per frame, before drawing starts. (raylib's draw handle
/// borrows the main handle mutably, so we copy what we need out first.)
pub struct Input {
    pub mouse: Vector2,
    pub down: bool,
    pub clicked: bool,
    /// Mouse wheel movement this frame (positive = scrolled up).
    pub wheel: f32,
    pub dt: f32,
}

impl Input {
    pub fn read(rl: &RaylibHandle) -> Input {
        Input {
            mouse: rl.get_mouse_position(),
            down: rl.is_mouse_button_down(MouseButton::MOUSE_BUTTON_LEFT),
            clicked: rl.is_mouse_button_released(MouseButton::MOUSE_BUTTON_LEFT),
            wheel: rl.get_mouse_wheel_move(),
            dt: rl.get_frame_time(),
        }
    }
}

/// Per-widget animation progress (0.0..=1.0), persisted across frames.
#[derive(Default)]
pub struct Anim(HashMap<&'static str, f32>);

impl Anim {
    /// Move the value for `id` toward `target`, taking `duration` seconds for a full 0→1 trip.
    fn approach(&mut self, id: &'static str, target: f32, duration: f32, dt: f32) -> f32 {
        let v = self.0.entry(id).or_insert(target);
        let step = dt / duration;
        *v = if *v < target { (*v + step).min(target) } else { (*v - step).max(target) };
        *v
    }
}

#[derive(Clone, Copy, PartialEq)]
pub enum Variant {
    Solid,
    Outline,
}

pub struct Ui<'a, D: RaylibDraw> {
    pub d: &'a mut D,
    pub fonts: &'a Fonts,
    pub c: Colors,
    pub input: &'a Input,
    pub anim: &'a mut Anim,
}

impl<'a, D: RaylibDraw> Ui<'a, D> {
    /// Draw text with its top-left at (x, y). Returns its size.
    pub fn text(&mut self, style: TextStyle, text: &str, x: f32, y: f32, color: Color) -> Vector2 {
        let s = typography::prepare(style, text);
        let font = self.fonts.get(style.face);
        self.d.draw_text_ex(font, &s, Vector2::new(x, y), style.size, style.tracking * style.size, color);
        self.fonts.measure(style, text)
    }

    pub fn eyebrow(&mut self, text: &str, x: f32, y: f32) -> Vector2 {
        let color = self.c.ink_muted;
        self.text(typography::EYEBROW, text, x, y, color)
    }

    pub fn rect(&mut self, r: Rectangle, color: Color) {
        self.d.draw_rectangle_rec(r, color);
    }

    pub fn hairline(&mut self, x: f32, y: f32, width: f32) {
        let color = self.c.line_soft();
        self.d.draw_rectangle_rec(Rectangle::new(x, y, width, shape::STROKE), color);
    }

    /// Returns (clicked, width) so callers can lay out the next widget.
    pub fn button(&mut self, id: &'static str, label: &str, x: f32, y: f32, variant: Variant) -> (bool, f32) {
        let (pad_x, pad_y) = (18.0, 10.0);
        let size = self.fonts.measure(typography::LABEL, label);
        let r = Rectangle::new(x, y, size.x + pad_x * 2.0, typography::LABEL.size + pad_y * 2.0);

        let hovered = r.check_collision_point_rec(self.input.mouse);
        let pressed = hovered && self.input.down;
        let h = self.anim.approach(id, if hovered { 1.0 } else { 0.0 }, motion::SNAP, self.input.dt);
        let h = motion::ease_out(h);

        let c = self.c;
        let (fill, border, ink) = match variant {
            Variant::Solid => {
                let fill = if pressed { c.bone_pressed() } else { mix(c.bone(), c.bone_hover(), h) };
                (fill, c.bone(), c.on_bone())
            }
            Variant::Outline => {
                let fill = if pressed { c.tint10() } else { mix(with_alpha(c.ink, 0.0), c.tint5(), h) };
                (fill, mix(c.line(), c.line_strong(), h), c.ink)
            }
        };

        self.d.draw_rectangle_rec(r, fill);
        self.d.draw_rectangle_lines_ex(r, shape::STROKE, border);
        self.text(typography::LABEL, label, x + pad_x, y + pad_y, ink);

        (hovered && self.input.clicked, r.width)
    }
}
