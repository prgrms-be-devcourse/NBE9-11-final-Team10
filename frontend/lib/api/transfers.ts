import { apiFetch } from '../api'
import type { DepositRequest, TransferRequest, TransferResult } from '../types'

export async function deposit(
  data: DepositRequest,
  idempotencyKey: string,
): Promise<TransferResult> {
  const response = await apiFetch<{
    transactionId: number
    accountId: number
    type: string
    amount: number
    balanceBefore: number
    balanceAfter: number
    memo?: string
    transactedAt: string
  }>('/api/v1/transfers/topUp', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(data),
  })

  return {
    success: true,
    transactionId: response.transactionId,
    accountId: response.accountId,
    amount: response.amount,
    balanceBefore: response.balanceBefore,
    balanceAfter: response.balanceAfter,
    memo: response.memo,
    createdAt: response.transactedAt,
  }
}

export async function transfer(
  data: TransferRequest,
  idempotencyKey: string,
): Promise<TransferResult> {
  const response = await apiFetch<{
    transferId: number
    status: string
    senderAccountId: number
    senderAccountNumber: string
    receiverAccountNumber: string
    amount: number
    senderBalanceAfter: number
    memo?: string
    transferredAt: string
  }>('/api/v1/transfers', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(data),
  })

  return {
    success: response.status === 'SUCCESS',
    transferId: response.transferId,
    status: response.status,
    senderAccountId: response.senderAccountId,
    senderAccountNumber: response.senderAccountNumber,
    receiverAccountNumber: response.receiverAccountNumber,
    amount: response.amount,
    senderBalanceAfter: response.senderBalanceAfter,
    memo: response.memo,
    createdAt: response.transferredAt,
  }
}
