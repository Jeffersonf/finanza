package com.finanza.v4.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

object WidgetActions {
    const val EXTRA_ACTION = "com.finanza.v4.widget.EXTRA_ACTION"
    const val ACTION_OPEN_HOME = "open_home"
    const val ACTION_OPEN_DUE = "open_due"
    const val ACTION_TRANSACTION = "transaction"
    const val ACTION_SHOPPING = "shopping"
    const val ACTION_OPEN_SHOPPING = "open_shopping"
}

class QuickAddWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, FinanzaWidgetUpdater.buildQuickAddViews(context))
        }
    }
}

class ShoppingListWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, FinanzaWidgetUpdater.buildShoppingViews(context))
        }
    }
}

class BalanceWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, FinanzaWidgetUpdater.buildBalanceViews(context))
        }
    }
}

class DueWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, FinanzaWidgetUpdater.buildDueViews(context))
        }
    }
}
