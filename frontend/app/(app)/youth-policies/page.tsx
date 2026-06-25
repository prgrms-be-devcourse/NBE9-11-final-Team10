'use client'

import { useEffect, useState } from 'react'
import { CalendarDays, ExternalLink, MapPin, Search, Sparkles } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  getYouthPolicies,
  recommendYouthPolicies,
  searchYouthPolicies,
} from '@/lib/api/youth-policies'
import type { RecommendedYouthPolicy } from '@/lib/api/youth-policies'
import type { YouthPolicy } from '@/lib/types'

export default function YouthPoliciesPage() {
  const [policies, setPolicies] = useState<YouthPolicy[]>([])
  const [recommendedPolicies, setRecommendedPolicies] = useState<RecommendedYouthPolicy[]>([])
  const [loading, setLoading] = useState(true)
  const [searching, setSearching] = useState(false)
  const [recommending, setRecommending] = useState(false)
  const [error, setError] = useState('')
  const [age, setAge] = useState('')
  const [region, setRegion] = useState('')
  const [category, setCategory] = useState('')
  const [keyword, setKeyword] = useState('')
  const [query, setQuery] = useState('')

  useEffect(() => {
    getYouthPolicies()
      .then(setPolicies)
      .catch(() => setError('청년정책 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [])

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setSearching(true)
    try {
      const result = await searchYouthPolicies({
        age: parseNumber(age),
        region,
        category,
        keyword,
        size: 30,
      })
      setPolicies(result.content)
    } catch {
      setError('청년정책 검색에 실패했습니다.')
    } finally {
      setSearching(false)
    }
  }

  async function handleRecommend(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (!query.trim()) {
      setError('맞춤 추천을 받으려면 고민이나 관심사를 입력해 주세요.')
      return
    }

    setRecommending(true)
    try {
      const result = await recommendYouthPolicies({
        age: parseNumber(age),
        region,
        category,
        query: query.trim(),
      })
      setRecommendedPolicies(result.recommendedPolicies)
    } catch {
      setError('맞춤 정책 추천에 실패했습니다.')
    } finally {
      setRecommending(false)
    }
  }

  async function resetFilters() {
    setError('')
    setAge('')
    setRegion('')
    setCategory('')
    setKeyword('')
    setRecommendedPolicies([])
    setLoading(true)
    try {
      const result = await getYouthPolicies()
      setPolicies(result)
    } catch {
      setError('청년정책 정보를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex flex-col gap-5">
      <div>
        <h1 className="text-xl font-bold text-foreground">청년 정책</h1>
        <p className="text-sm text-muted-foreground mt-0.5">
          청년을 위한 정부 지원 정책을 확인하세요.
        </p>
      </div>

      <Card className="border-border">
        <CardHeader className="pb-3">
          <CardTitle className="text-sm font-medium text-muted-foreground">정책 찾기</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSearch} className="flex flex-col gap-3">
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="policy-age">나이</Label>
                <Input
                  id="policy-age"
                  inputMode="numeric"
                  placeholder="예: 25"
                  value={age}
                  onChange={(e) => setAge(e.target.value.replace(/\D/g, ''))}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="policy-region">지역</Label>
                <Input
                  id="policy-region"
                  placeholder="예: 서울"
                  value={region}
                  onChange={(e) => setRegion(e.target.value)}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="policy-category">카테고리</Label>
                <Input
                  id="policy-category"
                  placeholder="예: 금융･복지･문화"
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="policy-keyword">키워드</Label>
                <Input
                  id="policy-keyword"
                  placeholder="예: 월세"
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                />
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={resetFilters} disabled={loading || searching}>
                초기화
              </Button>
              <Button type="submit" disabled={searching}>
                <Search data-icon="inline-start" />
                {searching ? '검색 중...' : '검색'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card className="border-border">
        <CardHeader className="pb-3">
          <CardTitle className="text-sm font-medium text-muted-foreground">맞춤 추천</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleRecommend} className="flex flex-col gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="policy-query">고민이나 관심사</Label>
              <Textarea
                id="policy-query"
                placeholder="예: 대학생인데 자취 월세 부담이 커서 주거비 지원을 받고 싶어요."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
            <div className="flex justify-end">
              <Button type="submit" disabled={recommending}>
                <Sparkles data-icon="inline-start" />
                {recommending ? '추천 중...' : '맞춤 정책 추천'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {recommendedPolicies.length > 0 && (
        <div className="flex flex-col gap-3">
          <div>
            <h2 className="text-base font-semibold text-foreground">추천 정책</h2>
            <p className="text-xs text-muted-foreground mt-0.5">입력한 조건과 고민을 기준으로 추천된 정책입니다.</p>
          </div>
          {recommendedPolicies.map((policy) => (
            <PolicyCard key={`recommended-${policy.id}`} policy={policy} />
          ))}
        </div>
      )}

      {loading ? (
        <div className="flex flex-col gap-3">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-36 w-full rounded-lg" />
          ))}
        </div>
      ) : policies.length === 0 ? (
        <Card className="border-border">
          <CardContent className="py-12 text-center">
            <p className="text-sm text-muted-foreground">조회된 정책 정보가 없습니다.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="flex flex-col gap-3">
          {policies.map((policy) => (
            <PolicyCard key={policy.id} policy={policy} />
          ))}
        </div>
      )}
    </div>
  )
}

function PolicyCard({ policy }: { policy: YouthPolicy | RecommendedYouthPolicy }) {
  const ageLabel =
    policy.minAge || policy.maxAge
      ? `${policy.minAge ?? 0}세 ~ ${policy.maxAge ?? '제한 없음'}`
      : '연령 제한 없음'

  return (
    <Card className="border-border">
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between gap-3">
          <CardTitle className="text-sm font-semibold text-foreground leading-relaxed">
            {policy.title}
          </CardTitle>
          {policy.category && (
            <Badge variant="outline" className="text-xs shrink-0">
              {policy.category}
            </Badge>
          )}
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
          {policy.subCategory && <Badge variant="secondary">{policy.subCategory}</Badge>}
          <span>{ageLabel}</span>
          {policy.regionCode && (
            <span className="inline-flex items-center gap-1">
              <MapPin className="size-3" />
              {policy.regionCode}
            </span>
          )}
          {policy.applyPeriod && (
            <span className="inline-flex items-center gap-1">
              <CalendarDays className="size-3" />
              {policy.applyPeriod}
            </span>
          )}
        </div>

        {policy.description && (
          <p className="text-sm text-muted-foreground leading-relaxed line-clamp-3">
            {policy.description}
          </p>
        )}

        {'recommendReason' in policy && policy.recommendReason && (
          <div className="rounded-md border border-primary/20 bg-primary/5 px-3 py-2">
            <p className="text-xs font-medium text-primary mb-1">추천 이유</p>
            <p className="text-sm text-foreground leading-relaxed">{policy.recommendReason}</p>
          </div>
        )}

        {policy.applyUrl && (
          <a
            href={policy.applyUrl}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1 text-sm font-medium text-primary"
          >
            신청 페이지
            <ExternalLink className="size-3.5" />
          </a>
        )}
      </CardContent>
    </Card>
  )
}

function parseNumber(value: string): number | undefined {
  if (!value.trim()) return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}
