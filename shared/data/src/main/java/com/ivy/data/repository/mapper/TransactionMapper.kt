package com.ivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import com.ivy.base.model.TransactionType
import com.ivy.base.time.convertToLocal
import com.ivy.data.InMemoryDataStore
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.common.Value
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

// TODO: !NOTE: Write unit tests to validate the mapper behavior
class TransactionMapper @Inject constructor(
    private val inMemoryDataStore: InMemoryDataStore
) {
    fun Transaction.toEntity(): TransactionEntity {
        val timeLocal = time.convertToLocal().toLocalDateTime()
        return TransactionEntity(
            accountId = when (this) {
                is Expense -> account.value
                is Income -> account.value
                is Transfer -> fromAccount.value
            },
            type = when (this) {
                is Expense -> TransactionType.EXPENSE
                is Income -> TransactionType.INCOME
                is Transfer -> TransactionType.TRANSFER
            },
            amount = when (this) {
                is Expense -> value.amount.value
                is Income -> value.amount.value
                is Transfer -> fromValue.amount.value
            },
            toAccountId = if (this is Transfer) {
                toAccount.value
            } else {
                null
            },
            toAmount = if (this is Transfer) {
                toValue.amount.value
            } else {
                null
            },
            title = title?.value,
            description = description?.value,
            dateTime = timeLocal,
            categoryId = category?.value,
            // TODO: Use Instant for the time type in the DB
            dueDate = timeLocal.takeIf { !settled },
            recurringRuleId = metadata.recurringRuleId,
            attachmentUrl = null,
            loanId = metadata.loanId,
            loanRecordId = metadata.loanRecordId,
            isSynced = true, // TODO: Remove this
            isDeleted = removed,
            id = id.value
        )
    }

    fun TransactionEntity.toDomain(): Either<String, Transaction> = either {
        val accountId = AccountId(accountId).takeIf {
            it in inMemoryDataStore.accountsIds.value
        } ?: raise("Invalid account id '$accountId' found in entity: ${this@toDomain}")
        // TODO: [Performance] This is inefficient
        val account = inMemoryDataStore.accounts.value.firstOrNull { it.id == accountId }
            ?: raise("Couldn't find account with id '$accountId' in entity: ${this@toDomain}")

        val category = categoryId?.let(::CategoryId).takeIf {
            it in inMemoryDataStore.categoriesIds.value
        }
        // TODO: Handle Instant <> Local conversion properly
        val time = (dateTime ?: dueDate)?.toInstant(ZoneOffset.UTC)
            ?: raise("Missing transaction time for entity: ${this@toDomain}")

        val titleNotBlank = title?.let(NotBlankTrimmedString::from)?.getOrNull()
        val descriptionNotBlank = description?.let(NotBlankTrimmedString::from)?.getOrNull()
        val settled = dateTime != null
        val metadata = TransactionMetadata(
            recurringRuleId = recurringRuleId,
            loanId = loanId,
            loanRecordId = loanRecordId
        )
        val lastUpdated = Instant.EPOCH // TODO: Wire that when the DB is updated

        when (type) {
            TransactionType.INCOME -> {
                Income(
                    id = TransactionId(value = id),
                    title = titleNotBlank,
                    description = descriptionNotBlank,
                    category = category,
                    time = time,
                    settled = settled,
                    metadata = metadata,
                    lastUpdated = lastUpdated,
                    removed = isDeleted,
                    value = Value(
                        amount = PositiveDouble.from(amount).bind(),
                        // TODO: Add support for transaction currency in the DB
                        asset = account.asset
                    ),
                    account = accountId
                )
            }

            TransactionType.EXPENSE -> {
                Expense(
                    id = TransactionId(value = id),
                    title = titleNotBlank,
                    description = descriptionNotBlank,
                    category = category,
                    time = time,
                    settled = settled,
                    metadata = metadata,
                    lastUpdated = lastUpdated,
                    removed = isDeleted,
                    value = Value(
                        amount = PositiveDouble.from(amount).bind(),
                        // TODO: Add support for transaction currency in the DB
                        asset = account.asset
                    ),
                    account = accountId
                )
            }

            TransactionType.TRANSFER -> {
                Transfer(
                    id = TransactionId(value = id),
                    title = titleNotBlank,
                    description = descriptionNotBlank,
                    category = category,
                    time = time,
                    settled = settled,
                    metadata = metadata,
                    lastUpdated = lastUpdated,
                    removed = isDeleted,
                    fromAccount = accountId,
                    fromValue = Value(
                        amount = PositiveDouble.from(amount).bind(),
                        // TODO: Add support for transaction currency in the DB
                        asset = account.asset
                    ),
                    toAccount = TODO(),
                    toValue = TODO()
                )
            }
        }
        TODO()
    }
}