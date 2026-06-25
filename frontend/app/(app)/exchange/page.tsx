'use client'

import { useEffect, useState } from 'react'
import { ArrowRightLeft, Clock3, Plus, ReceiptText, Trash2, WalletCards } from 'lucide-react'
import { toast } from 'sonner'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  closeFxWallet,
  createFxWallet,
  getExchangeCurrencies,
  getFxWallets,
  type ExchangeCurrency,
  type ExchangeCurrencyCode,
  type FxWallet,
} from '@/lib/api/exchanges'
import { ApiRequestError } from '@/lib/api'

const currencies = [
  { code: 'USD', name: '미국 달러' },
  { code: 'JPY', name: '일본 엔' },
  { code: 'EUR', name: '유로' },
]

const orderPreview = [
  {
    id: 1,
    direction: 'KRW → USD',
    amount: '100,000원',
    result: '72.50 USD',
    status: '완료',
    createdAt: '2026-06-17 10:00',
  },
]

export default function ExchangePage() {
  return (
    <div className="flex flex-col gap-5 max-w-3xl">
      <div>
        <h1 className="text-xl font-bold text-foreground">환전</h1>
        <p className="text-sm text-muted-foreground mt-0.5">
          환율을 확인하고 외화 지갑으로 환전하세요.
        </p>
      </div>

      <Tabs defaultValue="exchange">
        <TabsList className="w-full">
          <TabsTrigger value="exchange" className="flex-1">
            <ArrowRightLeft className="size-4 mr-1.5" />
            환전하기
          </TabsTrigger>
          <TabsTrigger value="wallets" className="flex-1">
            <WalletCards className="size-4 mr-1.5" />
            외화지갑
          </TabsTrigger>
          <TabsTrigger value="orders" className="flex-1">
            <ReceiptText className="size-4 mr-1.5" />
            환전내역
          </TabsTrigger>
        </TabsList>

        <TabsContent value="exchange" className="mt-4">
          <ExchangeFormSkeleton />
        </TabsContent>

        <TabsContent value="wallets" className="mt-4">
          <FxWalletTab />
        </TabsContent>

        <TabsContent value="orders" className="mt-4">
          <ExchangeOrdersSkeleton />
        </TabsContent>
      </Tabs>
    </div>
  )
}

