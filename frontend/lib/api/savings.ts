import { apiFetch } from '../api'
import type { SavingsProduct } from '../types'

export async function getDepositProducts(): Promise<SavingsProduct[]> {
  return apiFetch<SavingsProduct[]>('/api/v1/savings/deposit-products')
}

export async function getInstallmentProducts(): Promise<SavingsProduct[]> {
  return apiFetch<SavingsProduct[]>('/api/v1/savings/installment-products')
}

export async function getDepositProduct(productId: string | number): Promise<SavingsProduct> {
  return apiFetch<SavingsProduct>(`/api/v1/savings/deposit-products/${productId}`)
}

export async function getInstallmentProduct(productId: string | number): Promise<SavingsProduct> {
  return apiFetch<SavingsProduct>(`/api/v1/savings/installment-products/${productId}`)
}
