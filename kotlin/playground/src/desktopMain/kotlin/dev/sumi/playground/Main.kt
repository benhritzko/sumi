package dev.sumi.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.sumi.SumiTheme
import dev.sumi.components.Button
import dev.sumi.components.ButtonSize
import dev.sumi.components.ButtonVariant
import dev.sumi.components.Eyebrow
import dev.sumi.components.Text
import dev.sumi.effects.paperGrain
import dev.sumi.tokens.Edition
import dev.sumi.tokens.Shape
import dev.sumi.tokens.Space

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sumi playground",
        state = rememberWindowState(size = DpSize(1100.dp, 860.dp)),
    ) {
        var edition by remember { mutableStateOf(Edition.Moss) }
        SumiTheme(edition) {
            Playground(edition, onSelect = { edition = it })
        }
    }
}

@Composable
private fun Playground(selected: Edition, onSelect: (Edition) -> Unit) {
    val c = SumiTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.paper)
            .paperGrain(c.ink) // fixed to the window, drawn over everything
            .verticalScroll(rememberScrollState())
            .padding(Space.s7),
        verticalArrangement = Arrangement.spacedBy(Space.s7),
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
            Eyebrow("Sumi · design system · v0.1")
            Text("Sumi", style = SumiTheme.type.display, color = c.ink)
            Text("墨  ink on paper, for the sake of clarity", color = c.inkDim)
        }

        Section("Edition") {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                Edition.entries.forEach {
                    Button(
                        it.name,
                        onClick = { onSelect(it) },
                        variant = if (it == selected) ButtonVariant.Solid else ButtonVariant.Raised,
                    )
                }
            }
        }

        Section("Color") {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
                Swatch("paper", c.paper); Swatch("paperRaised", c.paperRaised)
                Swatch("ink", c.ink); Swatch("inkDim", c.inkDim)
                Swatch("inkMuted", c.inkMuted); Swatch("inkFaint", c.inkFaint)
                Swatch("line", c.line); Swatch("signal", c.signal)
            }
        }

        Section("Type") {
            val t = SumiTheme.type
            Specimen("display", t.display, "Power & beauty")
            Specimen("headline", t.headline, "A whole system")
            Specimen("title", t.title, "Change anything. Rebuild nothing.")
            Specimen("body", t.body, "Body copy sits in dim ink so headings can carry the weight.")
            Specimen("bodySmall", t.bodySmall, "Secondary paragraphs and values in a spec list.")
            Specimen("caption", t.caption, "Notes, footnotes, fine print.")
            Specimen("label", t.label, "INSTALL  ·  DOCS  ·  SOURCE")
            Specimen("eyebrow", t.eyebrow, "STABLE  ·  EXPERIMENTAL")
            Specimen("mono", t.mono, "sumi theme use moss")
        }

        Section("Space") {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                listOf(
                    "s1" to Space.s1, "s2" to Space.s2, "s3" to Space.s3, "s4" to Space.s4,
                    "s5" to Space.s5, "s6" to Space.s6, "s7" to Space.s7, "s8" to Space.s8,
                ).forEach { (name, dp) -> SpaceBar(name, dp) }
            }
        }

        Section("Button") {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s3), verticalAlignment = Alignment.CenterVertically) {
                Button("Install", onClick = {})
                Button("Docs", onClick = {}, variant = ButtonVariant.Outline)
                Button("Install Sumi", onClick = {}, size = ButtonSize.Lg)
                Button("Source", onClick = {}, variant = ButtonVariant.Outline, size = ButtonSize.Lg)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
        Box(Modifier.width(720.dp).height(Shape.stroke).background(SumiTheme.colors.lineSoft))
        Eyebrow(title)
        content()
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        Box(Modifier.size(72.dp).background(color, Shape.small).border(Shape.stroke, SumiTheme.colors.line, Shape.small))
        Text(name, style = SumiTheme.type.caption, color = SumiTheme.colors.inkMuted)
    }
}

@Composable
private fun Specimen(name: String, style: TextStyle, sample: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.width(120.dp), SumiTheme.type.mono, SumiTheme.colors.inkFaint)
        Text(sample, style = style, color = SumiTheme.colors.ink)
    }
}

@Composable
private fun SpaceBar(name: String, size: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$name  ${size.value.toInt()}", Modifier.width(120.dp), SumiTheme.type.mono, SumiTheme.colors.inkFaint)
        Box(Modifier.width(size).height(Space.s3).background(SumiTheme.colors.signal, Shape.small))
    }
}
