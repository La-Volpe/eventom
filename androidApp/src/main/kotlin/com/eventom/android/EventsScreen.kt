package com.eventom.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eventom.domain.model.EventCategory
import com.eventom.presentation.EventsUiState
import java.time.Instant as JavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val eventTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM · HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    viewState: EventsUiState,
    onSelect: (EventCategory?) -> Unit = {},
    onToggleSaved: (Long) -> Unit = {},
    onRefresh: () -> Unit = {},
    onOpenDetail: (Long) -> Unit = {},
    headerUser: String? = null,
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Compact brand wordmark (shared component = splash/login face).
            EvenTomWordmark(modifier = Modifier.weight(1f), compact = true)
            // Signed-in chip with sign-out / sign-in entry — session state
            // from the shared SessionStore.
            when {
                headerUser != null -> {
                    Text(
                        "Hi, $headerUser",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = onSignOut) {
                        Text("Sign out", style = MaterialTheme.typography.labelSmall)
                    }
                }

                else -> TextButton(onClick = onSignIn) { Text("Sign in") }
            }
            if (viewState.isRefreshing) {
                CircularProgressIndicator(Modifier.height(20.dp).padding(start = 8.dp))
            } else {
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
        }

        // Category filter chips: "All" clears the selection. The row scrolls
        // horizontally (Eventim category-nav idiom): the 7 chips overflow a
        // medium phone width, and a non-scrolling Row measures the overflow
        // children at ~zero width — they wrap into huge stacks and push the
        // whole list down (QA-found gap). Horizontal scrolling keeps every
        // chip one line tall and reachable.
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = viewState.selectedCategory == null,
                onClick = { onSelect(null) },
                label = { Text("All") },
            )
            viewState.categories.forEach { category ->
                FilterChip(
                    selected = viewState.selectedCategory == category,
                    onClick = { onSelect(category) },
                    label = { Text(categoryLabel(category)) },
                )
            }
        }

        when {
            viewState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            viewState.events.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing here yet — hit Refresh")
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                viewState.message?.let { message ->
                    item {
                        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                items(viewState.events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onToggleSaved = { onToggleSaved(event.id) },
                        onOpen = { onOpenDetail(event.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventCard(
    event: com.eventom.domain.model.Event,
    onToggleSaved: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row {
            // Poster thumbnail (offline artwork); decorative — title text follows.
            // T5: list rows decode the 480×600 thumb (~1.2MB ARGB_8888) instead
            // of the ~29MB hero decode — the card-scroll budget. Detail keeps hero.
            Image(
                painter = painterResource(posterThumbResFor(event.imageRef, event.category)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(12.dp)
                    .size(104.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 12.dp, end = 12.dp, bottom = 12.dp)
                    .padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(categoryLabel(event.category), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onToggleSaved) {
                        Text(if (event.isSaved) "★ Saved" else "☆ Save")
                    }
                }
                Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (event.summary.isNotEmpty()) {
                    Text(event.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }
                Text(
                    "${eventTimeFormatter.format(JavaInstant.ofEpochMilli(event.startTime.toEpochMilliseconds()))} · ${event.venue}, ${event.city}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Eventim cards always carry price-from — same formatting as the
                // detail screen's price line.
                event.priceFrom?.let { price ->
                    Text(
                        "ab ${formatPrice(price)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TicketBlue,
                    )
                }
            }
        }
    }
}
