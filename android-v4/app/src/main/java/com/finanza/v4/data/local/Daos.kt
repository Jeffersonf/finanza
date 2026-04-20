package com.finanza.v4.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY name")
    suspend fun listAll(): List<AccountEntity>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(accounts: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Query("UPDATE accounts SET balanceCents = balanceCents + :deltaCents WHERE id = :id")
    suspend fun adjustBalance(id: String, deltaCents: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    suspend fun listAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE date LIKE :month || '%' ORDER BY date DESC")
    fun observeMonth(month: String): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE date LIKE :month || '%'
          AND (:category IS NULL OR category = :category)
          AND (:type IS NULL OR type = :type)
        ORDER BY date DESC, createdAt DESC
        """
    )
    fun observeFiltered(
        month: String,
        category: String?,
        type: String?
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun countByAccount(accountId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Query("UPDATE transactions SET paid = :paid, pending = CASE WHEN :paid THEN 0 ELSE pending END WHERE id = :id")
    suspend fun setPaid(id: String, paid: Boolean)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month ORDER BY category")
    fun observeMonth(month: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY month DESC, category")
    suspend fun listAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(budgets: List<BudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY deadline, name")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY deadline, name")
    suspend fun listAll(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(goals: List<GoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name")
    suspend fun listAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists ORDER BY position, name")
    fun observeLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists ORDER BY position, name")
    suspend fun listAll(): List<ShoppingListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lists: List<ShoppingListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(list: ShoppingListEntity)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAll()
}

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items ORDER BY bought, createdMs")
    fun observeItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items ORDER BY bought, createdMs")
    suspend fun listAll(): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ShoppingItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET bought = :bought WHERE id = :id")
    suspend fun setBought(id: String, bought: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteByListId(listId: String)

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 'settings' LIMIT 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 'settings' LIMIT 1")
    suspend fun get(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)

    @Query("DELETE FROM app_settings")
    suspend fun deleteAll()
}
