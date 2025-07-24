package com.nikashitsa.polar_alert_android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun DropdownMenuSelector(
    value: Int,
    progression: IntProgression,
    setValue: (Int) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AppTextButton(onClick = { expanded = true }) {
            Text(text = "$value BPM")
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Chevron")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            progression.forEach { i ->
                DropdownMenuItem(
                    text = { Text("$i") },
                    onClick = {
                        setValue(i)
                        expanded = false
                    }
                )
            }
        }
    }
}