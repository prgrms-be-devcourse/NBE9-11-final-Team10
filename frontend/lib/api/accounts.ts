import { apiFetch } from '../api'
import type { Account } from '../types'

export async function getAccounts(userId: string | number): Promise<Account[]> {
  return apiFetch<Account[]>(`/api/v1/accounts?userId=${userId}`)
}

export async function getAccount(
  accountId: string | number,
  userId: string | number,
): Promise<Account> {
  return apiFetch<Account>(`/api/v1/accounts/${accountId}?userId=${userId}`)
}

export async function createAccount(
  userId: string | number,
  data: { nickname: string; accountType: string },
): Promise<Account> {
  return apiFetch<Account>(`/api/v1/accounts?userId=${userId}`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function updateAccountNickname(
  accountId: string | number,
  userId: string | number,
  nickname: string,
): Promise<Account> {
  return apiFetch<Account>(
    `/api/v1/accounts/${accountId}/nickname?userId=${userId}`,
    { method: 'PATCH', body: JSON.stringify({ nickname }) },
  )
}

export async function closeAccount(
  accountId: string | number,
  userId: string | number,
): Promise<void> {
  return apiFetch<void>(`/api/v1/accounts/${accountId}/close?userId=${userId}`, {
    method: 'POST',
  })
}
