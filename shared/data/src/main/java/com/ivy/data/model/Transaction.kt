package com.ivy.data.model

import com.ivy.base.model.TransactionType
import com.ivy.data.model.common.Value
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.sync.Syncable
import com.ivy.data.model.sync.UniqueId
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@JvmInline
value class TransactionId(override val value: UUID) : UniqueId

sealed interface Transaction : Syncable {
    override val id: TransactionId
    val title: NotBlankTrimmedString?
    val description: NotBlankTrimmedString?
    val category: CategoryId?
    val time: Instant
    val settled: Boolean
    val metadata: TransactionMetadata
}

data class Income(
    override val id: TransactionId,
    override val title: NotBlankTrimmedString?,
    override val description: NotBlankTrimmedString?,
    override val category: CategoryId?,
    override val time: Instant,
    override val settled: Boolean,
    override val metadata: TransactionMetadata,
    override val lastUpdated: Instant,
    override val removed: Boolean,
    val value: Value,
    val account: AccountId,
) : Transaction

data class Expense(
    override val id: TransactionId,
    override val title: NotBlankTrimmedString?,
    override val description: NotBlankTrimmedString?,
    override val category: CategoryId?,
    override val time: Instant,
    override val settled: Boolean,
    override val metadata: TransactionMetadata,
    override val lastUpdated: Instant,
    override val removed: Boolean,
    val value: Value,
    val account: AccountId,
) : Transaction

data class Transfer(
    override val id: TransactionId,
    override val title: NotBlankTrimmedString?,
    override val description: NotBlankTrimmedString?,
    override val category: CategoryId?,
    override val time: Instant,
    override val settled: Boolean,
    override val metadata: TransactionMetadata,
    override val lastUpdated: Instant,
    override val removed: Boolean,
    val fromAccount: AccountId,
    val fromValue: Value,
    val toAccount: AccountId,
    val toValue: Value,
) : Transaction

@Suppress("DataClassTypedIDs")
data class TransactionMetadata(
    val recurringRuleId: UUID?,
    // This refers to the loan id that is linked with a transaction
    val loanId: UUID? = null,
    // This refers to the loan record id that is linked with a transaction
    val loanRecordId: UUID?,
)

fun Transaction.getValue(): BigDecimal =
    when (this) {
        is Expense -> value.amount.value.toBigDecimal()
        is Income -> value.amount.value.toBigDecimal()
        is Transfer -> fromValue.amount.value.toBigDecimal()
    }

fun Transaction.getAccountId(): UUID =
    when (this) {
        is Expense -> account.value
        is Income -> account.value
        is Transfer -> fromAccount.value
    }

fun Transaction.getTransactionType(): TransactionType {
    return when (this) {
        is Expense -> TransactionType.EXPENSE
        is Income -> TransactionType.INCOME
        is Transfer -> TransactionType.TRANSFER
    }
}