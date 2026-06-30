import { apiFetch } from '../api'
import type { User } from '../types'

export async function getMe(): Promise<User> {
  return apiFetch<User>('/api/v1/users/me')
}

export interface UserProfile {
  userId: number
  birthYear?: number
  region?: string
  occupationStatus?: string
  financialInterests?: string[]
}

export async function getMyProfile(): Promise<UserProfile> {
  return apiFetch<UserProfile>('/api/v1/users/me/profile')
}

// 변경 즉시 서버에서 기존 Refresh Token을 삭제하고 현재 Access Token도 블랙리스트에
// 등록한다 — 호출 성공 시 프론트에서도 곧바로 로그아웃 처리를 해줘야 한다.
export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return apiFetch<void>('/api/v1/users/me/password', {
    method: 'PATCH',
    body: JSON.stringify({ currentPassword, newPassword }),
  })
}

// 계정 상태를 WITHDRAWN으로 변경하고 Refresh Token을 삭제한다 — 비밀번호 변경과 마찬가지로
// 성공 시 프론트에서 즉시 로그아웃 처리가 필요하다.
export async function withdraw(): Promise<void> {
  return apiFetch<void>('/api/v1/users/me', {
    method: 'DELETE',
  })
}
