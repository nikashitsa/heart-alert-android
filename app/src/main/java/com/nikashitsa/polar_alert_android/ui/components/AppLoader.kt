package com.nikashitsa.polar_alert_android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import com.nikashitsa.polar_alert_android.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppLoader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .width(32.dp),
            color = Colors.White,
            strokeWidth = 2.dp
        )
    }
}

@Preview
@Composable
fun AppLoaderPreview() {
    AppLoader()
}