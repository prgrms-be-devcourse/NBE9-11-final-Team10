import { apiFetch } from '../api'
import type { PageResponse, Transaction, TransactionFilter } from '../types'

export async function getTransactions(
  accountId: string | number,
  userId: string | number,
  filter: TransactionFilter = {},
): Promise<PageResponse<Transaction>> {
  const params = new URLSearchParams({ userId: String(userId) })
  params.set('page', String(filter.page ?? 0))
  params.set('sortDirection', filter.sortDirection ?? 'DESC')
  if (filter.startDate) params.set('startDate', filter.startDate)
  if (filter.endDate) params.set('endDate', filter.endDate)
  if (filter.direction) params.set('direction', filter.direction)
  if (filter.minAmount != null) params.set('minAmount', String(filter.minAmount))
  if (filter.maxAmount != null) params.set('maxAmount', String(filter.maxAmount))
  if (filter.counterpartyName) params.set('counterpartyName', filter.counterpartyName)

  return apiFetch<PageResponse<Transaction>>(
    `/api/v1/accounts/${accountId}/transactions?${params.toString()}`,
  )
}

export async function getTransaction(
  accountId: string | number,
  transactionId: string | number,
  userId: string | number,
): Promise<Transaction> {
  return apiFetch<Transaction>(
    `/api/v1/accounts/${accountId}/transactions/${transactionId}?userId=${userId}`,
  )
}
