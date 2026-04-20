package com.finanza.v4.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        CategoryEntity::class,
        ShoppingListEntity::class,
        ShoppingItemEntity::class,
        AppSettingsEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class FinanzaDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun categoryDao(): CategoryDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun appSettingsDao(): AppSettingsDao
}
