package com.emmav.monzo.widget.common

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ui.tooling.preview.Preview
import java.util.*

@Composable
fun Info(
    modifier: Modifier = Modifier,
    emoji: Text,
    title: Text,
    subtitle: Text
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = ContextAmbient.current.resolveText(emoji),
            fontSize = 84.sp
        )
        Text(
            text = ContextAmbient.current.resolveText(title),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(top = 32.dp)
        )
        Text(
            text = ContextAmbient.current.resolveText(subtitle),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun EmptyState(
    emoji: Text,
    title: Text,
    subtitle: Text
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        alignment = Alignment.Center
    ) {
        Info(
            modifier = Modifier.padding(16.dp),
            emoji = emoji,
            title = title,
            subtitle = subtitle
        )
    }
}

@Preview(name = "light theme long copy", group = "info")
@Composable
fun InfoPreviewLightLongCopy() {
    AppTheme {
        Surface {
            Info(modifier = Modifier, emoji = text("🙌🏽"), title = text("super incredibly long title with lots of stuff"), subtitle = text("also a mega subtitle with lots of really really really exiting words"))
        }
    }
}

@Preview(name = "light theme", group = "info")
@Composable
fun InfoPreviewLight() {
    AppTheme {
        Surface {
            Info(modifier = Modifier, emoji = text("🙌🏽"), title = text("title"), subtitle = text("subtitle"))
        }
    }
}

@Preview(name = "dark theme", group = "info")
@Composable
fun InfoPreviewDark() {
    AppTheme(darkTheme = true) {
        Surface {
            Info(modifier = Modifier, emoji = text("🙌🏽"), title = text("title"), subtitle = text("subtitle"))
        }
    }
}

@Composable
fun FullWidthButton(
    modifier: Modifier = Modifier.padding(bottom = 16.dp),
    onClick: () -> Unit,
    title: Int,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = ContextAmbient.current.getString(title).toUpperCase(Locale.getDefault()),
            color = MaterialTheme.colors.onPrimary,
        )
    }
}