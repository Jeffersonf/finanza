package com.finanza.v4.data.sync

import android.content.Context
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.finanza.v4.data.local.FinanzaDatabase
import com.finanza.v4.data.repository.FinanzaRepository
import com.finanza.v4.widget.FinanzaWidgetUpdater
import java.util.concurrent.TimeUnit

class BackgroundSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val syncPreferences = SyncPreferences(applicationContext)
        val config = syncPreferences.get()
        if (!config.connected || !config.autoSyncEnabled) return Result.success()

        val database = Room.databaseBuilder(
            applicationContext,
            FinanzaDatabase::class.java,
            "finanza-v4.db"
        ).fallbackToDestructiveMigration(true).build()

        return try {
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
                onLocalDataChanged = { FinanzaWidgetUpdater.refreshAll(applicationContext) }
            )
            repository.pushLocalToRemote(config)
            syncPreferences.markSyncResult(
                message = "Sincronizado em segundo plano",
                success = true
            )
            Result.success()
        } catch (error: Throwable) {
            syncPreferences.markSyncResult(
                message = error.message ?: "Falha ao sincronizar em segundo plano",
                success = false
            )
            Result.retry()
        } finally {
            database.close()
        }
    }

    companion object {
        private const val PERIODIC_WORK = "finanza-background-sync-periodic"
        private const val ONE_SHOT_WORK = "finanza-background-sync-once"

        private fun constraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun kick(context: Context) {
            val request = OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
                .setConstraints(constraints())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_SHOT_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
            WorkManager.getInstance(context).cancelUniqueWork(ONE_SHOT_WORK)
        }
    }
}
