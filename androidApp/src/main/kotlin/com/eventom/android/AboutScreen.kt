package com.eventom.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eventom.platform.PlatformInfo
import com.eventom.platform.SharedVersion
import com.eventom.platform.platformInfo

/** Second native screen, demonstrating expect/actual + shared constants. */
@Composable
fun AboutScreen(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val info: PlatformInfo = platformInfo()

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
    ) {
        Text("About", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("EvenTom", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Shared Kotlin ${SharedVersion.VERSION}", style = MaterialTheme.typography.bodyMedium)
                Text("Running on: ${info.osName} ${info.osVersion} (${info.deviceModel})", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}