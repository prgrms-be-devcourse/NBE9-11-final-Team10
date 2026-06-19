import { apiFetch } from '../api'
import type { AuthResponse } from '../types'

export async function login(email: string, password: string): Promise<AuthResponse> {
  return apiFetch<AuthResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
    skipAuth: true,
  })
}

export async function signup(data: {
  email: string
  password: string
  name: string
  phoneNumber: string
  birthDate: string
}): Promise<AuthResponse> {
  return apiFetch<AuthResponse>('/api/v1/auth/signup', {
    method: 'POST',
    body: JSON.stringify(data),
    skipAuth: true,
  })
}
