package com.nikashitsa.polar_alert_android.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Fonts {
    private fun textStyle(
        size: Int,
        weight: FontWeight,
        lineHeightMultiplier: Float,
        letterSpacingPercent: Float
    ): TextStyle {
        return TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = (size * lineHeightMultiplier).sp,
            letterSpacing = (size * letterSpacingPercent).sp,
        )
    }

    val textMd = textStyle(16, FontWeight.Normal, 1.5f, 0.01f)
    val textMdBold = textStyle(16, FontWeight.SemiBold, 1.5f, 0.01f)

    val textLg = textStyle(20, FontWeight.Normal, 1.35f, 0f)
    val textLgBold = textStyle(20, FontWeight.SemiBold, 1.35f, 0f)

    val textXl = textStyle(32, FontWeight.Normal, 1.2f, -0.01f)
    val textXlBold = textStyle(32, FontWeight.SemiBold, 1.2f, -0.01f)

    val text2Xl = textStyle(96, FontWeight.Normal, 1.2f, -0.02f)
    val text2XlBold = textStyle(96, FontWeight.SemiBold, 1.2f, -0.02f)
}

@Preview
@Composable
fun FontsPreview() {
    val text: @Composable (TextStyle) -> Unit = { style ->
        Text(
            text = "Font",
            color = Colors.White,
            style = style,
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
        text(Fonts.textMd)
        text(Fonts.textMdBold)
        text(Fonts.textLg)
        text(Fonts.textLgBold)
        text(Fonts.textXl)
        text(Fonts.textXlBold)
        text(Fonts.text2Xl)
        text(Fonts.text2XlBold)
    }
}