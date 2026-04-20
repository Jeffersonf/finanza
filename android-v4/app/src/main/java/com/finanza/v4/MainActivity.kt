package com.finanza.v4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import android.Manifest
import android.os.Build
import com.finanza.v4.data.notifications.DueReminderWorker
import com.finanza.v4.ui.FinanzaApp
import com.finanza.v4.ui.home.HomeViewModel
import com.finanza.v4.ui.home.HomeViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory((application as FinanzaApp).container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                DueReminderWorker.schedule((application as FinanzaApp).container.appContext)
            }
            FinanzaApp(
                viewModel = viewModel,
                onEnableReminders = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        DueReminderWorker.schedule((application as FinanzaApp).container.appContext)
                    }
                }
            )
        }
    }
}
