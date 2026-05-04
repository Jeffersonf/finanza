package com.finanza.v4

import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import android.Manifest
import android.os.Build
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import com.finanza.v4.data.notifications.DueReminderWorker
import com.finanza.v4.data.notifications.PersistentFinanzaNotification
import com.finanza.v4.ui.FinanzaApp
import com.finanza.v4.ui.home.AppScreen
import com.finanza.v4.ui.home.HomeViewModel
import com.finanza.v4.ui.home.HomeViewModelFactory
import com.finanza.v4.widget.FinanzaWidgetUpdater
import com.finanza.v4.widget.WidgetActions

class MainActivity : FragmentActivity() {
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            (application as FinanzaApp).container.repository,
            (application as FinanzaApp).container.securityPreferences,
            (application as FinanzaApp).container.appContext
        )
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
                biometricAvailable = isBiometricAvailable(),
                onRequestBiometricUnlock = ::requestBiometricUnlock,
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
        FinanzaWidgetUpdater.refreshAll((application as FinanzaApp).container.appContext)
        handleWidgetAction(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetAction(intent)
    }

    override fun onResume() {
        super.onResume()
        FinanzaWidgetUpdater.refreshAll((application as FinanzaApp).container.appContext)
        viewModel.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppBackgrounded()
    }

    private fun handleWidgetAction(intent: Intent?) {
        when (intent?.getStringExtra(WidgetActions.EXTRA_ACTION)) {
            WidgetActions.ACTION_OPEN_HOME -> viewModel.setScreen(AppScreen.Home)
            WidgetActions.ACTION_OPEN_DUE -> viewModel.setScreen(AppScreen.Due)
            WidgetActions.ACTION_TRANSACTION -> viewModel.openAddTransaction()
            WidgetActions.ACTION_SHOPPING -> {
                viewModel.setScreen(AppScreen.Shopping)
                viewModel.openAddShoppingItem()
            }
            WidgetActions.ACTION_OPEN_SHOPPING -> viewModel.setScreen(AppScreen.Shopping)
        }
        intent?.removeExtra(WidgetActions.EXTRA_ACTION)
    }

    private fun isBiometricAvailable(): Boolean {
        val result = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun requestBiometricUnlock() {
        if (!isBiometricAvailable()) {
            viewModel.reportUnlockError("Biometria indisponivel neste aparelho.")
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.unlockWithBiometricSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        viewModel.reportUnlockError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    viewModel.reportUnlockError("Biometria nao reconhecida. Tente novamente.")
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear Finanza")
                .setSubtitle("Use a biometria do aparelho para abrir o app")
                .setNegativeButtonText("Cancelar")
                .build()
        )
    }
}
