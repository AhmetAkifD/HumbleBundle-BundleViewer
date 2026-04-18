package com.ahmet.bundleviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

val HbDark = Color(0xFF1a1a1a)
val HbRed = Color(0xFFcc292b)
val HbDarkCard = Color(0xFF2a2a2a)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = HbDark,
                    surface = HbDarkCard,
                    primary = HbRed
                )
            ) {
                BundleViewerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundleViewerApp() {
    val context = LocalContext.current
    val storageManager = remember { StorageManager(context) }
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(false) }
    var newBundles by remember { mutableStateOf<List<BundleItem>>(emptyList()) }
    var existingBundles by remember { mutableStateOf<List<BundleItem>>(emptyList()) }
    
    var isNewSectionExpanded by remember { mutableStateOf(true) }
    var isExistingSectionExpanded by remember { mutableStateOf(true) }

    fun loadAndProcess() {
        isLoading = true
        scope.launch {
            val fetched = BundleScraper.fetchBundles()
            val storedIdentifiers = storageManager.loadStoredBundles().toSet()
            
            val newlyFetched = mutableListOf<BundleItem>()
            val alreadyStored = mutableListOf<BundleItem>()
            val updatedStoredIdentifiers = mutableSetOf<String>()
            
            updatedStoredIdentifiers.addAll(storedIdentifiers)
            
            for (item in fetched) {
                if (storedIdentifiers.contains(item.name)) {
                    alreadyStored.add(item)
                } else {
                    newlyFetched.add(item)
                    updatedStoredIdentifiers.add(item.name)
                }
            }
            
            newBundles = newlyFetched
            existingBundles = alreadyStored
            
            storageManager.saveStoredBundles(updatedStoredIdentifiers.toList())
            
            isLoading = false
            isNewSectionExpanded = true // Auto-expand on load
        }
    }

    LaunchedEffect(Unit) {
        loadAndProcess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bundle Viewer", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HbDark,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { loadAndProcess() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Yenile", tint = Color.White)
                    }
                }
            )
        },
        containerColor = HbDark
    ) { padding ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HbRed)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    SectionHeader(
                        title = "Yeni (${newBundles.size})",
                        isExpanded = isNewSectionExpanded,
                        onToggle = { isNewSectionExpanded = !isNewSectionExpanded }
                    )
                }
                if (isNewSectionExpanded) {
                    if (newBundles.isEmpty()) {
                        item {
                            EmptyText("Şimdilik burada yeni bir bundle yok.")
                        }
                    } else {
                        items(newBundles) { bundle ->
                            BundleCard(bundle)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = "Mevcut (${existingBundles.size})",
                        isExpanded = isExistingSectionExpanded,
                        onToggle = { isExistingSectionExpanded = !isExistingSectionExpanded }
                    )
                }
                if (isExistingSectionExpanded) {
                    if (existingBundles.isEmpty()) {
                        item {
                            EmptyText("Kayıtlı bundle bulunmamaktadır.")
                        }
                    } else {
                        items(existingBundles) { bundle ->
                            BundleCard(bundle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyText(text: String) {
    Text(
        text = text,
        color = Color.Gray,
        modifier = Modifier.padding(16.dp),
        fontSize = 14.sp
    )
}

@Composable
fun SectionHeader(title: String, isExpanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(24.dp).background(HbRed))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = "Genişlet/Daralt",
            tint = Color.White
        )
    }
}

@Composable
fun BundleCard(bundle: BundleItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = HbDarkCard),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = bundle.imageUrl,
                contentDescription = bundle.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = bundle.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (bundle.marketingBlurb.isNotEmpty()) {
                    Text(
                        text = bundle.marketingBlurb,
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .background(HbRed, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = bundle.timeLeft,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
