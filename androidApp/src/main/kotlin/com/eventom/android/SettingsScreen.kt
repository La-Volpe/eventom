package com.eventom.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eventom.language.AppLanguage
import com.eventom.strings.Strings

/**
 * Settings (bilingual 2/4): the language picker over the three seeded
 * options — [AppLanguage.SYSTEM] resolves through the device locale (de →
 * DE; anything else → EN, the pinned fallback), DE/EN pin the content
 * language directly. Choosing writes through [LanguageSetting.choose]: the
 * UI re-renders immediately from [Strings] and the events tab auto-refreshes
 * into the matching payload family.
 */
@Composable
fun SettingsScreen(
    strings: Strings,
    persisted: AppLanguage,
    onChoose: (AppLanguage) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
    ) {
        Text(strings.settingsTitle(), style = MaterialTheme.typography.headlineSmall)
        Text(
            strings.languageTitle(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        LanguageOption(
            label = strings.languageSystemDefault(),
            description = "languageOption-system",
            selected = persisted == AppLanguage.SYSTEM,
            onChoose = { onChoose(AppLanguage.SYSTEM) },
        )
        LanguageOption(
            label = strings.languageGerman(),
            description = "languageOption-de",
            selected = persisted == AppLanguage.DE,
            onChoose = { onChoose(AppLanguage.DE) },
        )
        LanguageOption(
            label = strings.languageEnglish(),
            description = "languageOption-en",
            selected = persisted == AppLanguage.EN,
            onChoose = { onChoose(AppLanguage.EN) },
        )
    }
}

@Composable
private fun LanguageOption(
    label: String,
    description: String,
    selected: Boolean,
    onChoose: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            // assists the machine walkthroughs: a semantic id per option that
            // does NOT depend on the label (which says in both languages).
            .semantics { contentDescription = description }
            .clickable(onClick = onChoose)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onChoose)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}