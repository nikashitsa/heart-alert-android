package com.nikashitsa.polar_alert_android.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

object Colors {
    val White = Color(0xFFFEFFFF)
    val Black = Color(0xFF000000)
    val Gray = Color(0xFF141414)
    val Red = Color(0xFFFF0000)

    val Transparent = Color.Transparent

    val Button = ButtonColors(
        containerColor = Colors.White,
        disabledContainerColor = Colors.White,
        contentColor = Colors.Black,
        disabledContentColor = Colors.Black
    )
    val TransparentButton = ButtonColors(
        containerColor = Colors.Transparent,
        disabledContainerColor = Colors.Transparent,
        contentColor = Colors.Transparent,
        disabledContentColor = Colors.Transparent
    )
}

@Preview
@Composable
fun ColorsPreview() {
    val rect: @Composable (Color) -> Unit = { color ->
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(color)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(all = 16.dp),
    ) {
        rect(Colors.White)
        rect(Colors.Black)
        rect(Colors.Gray)
        rect(Colors.Red)
    }
}