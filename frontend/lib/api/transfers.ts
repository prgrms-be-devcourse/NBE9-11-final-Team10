import { apiFetch } from '../api'
import type { DepositRequest, TransferRequest, TransferResult } from '../types'

export async function deposit(data: DepositRequest): Promise<TransferResult> {
  return apiFetch<TransferResult>('/api/v1/transfers/deposit', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function transfer(data: TransferRequest): Promise<TransferResult> {
  return apiFetch<TransferResult>('/api/v1/transfers', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}
