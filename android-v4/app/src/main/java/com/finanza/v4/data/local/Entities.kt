package com.finanza.v4.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val type: String,
    val balanceCents: Long,
    val yieldRate: Double = 0.0
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val type: String,
    val description: String,
    val category: String,
    val amountCents: Long,
    val date: String,
    val note: String = "",
    val paid: Boolean = false,
    val pending: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val category: String,
    val limitCents: Long,
    val month: String
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val targetCents: Long,
    val currentCents: Long,
    val deadline: String,
    val description: String = "",
    val monthlyCents: Long = 0
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val icon: String,
    val name: String,
    val color: String
)

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val position: Int = 0
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val name: String,
    val qty: String = "",
    val category: String = "",
    val bought: Boolean = false,
    val createdMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: String = "settings",
    val theme: String = "dark",
    val ratesJson: String = "{}",
    val widgetPrefsJson: String = "{}",
    val widgetOrderJson: String = "[]",
    val txView: String = "n",
    val activeList: String? = null
)
