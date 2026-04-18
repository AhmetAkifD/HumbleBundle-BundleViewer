import os

files = {
    "settings.gradle": """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "BundleViewer"
include(":app")
""",
    "build.gradle": """
buildscript {
    ext {
        compose_ui_version = '1.4.3'
    }
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.1.1'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10"
    }
}
""",
    "gradle.properties": """
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
""",
    "app/build.gradle": """
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.ahmet.bundleviewer'
    compileSdk 34

    defaultConfig {
        applicationId "com.ahmet.bundleviewer"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = '1.8'
    }
    buildFeatures {
        compose true
    }
    composeOptions {
        kotlinCompilerExtensionVersion '1.4.3'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.10.1'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.1'
    implementation 'androidx.activity:activity-compose:1.7.2'
    implementation "androidx.compose.ui:ui:$compose_ui_version"
    implementation "androidx.compose.ui:ui-tooling-preview:$compose_ui_version"
    implementation 'androidx.compose.material3:material3:1.1.1'
    implementation 'org.jsoup:jsoup:1.16.1'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'io.coil-kt:coil-compose:2.4.0'
}
""",
    "app/src/main/AndroidManifest.xml": """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.BundleViewer">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.BundleViewer">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""",
    "app/src/main/res/values/strings.xml": """<resources>
    <string name="app_name">Bundle Viewer</string>
</resources>
""",
    "app/src/main/res/values/colors.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    <color name="hb_red">#cc292b</color>
    <color name="hb_dark">#1a1a1a</color>
    <color name="hb_dark_card">#2a2a2a</color>
    <color name="hb_gray">#888888</color>
</resources>
""",
    "app/src/main/res/values/themes.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.BundleViewer" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
""",
    "app/src/main/java/com/ahmet/bundleviewer/BundleItem.kt": """package com.ahmet.bundleviewer

data class BundleItem(
    val name: String,
    val imageUrl: String,
    val marketingBlurb: String,
    val timeLeft: String,
    val detailUrl: String
)
""",
    "app/src/main/java/com/ahmet/bundleviewer/BundleScraper.kt": """package com.ahmet.bundleviewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object BundleScraper {
    suspend fun fetchBundles(): List<BundleItem> = withContext(Dispatchers.IO) {
        val url = "https://www.humblebundle.com/bundles"
        val bundles = mutableListOf<BundleItem>()
        try {
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()

            val tileHolders = document.select("div.tile-holder.js-tile-holder.one-third")
            
            for (element in tileHolders) {
                val anchor = element.selectFirst("a") ?: continue
                val detailUrl = anchor.attr("href")
                
                // Info block
                val infoSection = element.selectFirst(".info-section")
                val name = infoSection?.selectFirst(".name")?.text() ?: "Unknown Bundle"
                val timeLeft = infoSection?.selectFirst(".timer-field")?.text() ?: "Unknown Time"
                
                // Image
                val imgElement = element.selectFirst("img.tile-image")
                var imageUrl = imgElement?.attr("data-src") ?: ""
                if (imageUrl.isEmpty()) {
                    imageUrl = imgElement?.attr("src") ?: ""
                }
                
                // Text blurb
                val blurb = element.selectFirst(".marketing-blurb")?.text() ?: ""
                
                // We add to list
                bundles.add(BundleItem(
                    name = name,
                    imageUrl = imageUrl,
                    marketingBlurb = blurb,
                    timeLeft = timeLeft,
                    detailUrl = detailUrl
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace() // In a real app we'd log this properly
        }
        bundles
    }
}
""",
    "app/src/main/java/com/ahmet/bundleviewer/StorageManager.kt": """package com.ahmet.bundleviewer

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class StorageManager(private val context: Context) {

    private val fileName = "stored_bundles.json"
    private val gson = Gson()
    
    // The keys are the names of the bundles
    fun loadStoredBundles(): List<String> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()
        
        return try {
            val reader = FileReader(file)
            val type = object : TypeToken<List<String>>() {}.type
            val result: List<String> = gson.fromJson(reader, type) ?: emptyList()
            reader.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun saveStoredBundles(bundles: List<String>) {
        val file = File(context.filesDir, fileName)
        try {
            val writer = FileWriter(file)
            gson.toJson(bundles, writer)
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
""",
    "app/src/main/java/com/ahmet/bundleviewer/MainActivity.kt": """package com.ahmet.bundleviewer

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
"""
}

def create_files():
    for filepath, content in files.items():
        dir_name = os.path.dirname(filepath)
        if dir_name:
            os.makedirs(dir_name, exist_ok=True)
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content)

if __name__ == "__main__":
    create_files()
    print("Files created.")
