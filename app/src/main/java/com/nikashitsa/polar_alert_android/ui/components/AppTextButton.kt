package com.nikashitsa.polar_alert_android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import com.nikashitsa.polar_alert_android.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppTextButton(onClick: () -> Unit = {}, content: @Composable RowScope.() -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.fillMaxHeight(),
        colors = Colors.TransparentButton,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides Fonts.textMd,
            LocalContentColor provides Color.White
        ) {
            content()
        }
    }
}


@Preview
@Composable
fun AppTextButtonPreview() {
    Column(
        modifier = Modifier
            .height(40.dp)
    ) {
        AppTextButton() {
            Text(text = "Text button")
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Chevron")
        }
    }
}