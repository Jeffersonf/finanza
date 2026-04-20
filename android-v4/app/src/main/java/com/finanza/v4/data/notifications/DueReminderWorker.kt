package com.finanza.v4.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import com.finanza.v4.R
import com.finanza.v4.data.local.FinanzaDatabase
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class DueReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val database = Room.databaseBuilder(
            applicationContext,
            FinanzaDatabase::class.java,
            "finanza-v4.db"
        ).fallbackToDestructiveMigration(true).build()

        val today = LocalDate.now()
        val limit = today.plusDays(3).toString()
        val pending = database.transactionDao()
            .listAll()
            .filter { it.type == "expense" && !it.paid && (it.pending || it.date <= limit) }

        if (pending.isNotEmpty()) {
            ensureChannel(applicationContext)
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                val total = pending.sumOf { it.amountCents } / 100.0
                val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("Finanza")
                    .setContentText("${pending.size} despesa(s) pendente(s): R$ %.2f".format(total))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat.from(applicationContext).notify(4001, notification)
            }
        }

        database.close()
        return Result.success()
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembretes financeiros",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "finanza_due_reminders"
        private const val WORK_NAME = "due-reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DueReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
