// ──────────────────────────────────────────────
// Auth
// ──────────────────────────────────────────────
export interface User {
  id: number | string
  email: string
  name: string
  phoneNumber?: string
  birthDate?: string
  identityVerified?: boolean
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: User
}

// ──────────────────────────────────────────────
// Account
// ──────────────────────────────────────────────
export type AccountStatus = 'ACTIVE' | 'SUSPENDED' | 'CLOSED'
export type AccountType = 'DEPOSIT'

export interface Account {
  id: number | string
  accountNumber: string
  nickname: string
  accountType: AccountType
  balance: number
  status: AccountStatus
  createdAt: string
  updatedAt?: string
}

// ──────────────────────────────────────────────
// Transaction
// ──────────────────────────────────────────────
export type TransactionDirection = 'IN' | 'OUT'

export interface Transaction {
  id: number | string
  accountId: number | string
  amount: number
  direction: TransactionDirection
  counterpartyName?: string
  memo?: string
  balanceAfter?: number
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalPages: number
  totalElements: number
  number: number
  size: number
}

export interface TransactionFilter {
  startDate?: string
  endDate?: string
  direction?: TransactionDirection
  minAmount?: number
  maxAmount?: number
  counterpartyName?: string
  page?: number
  sortDirection?: 'ASC' | 'DESC'
}

// ──────────────────────────────────────────────
// Transfer
// ──────────────────────────────────────────────
export interface DepositRequest {
  accountId: number | string
  amount: number
  memo?: string
}

export interface TransferRequest {
  senderAccountId: number | string
  receiverAccountNumber: string
  amount: number
  memo?: string
}

export interface TransferResult {
  success: boolean
  transactionId?: number | string
  amount: number
  memo?: string
  receiverAccountNumber?: string
  createdAt?: string
}

// ──────────────────────────────────────────────
// Identity Verification
// ──────────────────────────────────────────────
export type VerificationStatus =
  | 'OCR_PENDING'
  | 'OCR_COMPLETED'
  | 'GOVERNMENT_VERIFIED'
  | 'ONE_WON_PENDING'
  | 'COMPLETED'
  | 'FAILED'

export interface VerificationResponse {
  verificationId?: string | number
  status: VerificationStatus
  message?: string
}

// ──────────────────────────────────────────────
// Savings Products
// ──────────────────────────────────────────────
export type SavingsType = 'DEPOSIT' | 'INSTALLMENT'

export interface SavingsProduct {
  id: number | string
  name: string
  bankName: string
  bankCode?: string
  type: SavingsType
  interestRate: number
  periodMonth: number
  minAmount: number
  maxAmount?: number
  monthlyLimit?: number
  terms?: string
}

// ──────────────────────────────────────────────
// API Error
// ──────────────────────────────────────────────
export interface ApiError {
  code: string
  message: string
  details?: { field: string; reason: string }[]
}
