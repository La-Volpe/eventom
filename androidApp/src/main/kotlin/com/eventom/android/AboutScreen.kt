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
import com.eventom.strings.Strings

/** Second native screen, demonstrating expect/actual + shared constants. */
@Composable
fun AboutScreen(strings: Strings, contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val info: PlatformInfo = platformInfo()

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
    ) {
        Text(strings.aboutHeading(), style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("EvenTom", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) // wordmark, never translated
                Text("${strings.aboutSharedKotlin()} ${SharedVersion.VERSION}", style = MaterialTheme.typography.bodyMedium)
                Text("${strings.aboutRunningOn()}: ${info.osName} ${info.osVersion} (${info.deviceModel})", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}