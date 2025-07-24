package com.nikashitsa.polar_alert_android.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikashitsa.polar_alert_android.ui.theme.Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier,
) {
    var valueIntOld by remember { mutableIntStateOf(value) }

    Slider(
        modifier = modifier,
        value = value.toFloat(),
        valueRange = valueRange,
        onValueChange = { value ->
            val valueInt = value.toInt()
            if (valueInt != valueIntOld) {
                valueIntOld = valueInt
                onValueChange(valueInt)
            }
        },
        colors = SliderDefaults.colors(
            thumbColor = Colors.White,
        ),
        track = {
            SliderDefaults.Track(
                colors = SliderDefaults.colors(
                    activeTrackColor = Colors.Red,
                    inactiveTrackColor = Colors.Gray,
                ),
                modifier = Modifier.height(4.dp),
                sliderState = it,
                drawStopIndicator = null,
            )
        }
    )
}