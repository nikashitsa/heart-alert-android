package com.nikashitsa.polar_alert_android.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.nikashitsa.polar_alert_android.ui.theme.Colors

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Switch(
        checked = checked,
        onCheckedChange = { it ->
            val type = if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
            haptic.performHapticFeedback(type)
            onCheckedChange(it)
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Colors.White,
            checkedTrackColor = Colors.Red,
            checkedBorderColor = Colors.Transparent,
            uncheckedThumbColor = Colors.White,
            uncheckedTrackColor = Colors.Gray,
            uncheckedBorderColor = Colors.Transparent,
        )
    )
}