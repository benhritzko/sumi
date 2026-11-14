package dev.sumi.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import dev.sumi.SumiTheme

/** Text with Sumi defaults: body style in dim ink. */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = SumiTheme.type.body,
    color: Color = SumiTheme.colors.inkDim,
) {
    BasicText(text, modifier, style.copy(color = color))
}

/** Small uppercase, widely tracked label that sits above a heading. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, SumiTheme.type.eyebrow, SumiTheme.colors.inkMuted)
}
