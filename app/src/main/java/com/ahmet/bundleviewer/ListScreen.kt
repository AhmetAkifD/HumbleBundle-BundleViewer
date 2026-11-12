package com.ahmet.bundleviewer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

enum class SortOrder(val label: String) {
    ENDING("Bitişi en yakın"),
    PRICE("En ucuz"),
    VALUE("En yüksek değer"),
    ITEMS("En çok ürün"),
}

private fun List<BundleSummary>.sortedBy(order: SortOrder) = when (order) {
    SortOrder.ENDING -> sortedBy { it.endDate }
    SortOrder.PRICE -> sortedBy { it.price?.amount ?: Double.MAX_VALUE }
    SortOrder.VALUE -> sortedByDescending { it.value?.amount ?: 0.0 }
    SortOrder.ITEMS -> sortedByDescending { it.itemCount }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    state: ListState,
    onRefresh: () -> Unit,
    onOpen: (BundleSummary) -> Unit,
) {
    var filterKey by rememberSaveable { mutableStateOf<String?>(null) }
    var sortName by rememberSaveable { mutableStateOf(SortOrder.ENDING.name) }
    val sort = SortOrder.valueOf(sortName)
    val filter = filterKey?.let(BundleCategory::fromKey)
    val visible = remember(state.bundles, filter, sort) {
        state.bundles.filter { filter == null || it.category == filter }.sortedBy(sort)
    }
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // When we already have data, report refresh failures without wiping the list.
    LaunchedEffect(state.error) {
        if (state.error != null && state.bundles.isNotEmpty()) snackbar.showSnackbar(state.error)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Column {
                        Text("Humble Bundles", fontWeight = FontWeight.ExtraBold)
                        val subtitle = when {
                            state.isLoading -> "Yenileniyor…"
                            state.lastUpdated != null ->
                                "${state.bundles.size} bundle · ${state.lastUpdated.formatClock()} itibarıyla"
                            else -> null
                        }
                        if (subtitle != null) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    SortButton(sort, onSelect = { sortName = it.name })
                    RefreshButton(isLoading = state.isLoading, onClick = onRefresh)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.bundles.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.bundles.isEmpty() && state.isLoading -> LoadingView()
                state.bundles.isEmpty() && state.error != null -> ErrorView(state.error, onRefresh)
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        CategoryFilters(
                            bundles = state.bundles,
                            selected = filter,
                            onSelect = { filterKey = it?.key },
                        )
                    }
                    items(visible, key = { it.machineName }) { bundle ->
                        BundleCard(bundle, onClick = { onOpen(bundle) })
                    }
                    if (visible.isEmpty()) {
                        item { EmptyHint("Bu kategoride şu an bundle yok.") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortButton(current: SortOrder, onSelect: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sırala")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label) },
                    leadingIcon = {
                        if (order == current) Icon(Icons.Outlined.Check, null) else Spacer(Modifier.size(24.dp))
                    },
                    onClick = {
                        onSelect(order)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
fun RefreshButton(isLoading: Boolean, onClick: () -> Unit) {
    val rotation = if (isLoading) {
        val transition = rememberInfiniteTransition(label = "refresh")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
            label = "rotation",
        ).value
    } else 0f
    IconButton(onClick = onClick, enabled = !isLoading) {
        Icon(Icons.Outlined.Refresh, contentDescription = "Yenile", modifier = Modifier.rotate(rotation))
    }
}

@Composable
private fun CategoryFilters(
    bundles: List<BundleSummary>,
    selected: BundleCategory?,
    onSelect: (BundleCategory?) -> Unit,
) {
    val counts = bundles.groupingBy { it.category }.eachCount()
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("Tümü · ${bundles.size}") },
                colors = chipColors,
                shape = RoundedCornerShape(50),
            )
        }
        items(BundleCategory.entries) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text("${category.label} · ${counts[category] ?: 0}") },
                colors = chipColors,
                shape = RoundedCornerShape(50),
            )
        }
    }
}

@Composable
private fun BundleCard(bundle: BundleSummary, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        CoverArt(
            imageUrl = bundle.imageUrl,
            logoUrl = bundle.logoUrl,
            contentDescription = bundle.name,
        ) {
            Pill(bundle.category.label, Color.Black.copy(alpha = 0.6f), Color.White, Modifier.padding(10.dp))
        }
        Column(Modifier.padding(16.dp)) {
            Text(
                bundle.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (bundle.itemTitles.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    includedItemsText(bundle.itemTitles, bundle.itemCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                listOfNotNull(
                    "${bundle.itemCount} ürün".takeIf { bundle.itemCount > 0 },
                    bundle.value?.let { "${it.format()} değerinde" },
                ).joinToString(" • "),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                bundle.price?.let { PriceBlock(it, Modifier.weight(1f)) } ?: Spacer(Modifier.weight(1f))
                timeLeft(bundle.endDate)?.let { TimeLeftPill(it) }
            }
        }
    }
}

private fun includedItemsText(titles: List<String>, total: Int): String {
    val shown = titles.take(4)
    val rest = maxOf(total, titles.size) - shown.size
    return "İçinde: " + shown.joinToString(", ") + if (rest > 0) " ve $rest ürün daha" else ""
}

/** Cover art with a darkening gradient and the bundle logo over it, like the website cards. */
@Composable
fun CoverArt(
    imageUrl: String?,
    logoUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (logoUrl != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color(0xE60F1729),
                        ),
                    ),
            )
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .height(52.dp)
                    .widthIn(max = 220.dp),
            )
        }
        overlay()
    }
}

@Composable
fun PriceBlock(price: Money, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            "İstediğin kadar öde",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${price.format()}+",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = HumbleVioletLight,
        )
    }
}

@Composable
fun TimeLeftPill(left: java.time.Duration, modifier: Modifier = Modifier) {
    val urgent = left.isUrgent()
    val color = if (urgent) UrgentOrange else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, if (urgent) UrgentOrange else CardBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(Icons.Outlined.Schedule, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(left.formatRemaining(), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Pill(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        color = content,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Bundle'lar yükleniyor…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("Veriler alınamadı", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(6.dp)) { Text("Tekrar dene") }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
    ) {
        Icon(Icons.Outlined.Inventory2, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
