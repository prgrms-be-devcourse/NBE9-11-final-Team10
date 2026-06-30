'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { KeyRound, ShieldAlert, ShieldCheck, UserX } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { useAuth } from '@/contexts/AuthContext'
import { createProfile, getProfile, updateProfile } from '@/lib/api/profile'
import { changePassword, withdraw } from '@/lib/api/users'
import { ApiRequestError } from '@/lib/api'
import { ageGroupOptions, interestOptions, occupationOptions, regionOptions } from '@/lib/profileOptions'
import type {
  AgeGroup,
  FinancialInterest,
  OccupationStatus,
  Region,
  UserProfile,
} from '@/lib/types'

export default function MyPage() {
  const { user, logout } = useAuth()
  const router = useRouter()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const [ageGroup, setAgeGroup] = useState<AgeGroup | ''>('')
  const [region, setRegion] = useState<Region | ''>('')
  const [occupationStatus, setOccupationStatus] = useState<OccupationStatus | ''>('')
  const [interests, setInterests] = useState<Set<FinancialInterest>>(new Set())

  // 로그인 비밀번호 변경
  const [passwordOpen, setPasswordOpen] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('')
  const [passwordFieldErrors, setPasswordFieldErrors] = useState<Record<string, string>>({})
  const [passwordLoading, setPasswordLoading] = useState(false)

  // 회원 탈퇴
  const [withdrawLoading, setWithdrawLoading] = useState(false)

  useEffect(() => {
    let mounted = true

    async function load() {
      try {
        const res = await getProfile()
        if (!mounted) return
        setProfile(res)
        setAgeGroup(res.ageGroup)
        setRegion(res.region)
        setOccupationStatus(res.occupationStatus)
        setInterests(new Set(res.financialInterests))
      } catch (err) {
        // 프로필 미등록은 정상 흐름(신규 등록 폼)이므로 에러로 표시하지 않는다.
        if (!(err instanceof ApiRequestError && err.code === 'PROFILE_NOT_FOUND')) {
          setError(
            err instanceof ApiRequestError ? err.message : '프로필을 불러오는 중 오류가 발생했습니다.',
          )
        }
      } finally {
        if (mounted) setLoading(false)
      }
    }

    load()
    return () => {
      mounted = false
    }
  }, [])

  function toggleInterest(value: FinancialInterest) {
    setInterests((prev) => {
      const next = new Set(prev)
      if (next.has(value)) next.delete(value)
      else next.add(value)
      return next
    })
  }

  async function handleSubmit() {
    if (!ageGroup || !region || !occupationStatus) {
      setError('연령대, 지역, 직업 상태를 모두 선택해 주세요.')
      return
    }
    setError('')
    setSaving(true)
    try {
      const body = {
        ageGroup,
        region,
        occupationStatus,
        financialInterests: Array.from(interests),
      }
      const res = profile ? await updateProfile(body) : await createProfile(body)
      setProfile(res)
      toast.success(profile ? '프로필이 수정되었습니다.' : '프로필이 등록되었습니다.')
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setError(err.message)
      } else {
        setError('저장 중 오류가 발생했습니다.')
      }
    } finally {
      setSaving(false)
    }
  }

  function closePasswordDialog() {
    setPasswordOpen(false)
    setCurrentPassword('')
    setNewPassword('')
    setNewPasswordConfirm('')
    setPasswordFieldErrors({})
  }

  async function handlePasswordChange() {
    const fieldErrs: Record<string, string> = {}
    if (!currentPassword) fieldErrs.currentPassword = '현재 비밀번호를 입력해 주세요.'
    if (!newPassword) fieldErrs.newPassword = '새 비밀번호를 입력해 주세요.'
    else if (newPassword.length < 8) fieldErrs.newPassword = '비밀번호는 8자 이상이어야 합니다.'
    else if (!/^(?=.*[A-Za-z])(?=.*\d).+$/.test(newPassword))
      fieldErrs.newPassword = '비밀번호는 영문과 숫자를 각각 1자 이상 포함해야 합니다.'
    if (newPassword !== newPasswordConfirm) fieldErrs.newPasswordConfirm = '새 비밀번호가 일치하지 않습니다.'
    setPasswordFieldErrors(fieldErrs)
    if (Object.keys(fieldErrs).length > 0) return

    setPasswordLoading(true)
    try {
      await changePassword(currentPassword, newPassword)
      closePasswordDialog()
      toast.success('비밀번호가 변경되었습니다. 다시 로그인해 주세요.')
      // 비밀번호 변경 즉시 서버에서 RT를 삭제하고 현재 AT도 블랙리스트에 등록하므로,
      // 다음 API 호출에서 401이 뜨길 기다리지 않고 여기서 바로 로그아웃 처리한다.
      await logout()
      router.push('/login')
    } catch (err) {
      if (err instanceof ApiRequestError) {
        if (err.details && err.details.length > 0) {
          const errs: Record<string, string> = {}
          for (const d of err.details) errs[d.field] = d.reason
          setPasswordFieldErrors(errs)
        } else {
          // currentPassword 불일치 등 details 없는 오류는 다이얼로그 안에서 토스트로 즉시 표시
          toast.error(err.message)
        }
      } else {
        toast.error('오류가 발생했습니다.')
      }
    } finally {
      setPasswordLoading(false)
    }
  }

  async function handleWithdraw() {
    setWithdrawLoading(true)
    try {
      await withdraw()
      toast.success('회원 탈퇴가 완료되었습니다.')
      await logout()
      router.push('/login')
    } catch (err) {
      toast.error(err instanceof ApiRequestError ? err.message : '오류가 발생했습니다.')
    } finally {
      setWithdrawLoading(false)
    }
  }

  return (
    <div className="flex flex-col gap-5 max-w-md">
      <div>
        <h1 className="text-xl font-bold text-foreground">마이페이지</h1>
        <p className="text-sm text-muted-foreground mt-0.5">내 정보와 프로필을 확인하고 관리하세요.</p>
      </div>

      <Card className="border-border">
        <CardHeader className="pb-3">
          <CardTitle className="text-base">기본 정보</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">이름</span>
            <span className="text-foreground font-medium">{user?.name ?? '-'}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">이메일</span>
            <span className="text-foreground font-medium">{user?.email ?? '-'}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">휴대폰</span>
            <span className="text-foreground font-medium">{user?.phoneNumber ?? '-'}</span>
          </div>
          <div className="flex justify-between items-center text-sm">
            <span className="text-muted-foreground">본인인증</span>
            {user?.identityVerified ? (
              <Badge variant="secondary">
                <ShieldCheck data-icon="inline-start" />
                인증 완료
              </Badge>
            ) : (
              <Badge variant="outline">
                <ShieldAlert data-icon="inline-start" />
                미인증
              </Badge>
            )}
          </div>
        </CardContent>
      </Card>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <Card className="border-border">
        <CardHeader className="pb-3">
          <CardTitle className="text-base">{profile ? '프로필 수정' : '프로필 등록'}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {loading ? (
            <p className="text-sm text-muted-foreground">불러오는 중...</p>
          ) : (
            <>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="ageGroup">연령대</Label>
                <select
                  id="ageGroup"
                  value={ageGroup}
                  onChange={(e) => setAgeGroup(e.target.value as AgeGroup)}
                  className="h-10 rounded-md border border-input bg-background px-3 text-sm text-foreground"
                >
                  <option value="" disabled>
                    선택해 주세요
                  </option>
                  {ageGroupOptions.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="region">지역</Label>
                <select
                  id="region"
                  value={region}
                  onChange={(e) => setRegion(e.target.value as Region)}
                  className="h-10 rounded-md border border-input bg-background px-3 text-sm text-foreground"
                >
                  <option value="" disabled>
                    선택해 주세요
                  </option>
                  {regionOptions.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="occupationStatus">직업 상태</Label>
                <select
                  id="occupationStatus"
                  value={occupationStatus}
                  onChange={(e) => setOccupationStatus(e.target.value as OccupationStatus)}
                  className="h-10 rounded-md border border-input bg-background px-3 text-sm text-foreground"
                >
                  <option value="" disabled>
                    선택해 주세요
                  </option>
                  {occupationOptions.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <Label>관심 금융 분야</Label>
                <div className="flex flex-wrap gap-1.5">
                  {interestOptions.map((o) => {
                    const active = interests.has(o.value)
                    return (
                      <button
                        key={o.value}
                        type="button"
                        onClick={() => toggleInterest(o.value)}
                        className="focus:outline-none"
                      >
                        <Badge variant={active ? 'default' : 'outline'}>{o.label}</Badge>
                      </button>
                    )
                  })}
                </div>
              </div>

              <Button onClick={handleSubmit} disabled={saving} className="w-full">
                {saving ? '저장 중...' : profile ? '수정하기' : '등록하기'}
              </Button>
            </>
          )}
        </CardContent>
      </Card>

      <Card className="border-border">
        <CardHeader className="pb-3">
          <CardTitle className="text-base">계정 관리</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2">
          <Dialog
            open={passwordOpen}
            onOpenChange={(open) => (open ? setPasswordOpen(true) : closePasswordDialog())}
          >
            <DialogTrigger render={<Button variant="outline" className="w-full justify-start" />}>
              <KeyRound data-icon="inline-start" />
              비밀번호 변경
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>비밀번호 변경</DialogTitle>
              </DialogHeader>
              <div className="flex flex-col gap-3 py-2">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="currentPassword">현재 비밀번호</Label>
                  <Input
                    id="currentPassword"
                    type="password"
                    autoComplete="current-password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="현재 비밀번호 입력"
                    aria-invalid={!!passwordFieldErrors.currentPassword}
                  />
                  {passwordFieldErrors.currentPassword && (
                    <p className="text-xs text-destructive">{passwordFieldErrors.currentPassword}</p>
                  )}
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="newPassword">새 비밀번호</Label>
                  <Input
                    id="newPassword"
                    type="password"
                    autoComplete="new-password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="8자 이상"
                    aria-invalid={!!passwordFieldErrors.newPassword}
                  />
                  {passwordFieldErrors.newPassword ? (
                    <p className="text-xs text-destructive">{passwordFieldErrors.newPassword}</p>
                  ) : (
                    <p className="text-xs text-muted-foreground">
                      영문, 숫자를 각각 1자 이상 포함해 8자 이상으로 입력해 주세요.
                    </p>
                  )}
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="newPasswordConfirm">새 비밀번호 확인</Label>
                  <Input
                    id="newPasswordConfirm"
                    type="password"
                    autoComplete="new-password"
                    value={newPasswordConfirm}
                    onChange={(e) => setNewPasswordConfirm(e.target.value)}
                    placeholder="새 비밀번호 한 번 더 입력"
                    aria-invalid={!!passwordFieldErrors.newPasswordConfirm}
                  />
                  {passwordFieldErrors.newPasswordConfirm && (
                    <p className="text-xs text-destructive">{passwordFieldErrors.newPasswordConfirm}</p>
                  )}
                </div>
                <p className="text-xs text-muted-foreground">
                  변경 후에는 보안을 위해 자동으로 로그아웃되며, 새 비밀번호로 다시 로그인해야 합니다.
                </p>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={closePasswordDialog}>
                  취소
                </Button>
                <Button onClick={handlePasswordChange} disabled={passwordLoading}>
                  {passwordLoading ? '변경 중...' : '변경'}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>

          <AlertDialog>
            <AlertDialogTrigger
              render={
                <Button
                  variant="ghost"
                  className="w-full justify-start text-destructive hover:text-destructive hover:bg-destructive/10"
                />
              }
            >
              <UserX data-icon="inline-start" />
              회원 탈퇴
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>정말 탈퇴하시겠습니까?</AlertDialogTitle>
                <AlertDialogDescription>
                  이 작업은 되돌릴 수 없습니다. 탈퇴 즉시 로그아웃되며 모든 서비스 이용이 중단됩니다.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>취소</AlertDialogCancel>
                <AlertDialogAction
                  onClick={(event) => {
                    event.preventDefault()
                    handleWithdraw()
                  }}
                  disabled={withdrawLoading}
                  className="bg-destructive hover:bg-destructive/90"
                >
                  {withdrawLoading ? '처리 중...' : '탈퇴하기'}
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </CardContent>
      </Card>
    </div>
  )
}
