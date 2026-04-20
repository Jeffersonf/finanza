package com.finanza.v4.domain

data class MoneySummary(
    val balanceCents: Long,
    val incomeCents: Long,
    val expenseCents: Long,
    val futureCents: Long
)

data class Account(
    val id: String,
    val name: String,
    val icon: String,
    val type: String,
    val balanceCents: Long,
    val yieldRate: Double
)

data class AccountDraft(
    val id: String? = null,
    val name: String,
    val icon: String,
    val type: String,
    val balanceCents: Long,
    val yieldRate: Double = 0.0
)

data class Budget(
    val id: String,
    val category: String,
    val limitCents: Long,
    val month: String
)

data class BudgetDraft(
    val id: String? = null,
    val category: String,
    val limitCents: Long,
    val month: String
)

data class Transaction(
    val id: String,
    val accountId: String,
    val type: TransactionType,
    val description: String,
    val category: String,
    val amountCents: Long,
    val date: String,
    val paid: Boolean,
    val pending: Boolean
)

data class TransactionDraft(
    val id: String? = null,
    val accountId: String,
    val type: TransactionType,
    val description: String,
    val category: String,
    val amountCents: Long,
    val date: String,
    val note: String = "",
    val pending: Boolean = false
)

enum class TransactionType {
    Income,
    Expense
}

data class Goal(
    val id: String,
    val name: String,
    val icon: String,
    val targetCents: Long,
    val currentCents: Long,
    val deadline: String,
    val description: String,
    val monthlyCents: Long
)

data class GoalDraft(
    val id: String? = null,
    val name: String,
    val icon: String,
    val targetCents: Long,
    val currentCents: Long,
    val deadline: String,
    val description: String,
    val monthlyCents: Long
)

data class ShoppingList(
    val id: String,
    val name: String,
    val icon: String,
    val position: Int
)

data class ShoppingItem(
    val id: String,
    val listId: String,
    val name: String,
    val qty: String,
    val category: String,
    val bought: Boolean,
    val createdMs: Long
)

data class ShoppingListDraft(
    val id: String? = null,
    val name: String,
    val icon: String,
    val position: Int = 0
)

data class ShoppingItemDraft(
    val id: String? = null,
    val listId: String,
    val name: String,
    val qty: String,
    val category: String,
    val bought: Boolean = false
)
