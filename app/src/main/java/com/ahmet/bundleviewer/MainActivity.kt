package com.ahmet.bundleviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel: BundleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BundleViewerTheme {
                BundleViewerApp(viewModel)
            }
        }
    }
}

@Composable
fun BundleViewerApp(viewModel: BundleViewModel) {
    val list by viewModel.list.collectAsStateWithLifecycle()
    val details by viewModel.details.collectAsStateWithLifecycle()
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = list.bundles.firstOrNull { it.machineName == selectedName }

    AnimatedContent(
        targetState = selected,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen",
    ) { bundle ->
        if (bundle == null) {
            ListScreen(
                state = list,
                onRefresh = viewModel::refresh,
                onOpen = { selectedName = it.machineName },
            )
        } else {
            // Re-run when a list refresh clears the cached detail.
            val missing = details[bundle.url] == null
            LaunchedEffect(bundle.url, missing) { if (missing) viewModel.loadDetail(bundle) }
            BackHandler { selectedName = null }
            DetailScreen(
                bundle = bundle,
                state = details[bundle.url],
                onBack = { selectedName = null },
                onRefresh = { viewModel.loadDetail(bundle, force = true) },
            )
        }
    }
}
