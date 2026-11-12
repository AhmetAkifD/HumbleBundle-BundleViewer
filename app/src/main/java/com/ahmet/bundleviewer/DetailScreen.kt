package com.ahmet.bundleviewer

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    bundle: BundleSummary,
    state: DetailState?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val detail = (state as? DetailState.Loaded)?.detail

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(bundle.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    RefreshButton(isLoading = state is DetailState.Loading, onClick = onRefresh)
                    IconButton(onClick = { uriHandler.openUri(bundle.url) }) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Tarayıcıda aç")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state is DetailState.Loading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { Header(bundle, detail) }
                when (state) {
                    is DetailState.Loaded -> {
                        itemsIndexed(state.detail.tiers) { index, tier ->
                            TierCard(tier, isTop = index == state.detail.tiers.lastIndex)
                        }
                        item { Footer(detail = state.detail, onOpen = { uriHandler.openUri(bundle.url) }) }
                    }
                    is DetailState.Failed -> item {
                        Box(Modifier.height(320.dp)) { ErrorView(state.message, onRefresh) }
                    }
                    else -> item {
                        Box(Modifier.height(240.dp)) { LoadingView() }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(bundle: BundleSummary, detail: BundleDetail?) {
    Column {
        CoverArt(
            imageUrl = detail?.imageUrl ?: bundle.imageUrl,
            logoUrl = detail?.logoUrl ?: bundle.logoUrl,
            contentDescription = bundle.name,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.height(16.dp))
        detail?.headline?.let {
            Text(
                it.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = HumbleVioletLight,
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(bundle.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            bundle.price?.let { PriceBlock(it, Modifier.weight(1f)) } ?: Spacer(Modifier.weight(1f))
            timeLeft(detail?.endDate ?: bundle.endDate)?.let { TimeLeftPill(it) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("Ürün", "${detail?.tiers?.lastOrNull()?.totalItemCount ?: bundle.itemCount}", Modifier.weight(1f))
            StatBox("Toplam değer", (detail?.value ?: bundle.value)?.format() ?: "—", Modifier.weight(1f))
            StatBox("Kademe", detail?.tiers?.size?.toString() ?: "…", Modifier.weight(1f))
        }
        detail?.description?.let { Description(it) }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Description(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.animateContentSize()) {
        Spacer(Modifier.height(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
            Text(if (expanded) "Daha az göster" else "Devamını oku", color = HumbleVioletLight)
        }
    }
}

@Composable
private fun TierCard(tier: BundleTier, isTop: Boolean) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isTop) HumbleViolet else CardBorder),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isTop) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    tier.price?.let { "${it.format()} veya fazlası" } ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    listOfNotNull(
                        "${tier.totalItemCount} ürün",
                        tier.totalValue?.let { "${it.format()} değerinde" },
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isTop) Pill("Tamamı", HumbleViolet, MaterialTheme.colorScheme.onPrimary)
        }
        if (tier.newItems.isEmpty()) {
            Text(
                "Bu kademede yeni ürün yok.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            Text(
                if (tier.totalItemCount == tier.newItems.size) "İçerik" else "Bu kademeyle eklenenler",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp),
            )
        }
        tier.newItems.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = CardBorder, modifier = Modifier.padding(horizontal = 16.dp))
            ItemRow(item)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemRow(item: BundleItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = null,
            // Book covers are portrait, game art is landscape; Fit shows both whole.
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(width = 96.dp, height = 54.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                item.msrp?.takeIf { it.amount > 0 }?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        it.format(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                    )
                }
            }
            if (item.developers.isNotEmpty()) {
                Text(
                    item.developers.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val tags = (item.deliveryMethods + item.platforms).map { it.toPlatformLabel() }.distinct()
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tags.forEach {
                        Pill(it, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun Footer(detail: BundleDetail, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        detail.keyExpirationDate?.let {
            InfoLine(Icons.Outlined.Key, "Anahtarları ${it.formatDate()} tarihine kadar etkinleştir.")
        }
        if (detail.charities.isNotEmpty()) {
            InfoLine(Icons.Outlined.Favorite, "Destek verilen kuruluş: ${detail.charities.joinToString(", ")}")
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onOpen,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Humble Bundle'da satın al", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