function ExchangeFormSkeleton() {
  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_18rem]">
      <Card className="border-border">
        <CardHeader className="pb-3">
          <CardTitle className="text-sm font-medium text-muted-foreground">환전 정보</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="from-currency">출금 통화</Label>
              <Select defaultValue="KRW">
                <SelectTrigger id="from-currency">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="KRW">KRW 원화</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="to-currency">입금 통화</Label>
              <Select defaultValue="USD">
                <SelectTrigger id="to-currency">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {currencies.map((currency) => (
                    <SelectItem key={currency.code} value={currency.code}>
                      {currency.code} {currency.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="exchange-amount">환전 금액</Label>
            <Input id="exchange-amount" type="number" inputMode="numeric" min={1} placeholder="0" />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="krw-account">원화 계좌</Label>
              <Select disabled>
                <SelectTrigger id="krw-account">
                  <SelectValue placeholder="계좌 선택" />
                </SelectTrigger>
              </Select>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="fx-wallet">외화 지갑</Label>
              <Select disabled>
                <SelectTrigger id="fx-wallet">
                  <SelectValue placeholder="지갑 선택" />
                </SelectTrigger>
              </Select>
            </div>
          </div>

          <Button type="button" className="w-full" disabled>
            견적 확인
          </Button>
        </CardContent>
      </Card>

      <Card className="border-border">
        <CardHeader className="pb-3">
          <CardTitle className="text-sm font-medium text-muted-foreground">견적 요약</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <SummaryRow label="적용 환율" value="-" />
          <SummaryRow label="예상 수수료" value="-" />
          <SummaryRow label="예상 입금액" value="-" strong />
          <Button type="button" variant="outline" className="mt-1" disabled>
            환전 실행
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}

function FxWalletTab() {
  const [currencies, setCurrencies] = useState<ExchangeCurrency[]>([])
  const [wallets, setWallets] = useState<FxWallet[]>([])
  const [selectedCurrencyCode, setSelectedCurrencyCode] = useState<ExchangeCurrencyCode | ''>('')
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [closingWalletId, setClosingWalletId] = useState<number | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    loadWalletData()
  }, [])

  async function loadWalletData() {
    setLoading(true)
    setError('')
    try {
      const [currencyData, walletData] = await Promise.all([
        getExchangeCurrencies(),
        getFxWallets(),
      ])
      setCurrencies(currencyData)
      setWallets(walletData)
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : '외화 지갑 정보를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  async function handleCreateWallet() {
    if (!selectedCurrencyCode) return

    setCreating(true)
    setError('')
    try {
      const wallet = await createFxWallet({ currencyCode: selectedCurrencyCode })
      setWallets((prev) => [wallet, ...prev.filter((item) => item.walletId !== wallet.walletId)])
      setSelectedCurrencyCode('')
      toast.success(`${wallet.currencyCode} 지갑을 만들었습니다.`)
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : '외화 지갑 생성에 실패했습니다.')
    } finally {
      setCreating(false)
    }
  }

  async function handleCloseWallet(wallet: FxWallet) {
    const ok = window.confirm(`${wallet.currencyCode} 지갑을 해지할까요?`)
    if (!ok) return

    setClosingWalletId(wallet.walletId)
    setError('')
    try {
      const closedWallet = await closeFxWallet(wallet.walletId)
      setWallets((prev) =>
        prev.map((item) => (item.walletId === closedWallet.walletId ? closedWallet : item)),
      )
      toast.success(`${closedWallet.currencyCode} 지갑을 해지했습니다.`)
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : '외화 지갑 해지에 실패했습니다.')
    } finally {
      setClosingWalletId(null)
    }
  }

  const unavailableCurrencyCodes = new Set(
    wallets
      .filter((wallet) => wallet.status === 'ACTIVE' || wallet.status === 'SUSPENDED')
      .map((wallet) => wallet.currencyCode),
  )

  const creatableCurrencies = currencies.filter(
    (currency) =>
      currency.status === 'ACTIVE'
      && currency.currencyCode !== 'KRW'
      && !unavailableCurrencyCodes.has(currency.currencyCode),
  )

  if (loading) {
    return (
      <div className="flex flex-col gap-3">
        <Skeleton className="h-24 w-full rounded-lg" />
        <div className="grid gap-3 sm:grid-cols-2">
          <Skeleton className="h-32 w-full rounded-lg" />
          <Skeleton className="h-32 w-full rounded-lg" />
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <Card className="border-border">
        <CardHeader className="pb-3">
          <CardTitle className="text-sm font-medium text-muted-foreground">외화 지갑 만들기</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3 sm:grid-cols-[1fr_auto]">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="wallet-currency">통화</Label>
            <Select
              value={selectedCurrencyCode}
              onValueChange={(value) => setSelectedCurrencyCode(value as ExchangeCurrencyCode)}
              disabled={creating || creatableCurrencies.length === 0}
            >
              <SelectTrigger id="wallet-currency">
                <SelectValue placeholder="통화 선택" />
              </SelectTrigger>
              <SelectContent>
                {creatableCurrencies.map((currency) => (
                  <SelectItem key={currency.currencyId} value={currency.currencyCode}>
                    {currency.currencyCode} {currency.currencyName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Button
            type="button"
            className="self-end"
            disabled={!selectedCurrencyCode || creating}
            onClick={handleCreateWallet}
          >
            <Plus data-icon="inline-start" />
            {creating ? '생성 중...' : '지갑 만들기'}
          </Button>
        </CardContent>
      </Card>

      {wallets.length === 0 ? (
        <Card className="border-border">
          <CardContent className="py-10 text-center text-sm text-muted-foreground">
            만든 외화 지갑이 없습니다.
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2">
          {wallets.map((wallet) => (
            <Card key={wallet.walletId} className="border-border">
              <CardContent className="pt-4 flex flex-col gap-3">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2">
                    <div className="size-9 rounded-md bg-primary/10 text-primary flex items-center justify-center">
                      <WalletCards className="size-5" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-foreground">{wallet.currencyCode}</p>
                      <p className="text-xs text-muted-foreground">{fxWalletStatusLabel[wallet.status]}</p>
                    </div>
                  </div>
                  <p className="text-base font-bold text-foreground tabular-nums">
                    {formatFxBalance(wallet.balance)} {wallet.currencyCode}
                  </p>
                </div>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={wallet.status === 'CLOSED' || closingWalletId === wallet.walletId}
                  onClick={() => handleCloseWallet(wallet)}
                >
                  <Trash2 data-icon="inline-start" />
                  {closingWalletId === wallet.walletId ? '해지 중...' : '지갑 해지'}
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}

function ExchangeOrdersSkeleton() {
  return (
    <Card className="border-border">
      <CardHeader className="pb-3">
        <CardTitle className="text-sm font-medium text-muted-foreground">최근 환전내역</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-0">
        {orderPreview.map((order) => (
          <div key={order.id} className="flex items-center justify-between gap-3 py-3 border-b last:border-b-0 border-border">
            <div className="flex items-center gap-3 min-w-0">
              <div className="size-9 rounded-md bg-muted flex items-center justify-center shrink-0">
                <Clock3 className="size-4 text-muted-foreground" />
              </div>
              <div className="min-w-0">
                <p className="text-sm font-medium text-foreground truncate">{order.direction}</p>
                <p className="text-xs text-muted-foreground">{order.createdAt}</p>
              </div>
            </div>
            <div className="text-right shrink-0">
              <p className="text-sm font-semibold text-foreground">{order.result}</p>
              <p className="text-xs text-muted-foreground">
                {order.amount} · {order.status}
              </p>
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}

function SummaryRow({
  label,
  value,
  strong = false,
}: {
  label: string
  value: string
  strong?: boolean
}) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className={`text-sm tabular-nums ${strong ? 'font-bold text-foreground' : 'font-medium text-foreground'}`}>
        {value}
      </span>
    </div>
  )
}

const fxWalletStatusLabel = {
  ACTIVE: '활성',
  SUSPENDED: '거래 제한',
  CLOSED: '해지',
}

function formatFxBalance(value: number) {
  return new Intl.NumberFormat('ko-KR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 8,
  }).format(value)
}
