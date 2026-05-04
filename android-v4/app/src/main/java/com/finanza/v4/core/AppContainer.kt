package com.finanza.v4.core

import android.content.Context
import androidx.room.Room
import com.finanza.v4.data.local.FinanzaDatabase
import com.finanza.v4.data.repository.FinanzaRepository
import com.finanza.v4.data.security.SecurityPreferences
import com.finanza.v4.data.sync.BackgroundSyncWorker
import com.finanza.v4.data.sync.FinanzaApiClient
import com.finanza.v4.data.sync.SyncPreferences
import com.finanza.v4.widget.FinanzaWidgetUpdater

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    private val database = Room.databaseBuilder(
        appContext,
        FinanzaDatabase::class.java,
        "finanza-v4.db"
    ).fallbackToDestructiveMigration(true).build()

    val securityPreferences = SecurityPreferences(context)
    val syncPreferences = SyncPreferences(context)

    val repository = FinanzaRepository(
        accountDao = database.accountDao(),
        transactionDao = database.transactionDao(),
        budgetDao = database.budgetDao(),
        goalDao = database.goalDao(),
        categoryDao = database.categoryDao(),
        shoppingListDao = database.shoppingListDao(),
        shoppingItemDao = database.shoppingItemDao(),
        appSettingsDao = database.appSettingsDao(),
        syncPreferences = syncPreferences,
        apiClient = FinanzaApiClient(),
        onLocalDataChanged = {
            FinanzaWidgetUpdater.refreshAll(appContext)
            BackgroundSyncWorker.kick(appContext)
        }
    )
}
