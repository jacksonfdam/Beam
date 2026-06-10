package com.jacksonfdam.beam.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Process-wide selected UI language. In-memory and defaults to English; both
 * the desktop windows and the mobile remote read the same value, so switching
 * language anywhere updates everything. (Persisting the choice is a small
 * follow-up — see the README.)
 */
object LanguageState {
    var current by mutableStateOf(Language.EN)
}

/** The active string bundle. Read `LocalStrings.current` inside composables. */
val LocalStrings = staticCompositionLocalOf { stringsFor(Language.EN) }

/** Provides [LocalStrings] for the currently selected language to [content]. */
@Composable
fun ProvideStrings(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalStrings provides stringsFor(LanguageState.current),
        content = content,
    )
}
