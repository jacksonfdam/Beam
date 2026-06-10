package com.jacksonfdam.beam.i18n

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Compact EN / PT / ES / SV switcher. Tapping a chip updates the global
 * [LanguageState], so every Beam window/screen re-renders in that language.
 */
@Composable
fun LanguageSelector(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Language.entries.forEach { lang ->
            if (LanguageState.current == lang) {
                Button(onClick = { LanguageState.current = lang }) { Text(lang.label) }
            } else {
                OutlinedButton(onClick = { LanguageState.current = lang }) { Text(lang.label) }
            }
        }
    }
}
