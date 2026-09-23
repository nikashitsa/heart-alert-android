package com.nikashitsa.polar_alert_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikashitsa.polar_alert_android.R
import com.nikashitsa.polar_alert_android.lib.AppLanguage
import com.nikashitsa.polar_alert_android.ui.theme.Colors
import com.nikashitsa.polar_alert_android.ui.theme.HeartAlertTheme

/** Pill with a globe and the current language's code; opens a menu of all languages. */
@Composable
fun LanguageSelector(
    value: AppLanguage,
    setValue: (AppLanguage) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .height(36.dp)
                .clip(CircleShape)
                .background(Colors.Gray)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(horizontal = 14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = stringResource(R.string.language),
                modifier = Modifier.size(18.dp),
            )
            Text(text = value.code)
        }
        DropdownMenu(expanded = expanded, containerColor = Colors.Gray, onDismissRequest = { expanded = false }) {
            AppLanguage.sorted.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    trailingIcon = {
                        if (language == value) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        if (language != value) setValue(language)
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun LanguageSelectorPreview() {
    HeartAlertTheme {
        LanguageSelector(AppLanguage.EN)
    }
}
