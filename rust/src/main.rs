// Design-system modules define more tokens than the playground uses; that's expected.
#[allow(dead_code)]
mod tokens;
#[allow(dead_code)]
mod typography;
mod ui;

use raylib::prelude::*;

use tokens::{motion, space, Colors, Edition};
use typography as ty;
use ui::{Anim, Input, Ui, Variant};

fn main() {
    let (mut rl, thread) = raylib::init()
        .size(1100, 860)
        .title("Sumi playground (rust)")
        .resizable()
        .msaa_4x()
        .build();
    rl.set_target_fps(120);

    let fonts = ty::Fonts::load(&mut rl, &thread);
    let mut anim = Anim::default();

    // Edition cross-fade: blend from `from` to `edition.colors()` as `fade` goes 0 → 1.
    let mut edition = Edition::Ember;
    let mut from = edition.colors();
    let mut fade = 1.0_f32;

    let keys = [KeyboardKey::KEY_ONE, KeyboardKey::KEY_TWO, KeyboardKey::KEY_THREE, KeyboardKey::KEY_FOUR];

    while !rl.window_should_close() {
        let input = Input::read(&rl);
        fade = (fade + input.dt / motion::SWAP).min(1.0);
        let colors = Colors::lerp(&from, &edition.colors(), motion::ease_out(fade));

        // Anything that wants to change state during drawing writes here; applied after the frame.
        let mut next_edition = keys
            .iter()
            .position(|k| rl.is_key_pressed(*k))
            .map(|i| Edition::ALL[i]);

        let mut d = rl.begin_drawing(&thread);
        d.clear_background(colors.paper);

        let mut ui = Ui { d: &mut d, fonts: &fonts, c: colors, input: &input, anim: &mut anim };
        let x = space::S7;
        let mut y = space::S7;

        // Header
        y += ui.eyebrow("Sumi · design system · v0.1", x, y).y + space::S3;
        y += ui.text(ty::DISPLAY, "Sumi", x, y, colors.ink).y + space::S2;
        y += ui.text(ty::BODY, "ink on paper, for the sake of clarity", x, y, colors.ink_dim).y;

        // Edition
        y = section(&mut ui, "Edition  (keys 1-4)", x, y);
        let mut bx = x;
        for (i, e) in Edition::ALL.iter().enumerate() {
            let id = ["ed0", "ed1", "ed2", "ed3"][i];
            let variant = if *e == edition { Variant::Solid } else { Variant::Outline };
            let (clicked, w) = ui.button(id, e.name(), bx, y, variant);
            if clicked {
                next_edition = Some(*e);
            }
            bx += w + space::S2;
        }
        y += 40.0;

        // Color
        y = section(&mut ui, "Color", x, y);
        let swatches = [
            ("paper", colors.paper),
            ("paperRaised", colors.paper_raised),
            ("ink", colors.ink),
            ("inkDim", colors.ink_dim),
            ("inkMuted", colors.ink_muted),
            ("inkFaint", colors.ink_faint),
            ("line", colors.line()),
            ("signal", colors.signal),
        ];
        for (i, (name, c)) in swatches.iter().enumerate() {
            let sx = x + i as f32 * (72.0 + space::S4);
            let r = Rectangle::new(sx, y, 72.0, 72.0);
            ui.rect(r, *c);
            ui.d.draw_rectangle_lines_ex(r, tokens::shape::STROKE, colors.line());
            ui.text(ty::CAPTION, name, sx, y + 72.0 + space::S2, colors.ink_muted);
        }
        y += 72.0 + space::S2 + 17.0;

        // Type
        y = section(&mut ui, "Type", x, y);
        let specimens = [
            ("display", ty::DISPLAY, "Power & beauty"),
            ("headline", ty::HEADLINE, "A whole system"),
            ("title", ty::TITLE, "Change anything. Rebuild nothing."),
            ("body", ty::BODY, "Body copy sits in dim ink so headings can carry the weight."),
            ("caption", ty::CAPTION, "Notes, footnotes, fine print."),
            ("label", ty::LABEL, "Install · Docs · Source"),
            ("eyebrow", ty::EYEBROW, "Stable · Experimental"),
            ("mono", ty::MONO, "cargo run --release"),
        ];
        for (name, style, sample) in specimens {
            let h = ui.text(style, sample, x + 120.0, y, colors.ink).y;
            ui.text(ty::MONO, name, x, y + (h - ty::MONO.size) / 2.0, colors.ink_faint);
            y += h + space::S2;
        }

        // Space
        y = section(&mut ui, "Space", x, y);
        for (name, px) in space::ALL {
            ui.text(ty::MONO, &format!("{name}  {px}"), x, y, colors.ink_faint);
            ui.rect(Rectangle::new(x + 120.0, y + 2.0, px, space::S3), colors.signal);
            y += space::S3 + space::S2;
        }

        // Button
        y = section(&mut ui, "Button", x, y);
        let (_, w) = ui.button("b1", "Install", x, y, Variant::Solid);
        ui.button("b2", "Docs", x + w + space::S3, y, Variant::Outline);

        drop(d);
        if let Some(e) = next_edition.filter(|e| *e != edition) {
            from = colors; // start the fade from whatever is on screen right now
            edition = e;
            fade = 0.0;
        }
    }
}

/// Hairline + eyebrow heading. Returns the y where section content starts.
fn section<D: RaylibDraw>(ui: &mut Ui<D>, title: &str, x: f32, y: f32) -> f32 {
    let y = y + space::S6;
    ui.hairline(x, y, 720.0);
    let y = y + space::S4;
    let h = ui.eyebrow(title, x, y).y;
    y + h + space::S4
}
