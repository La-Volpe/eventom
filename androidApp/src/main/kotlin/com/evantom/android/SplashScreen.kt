package com.evantom.android

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** How long the branded splash holds before routing (M3 spec: 1.2–1.8s). */
private val SPLASH_HOLD = 1_500

/**
 * Branded cold-start splash: bright background, centered EVENTOM wordmark in
 * ticket-blue, an animated accent line, and the Eventim-style tagline. Plain
 * Compose — no core-splashscreen dependency. Routing after the hold is
 * decided by the caller from the shared SessionStore (signed in → Events,
 * else → Login).
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Accent line fades/extends in over the first beat of the hold.
    val accent = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        accent.animateTo(1f, animationSpec = tween(durationMillis = 500))
    }

    LaunchedEffect(Unit) {
        delay(SPLASH_HOLD.toLong())
        onSplashFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            EventomWordmark()
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .width((28 + 36 * accent.value).dp)
                    .height(3.dp)
                    .background(TicketBlue, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Tickets für Live-Entertainment",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}