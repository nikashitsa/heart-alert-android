package com.nikashitsa.polar_alert_android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.nikashitsa.polar_alert_android.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = CircleShape,
        colors = Colors.Button
    ) {
        Text(
            text = text,
            style = Fonts.textMdBold
        )
    }
}

@Preview
@Composable
fun AppButtonPreview() {
    AppButton("Connect", onClick = {})
}