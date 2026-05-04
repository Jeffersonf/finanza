package com.finanza.v4.data.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.finanza.v4.MainActivity
import com.finanza.v4.R
import com.finanza.v4.data.local.FinanzaDatabase
import com.finanza.v4.data.local.TransactionEntity
import com.finanza.v4.data.repository.DueItem
import com.finanza.v4.data.repository.FinanzaRepository
import com.finanza.v4.data.sync.FinanzaApiClient
import com.finanza.v4.data.sync.SyncPreferences
import com.finanza.v4.widget.FinanzaWidgetUpdater
import com.finanza.v4.widget.WidgetActions
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

class DueReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        showNow(applicationContext)
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "finanza_due_reminders"
        private const val WORK_NAME = "due-reminders"
        private const val NOTIFICATION_ID = 4001

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DueReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            PersistentFinanzaNotification.show(context)
        }

        fun showNow(context: Context) {
            val database = Room.databaseBuilder(
                context.applicationContext,
                FinanzaDatabase::class.java,
                "finanza-v4.db"
            ).fallbackToDestructiveMigration(true).build()

            try {
                val snapshot = runBlocking {
                    val pending = loadPendingTransactions(database)
                    val preferences = buildRepository(context.applicationContext, database).getAppPreferences()
                    val dueOccurrences = dueOccurrences(preferences.dueItems).filter {
                        runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { date -> date <= LocalDate.now().plusDays(7) } == true
                    }
                    PendingSnapshot(pending, dueOccurrences)
                }

                if (snapshot.pending.isNotEmpty() || snapshot.dueOccurrences.isNotEmpty()) {
                    ensureChannel(context.applicationContext)
                    if (ContextCompat.checkSelfPermission(context.applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        NotificationManagerCompat.from(context.applicationContext)
                            .notify(NOTIFICATION_ID, buildNotification(context.applicationContext, snapshot.pending, snapshot.dueOccurrences))
                    }
                } else {
                    NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
                }
            } finally {
                database.close()
            }
        }

        private suspend fun loadPendingTransactions(database: FinanzaDatabase): List<TransactionEntity> {
            val today = LocalDate.now()
            val limit = today.plusDays(3).toString()
            return database.transactionDao()
                .listAll()
                .filter { it.type == "expense" && !it.paid && (it.pending || it.date <= limit) }
        }

        private fun buildRepository(context: Context, database: FinanzaDatabase): FinanzaRepository {
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

        private fun buildNotification(
            context: Context,
            pending: List<TransactionEntity>,
            dueOccurrences: List<DueOccurrence>
        ): Notification {
            val totalPending = pending.sumOf { it.amountCents }
            val totalDue = dueOccurrences.sumOf { it.item.amountCents }
            val title = if (dueOccurrences.isNotEmpty()) {
                context.getString(R.string.due_notification_title, dueOccurrences.size)
            } else {
                context.getString(R.string.due_notification_pending_title, pending.size)
            }
            val text = if (dueOccurrences.isNotEmpty()) {
                val next = dueOccurrences.first()
                context.getString(
                    R.string.due_notification_next,
                    next.item.name,
                    money(totalDue),
                    formatDate(next.date)
                )
            } else {
                context.getString(R.string.due_notification_pending_text, money(totalPending))
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(openDueIntent(context))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val next = dueOccurrences.firstOrNull()
            if (next != null) {
                builder
                    .addAction(0, context.getString(R.string.due_notification_open), openDueIntent(context))
                    .addAction(0, context.getString(R.string.due_notification_pay), dueActionIntent(context, DueNotificationReceiver.KIND_PAY, next.item.id, next.date, 4011))
                    .addAction(0, context.getString(R.string.due_notification_postpone), dueActionIntent(context, DueNotificationReceiver.KIND_POSTPONE, next.item.id, next.date, 4012))
            } else {
                builder.addAction(0, context.getString(R.string.due_notification_open), openDueIntent(context))
            }
            return builder.build()
        }

        private fun openDueIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetActions.EXTRA_ACTION, WidgetActions.ACTION_OPEN_DUE)
            }
            return PendingIntent.getActivity(
                context,
                4010,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun dueActionIntent(context: Context, kind: String, itemId: String, date: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, DueNotificationReceiver::class.java).apply {
                putExtra(DueNotificationReceiver.EXTRA_KIND, kind)
                putExtra(DueNotificationReceiver.EXTRA_ITEM_ID, itemId)
                putExtra(DueNotificationReceiver.EXTRA_DATE, date)
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun ensureChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lembretes financeiros",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        private fun dueOccurrences(items: List<DueItem>): List<DueOccurrence> {
            val today = LocalDate.now()
            val until = today.plusDays(45)
            return items.filter { it.active }.flatMap { item ->
                if (item.recurrence == "once") {
                    val date = runCatching { LocalDate.parse(item.nextDueDate) }.getOrDefault(today)
                    val key = YearMonth.from(date).toString()
                    if (date in today..until && key !in item.paidKeys) listOf(DueOccurrence(item, date.toString())) else emptyList()
                } else {
                    (0..2).mapNotNull { offset ->
                        val ym = YearMonth.from(today).plusMonths(offset.toLong())
                        val date = ym.atDay(item.dueDay.coerceAtMost(ym.lengthOfMonth()))
                        if (date in today..until && ym.toString() !in item.paidKeys) DueOccurrence(item, date.toString()) else null
                    }
                }
            }.sortedBy { it.date }
        }

        private fun money(cents: Long): String = "R$ %.2f".format(cents / 100.0)

        private fun formatDate(date: String): String {
            return runCatching {
                val d = LocalDate.parse(date)
                "%02d/%02d".format(d.dayOfMonth, d.monthValue)
            }.getOrDefault(date)
        }
    }
}

private data class DueOccurrence(val item: DueItem, val date: String)
private data class PendingSnapshot(
    val pending: List<TransactionEntity>,
    val dueOccurrences: List<DueOccurrence>
)
