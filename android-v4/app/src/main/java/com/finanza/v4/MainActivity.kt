package com.finanza.v4

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import android.Manifest
import android.os.Build
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.finanza.v4.data.notifications.DueReminderWorker
import com.finanza.v4.data.notifications.PersistentFinanzaNotification
import com.finanza.v4.ui.FinanzaApp
import com.finanza.v4.ui.home.AppScreen
import com.finanza.v4.ui.home.HomeViewModel
import com.finanza.v4.ui.home.HomeViewModelFactory
import com.finanza.v4.widget.WidgetActions

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory((application as FinanzaApp).container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
                onExitApp = ::finishAffinity,
                onEnableReminders = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        DueReminderWorker.schedule((application as FinanzaApp).container.appContext)
                    }
                }
            )
        }
        PersistentFinanzaNotification.show((application as FinanzaApp).container.appContext)
        handleWidgetAction(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetAction(intent)
    }

    private fun handleWidgetAction(intent: Intent?) {
        when (intent?.getStringExtra(WidgetActions.EXTRA_ACTION)) {
            WidgetActions.ACTION_TRANSACTION -> viewModel.openAddTransaction()
            WidgetActions.ACTION_SHOPPING -> {
                viewModel.setScreen(AppScreen.Shopping)
                viewModel.openAddShoppingItem()
            }
            WidgetActions.ACTION_OPEN_SHOPPING -> viewModel.setScreen(AppScreen.Shopping)
        }
        intent?.removeExtra(WidgetActions.EXTRA_ACTION)
    }
}
