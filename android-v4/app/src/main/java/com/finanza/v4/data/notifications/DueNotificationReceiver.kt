package com.finanza.v4.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.finanza.v4.MainActivity
import com.finanza.v4.core.AppContainer
import com.finanza.v4.data.local.FinanzaDatabase
import com.finanza.v4.data.repository.DueItem
import com.finanza.v4.data.repository.FinanzaRepository
import com.finanza.v4.data.sync.FinanzaApiClient
import com.finanza.v4.data.sync.SyncPreferences
import com.finanza.v4.widget.FinanzaWidgetUpdater
import com.finanza.v4.widget.WidgetActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DueNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.getStringExtra(EXTRA_KIND)) {
            KIND_OPEN -> {
                val launch = Intent(appContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(WidgetActions.EXTRA_ACTION, WidgetActions.ACTION_OPEN_DUE)
                }
                appContext.startActivity(launch)
            }
            KIND_PAY, KIND_POSTPONE -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = buildRepository(appContext)
                        val itemId = intent.getStringExtra(EXTRA_ITEM_ID).orEmpty()
                        val date = intent.getStringExtra(EXTRA_DATE).orEmpty()
                        when (intent.getStringExtra(EXTRA_KIND)) {
                            KIND_PAY -> repository.payDueItem(itemId, date)
                            KIND_POSTPONE -> repository.postponeDueItem(itemId, date, 1)
                        }
                        DueReminderWorker.showNow(appContext)
                        FinanzaWidgetUpdater.refreshAll(appContext)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun buildRepository(context: Context): FinanzaRepository {
        val database = Room.databaseBuilder(
            context,
            FinanzaDatabase::class.java,
            "finanza-v4.db"
        ).fallbackToDestructiveMigration(true).build()
        return FinanzaRepository(
            accountDao = database.accountDao(),
            transactionDao = database.transactionDao(),
            budgetDao = database.budgetDao(),
            goalDao = database.goalDao(),
            categoryDao = database.categoryDao(),
            shoppingListDao = database.shoppingListDao(),
            shoppingItemDao = database.shoppingItemDao(),
            appSettingsDao = database.appSettingsDao(),
            syncPreferences = SyncPreferences(context),
            apiClient = FinanzaApiClient(),
            onLocalDataChanged = { FinanzaWidgetUpdater.refreshAll(context) }
        )
    }

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_DATE = "date"

        const val KIND_OPEN = "open"
        const val KIND_PAY = "pay"
        const val KIND_POSTPONE = "postpone"
    }
}
