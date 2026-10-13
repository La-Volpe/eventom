package com.eventom.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eventom.domain.model.EventCategory
import com.eventom.presentation.EventDetailUiState
import com.eventom.strings.Strings
import java.time.Instant as JavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val eventDetailDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy · HH:mm").withZone(ZoneId.systemDefault())

/**
 * Category-colored offline hero gradients (no remote images): a per-category
 * two-stop gradient stands in for the header image area.
 */
private fun EventCategory.heroColors(): List<Color> = when (this) {
    EventCategory.CONCERTS -> listOf(Color(0xFF1C6EE8), Color(0xFF0F3FA0))
    EventCategory.MUSICAL_SHOW -> listOf(Color(0xFF7B3FE4), Color(0xFF4A1E9E))
    EventCategory.SPORTS -> listOf(Color(0xFF0E9F6E), Color(0xFF066544))
    EventCategory.COMEDY -> listOf(Color(0xFFF59E0B), Color(0xFFB45309))
    EventCategory.THEATER -> listOf(Color(0xFFDC2626), Color(0xFF9A1F1F))
    EventCategory.LEISURE -> listOf(Color(0xFF06B6D4), Color(0xFF037A9C))
}

/**
 * Eventim-style price formatting, shared by event cards and the detail
 * screen ("ab X.XX €" lines).
 */
internal fun formatPrice(price: Double): String = "%.2f €".format(price)

/**
 * Eventim-style product page (M4). Rendering-only: prices/actions are demo
 * data from the fake backend; the CTA fires an "Offline-Demo" snackbar.
 */
@Composable
fun EventDetailScreen(
    strings: Strings,
    state: EventDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state is EventDetailUiState.Success) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = { scope.launch { snackbarHostState.showSnackbar(strings.offlineDemoFeedback()) } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TicketBlue,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(52.dp),
                    ) {
                        Text(strings.buyTicketsCta(), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        when (state) {
            EventDetailUiState.Idle, EventDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TicketBlue)
                        Spacer(Modifier.height(12.dp))
                        Text(strings.loadingDetails(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            is EventDetailUiState.Error -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = TicketBlue)) {
                            Text(strings.tryAgain())
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onBack) {
                            Text(strings.backToEvents())
                        }
                    }
                }
            }

            is EventDetailUiState.Success ->
                // Bottom scaffold padding clears the sticky CTA; the hero may
                // still extend under the status bar (no top inset used).
                EventDetailContent(
                    state = state,
                    strings = strings,
                    onBack = onBack,
                    bottomPadding = scaffoldPadding.calculateBottomPadding(),
                )
        }
    }
}

@Composable
private fun EventDetailContent(
    state: EventDetailUiState.Success,
    strings: Strings,
    onBack: () -> Unit,
    bottomPadding: Dp,
) {
    val event = state.event
    val detail = state.detail

    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        // --- Hero: full-bleed poster artwork, scrim on top ------------------
        // The bitmap already carries the event title in the art, so the hero
        // keeps only the category accent + back control; title/date/venue/
        // price render in the stacked info block below (Eventim product-page
        // order) — this also keeps the fallback-safe title legible.
        Box(
            Modifier
                .fillMaxWidth()
                .height(230.dp),
        ) {
            Image(
                painter = painterResource(posterResFor(event.imageRef, event.category)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                // Anchored to the poster's top so the in-art title band stays
                // out of the crop and cannot collide with the overlay.
                alignment = Alignment.TopCenter,
                modifier = Modifier.matchParentSize(),
            )
            // Scrim: transparent art up top, dark toward the chip.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0f),
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.72f),
                            ),
                        ),
                    ),
            )
            // Back affordance sits on the hero, clear of the status bar.
            // Plain glyph — material-icons-core is not on the classpath and no
            // new dependencies are allowed.
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 4.dp)
                    .clickable(onClick = onBack)
                    .semantics { contentDescription = strings.backLabel() },
            ) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "‹",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // Category accent: colored chip over the scrim's dark ground
            // (covers the in-art label zone; white text stays legible).
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TicketBlue,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text(
                    strings.category(event.category),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        // --- Stacked info block (below the hero) ----------------------------
        Column(Modifier.padding(16.dp)) {
            // Title renders below the artwork: legible on the bright
            // background on BOTH paths (the category-fallback bitmap carries
            // no title) and no double-title over event posters.
            Text(
                event.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${eventDetailDateFormatter.format(JavaInstant.ofEpochMilli(event.startTime.toEpochMilliseconds()))} · ${event.venue}, ${event.city}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            // Prominent "ab X.XX €" price line.
            event.priceFrom?.let { price ->
                Text(
                    strings.priceFrom(min = formatPrice(price)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TicketBlue,
                )
                Spacer(Modifier.height(16.dp))
            }

            SectionHeading(strings.aboutTitle())
            Text(detail.longSummary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            if (detail.lineup.isNotEmpty()) {
                SectionHeading(strings.lineupTitle())
                detail.lineup.forEach { act ->
                    Text("•  $act", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
            }

            SectionHeading(strings.infoTitle())
            if (detail.doorsTime.isNotBlank()) {
                Text(strings.doorsLabel(time = detail.doorsTime), style = MaterialTheme.typography.bodyMedium)
            }
            if (detail.ageInfo.isNotBlank()) {
                Text(detail.ageInfo, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))

            if (detail.ticketCategories.isNotEmpty()) {
                SectionHeading(strings.ticketCategoriesTitle())
                detail.ticketCategories.forEach { ticket ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(ticket.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatPrice(ticket.price),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TicketBlue,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
}