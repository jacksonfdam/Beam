package com.jacksonfdam.beam.i18n

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Dropdown language switcher: a button showing the current flag + label opens a
 * menu of flag + native name. Tapping an item updates the global [LanguageState],
 * so every Beam window/screen re-renders in that language.
 *
 * Note: flag emoji render on Android/iOS; on some desktop JVM/OS font setups they
 * may fall back to the two-letter region code — the label keeps it unambiguous.
 */
@Composable
fun LanguageSelector(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val current = LanguageState.current
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text("${current.flag}  ${current.label}  ▾")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Language.entries.forEach { lang ->
                DropdownMenuItem(
                    text = { Text("${lang.flag}  ${lang.nativeName}") },
                    onClick = {
                        LanguageState.current = lang
                        expanded = false
                    },
                )
            }
        }
    }
}
