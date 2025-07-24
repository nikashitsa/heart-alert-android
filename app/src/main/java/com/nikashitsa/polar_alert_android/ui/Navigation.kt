package com.nikashitsa.polar_alert_android.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nikashitsa.polar_alert_android.ui.screens.*
import com.nikashitsa.polar_alert_android.ui.theme.*

enum class AppFlow {
    Connect,
    Settings,
    Tracking
}

@Composable
fun Navigation() {
    var flow by rememberSaveable { mutableStateOf(AppFlow.Connect) }
    val context = LocalContext.current
    val activity = context as? Activity

    Box(
        modifier = Modifier
            .background(Colors.Black)
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        Crossfade(
            targetState = flow,
            label = "NavigationTransition",
            animationSpec = tween(durationMillis = 200)
        ) { currentFlow ->
            when (currentFlow) {
                AppFlow.Connect -> ConnectScreen(
                    onNext = { flow = AppFlow.Settings }
                )
                AppFlow.Settings -> SettingsScreen(
                    onBack = {
                        activity?.moveTaskToBack(true)
                    },
                    onNext = { flow = AppFlow.Tracking },
                )
                AppFlow.Tracking -> TrackingScreen(
                    onBack = { flow = AppFlow.Settings }
                )
            }
        }
    }
}