package com.finanza.v4.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.room.Room
import com.finanza.v4.MainActivity
import com.finanza.v4.R
import com.finanza.v4.data.local.FinanzaDatabase
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.runBlocking

private const val DB_NAME = "finanza-v4.db"

object FinanzaWidgetUpdater {
    @Volatile
    private var database: FinanzaDatabase? = null

    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        updateQuickAddWidgets(appContext, manager)
        updateShoppingWidgets(appContext, manager)
        updateBalanceWidgets(appContext, manager)
        updateDueWidgets(appContext, manager)
    }

    internal fun buildQuickAddViews(context: Context): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_quick_add).apply {
            setOnClickPendingIntent(
                R.id.widget_add_transaction,
                actionIntent(context, WidgetActions.ACTION_TRANSACTION, 101)
            )
        }
    }

    internal fun buildShoppingViews(context: Context): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_shopping_list).apply {
            setOnClickPendingIntent(
                R.id.widget_open_shopping,
                actionIntent(context, WidgetActions.ACTION_OPEN_SHOPPING, 201)
            )
            setOnClickPendingIntent(
                R.id.widget_add_shopping_item,
                actionIntent(context, WidgetActions.ACTION_SHOPPING, 202)
            )
        }
    }

    internal fun buildBalanceViews(context: Context): RemoteViews {
        val snapshot = loadBalanceSnapshot(context)
        return RemoteViews(context.packageName, R.layout.widget_balance).apply {
            setTextViewText(R.id.widget_balance_amount, formatMoney(snapshot.balanceCents))
            setTextViewText(R.id.widget_balance_subtitle, snapshot.subtitle)
            setOnClickPendingIntent(
                R.id.widget_balance_action,
                actionIntent(context, WidgetActions.ACTION_OPEN_HOME, 401)
            )
        }
    }

    internal fun buildDueViews(context: Context): RemoteViews {
        val snapshot = loadDueSnapshot(context)
        return RemoteViews(context.packageName, R.layout.widget_due).apply {
            setTextViewText(R.id.widget_due_count, snapshot.countLabel)
            setTextViewText(R.id.widget_due_subtitle, snapshot.subtitle)
            setOnClickPendingIntent(
                R.id.widget_open_due,
                actionIntent(context, WidgetActions.ACTION_OPEN_DUE, 501)
            )
            setOnClickPendingIntent(
                R.id.widget_due_add_transaction,
                actionIntent(context, WidgetActions.ACTION_TRANSACTION, 502)
            )
        }
    }

    private fun updateQuickAddWidgets(context: Context, manager: AppWidgetManager) {
        val ids = manager.getAppWidgetIds(ComponentName(context, QuickAddWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val views = buildQuickAddViews(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun updateShoppingWidgets(context: Context, manager: AppWidgetManager) {
        val ids = manager.getAppWidgetIds(ComponentName(context, ShoppingListWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val views = buildShoppingViews(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun updateBalanceWidgets(context: Context, manager: AppWidgetManager) {
        val ids = manager.getAppWidgetIds(ComponentName(context, BalanceWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val views = buildBalanceViews(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun updateDueWidgets(context: Context, manager: AppWidgetManager) {
        val ids = manager.getAppWidgetIds(ComponentName(context, DueWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val views = buildDueViews(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun loadBalanceSnapshot(context: Context): BalanceSnapshot = runBlocking {
        val db = database(context)
        val accounts = db.accountDao().listAll()
        val transactions = db.transactionDao().listAll()
        val balanceCents = accounts.sumOf { it.balanceCents }
        val pendingCents = transactions
            .filter { it.type == "expense" && it.pending && !it.paid }
            .sumOf { it.amountCents }
        val subtitle = if (pendingCents > 0L) {
            context.getString(R.string.widget_balance_pending, formatMoney(pendingCents))
        } else {
            context.getString(R.string.widget_balance_clear)
        }
        BalanceSnapshot(balanceCents, subtitle)
    }

    private fun loadDueSnapshot(context: Context): DueSnapshot = runBlocking {
        val db = database(context)
        val pending = db.transactionDao()
            .listAll()
            .filter { it.type == "expense" && it.pending && !it.paid }
            .sortedBy { parseDateOrMax(it.date) }
        val count = pending.size
        val countLabel = context.resources.getQuantityString(R.plurals.widget_due_count, count, count)
        val subtitle = pending.firstOrNull()?.let { tx ->
            context.getString(
                R.string.widget_due_next,
                tx.description,
                formatMoney(tx.amountCents),
                tx.date
            )
        } ?: context.getString(R.string.widget_due_empty)
        DueSnapshot(countLabel, subtitle)
    }

    private fun database(context: Context): FinanzaDatabase {
        database?.let { return it }
        return synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                FinanzaDatabase::class.java,
                DB_NAME
            ).fallbackToDestructiveMigration(true).build().also { database = it }
        }
    }

    private fun formatMoney(cents: Long): String {
        return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(cents / 100.0)
    }

    private fun parseDateOrMax(value: String): LocalDate {
        return try {
            LocalDate.parse(value)
        } catch (_: DateTimeParseException) {
            LocalDate.MAX
        }
    }

    private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WidgetActions.EXTRA_ACTION, action)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}

private data class BalanceSnapshot(
    val balanceCents: Long,
    val subtitle: String
)

private data class DueSnapshot(
    val countLabel: String,
    val subtitle: String
)
