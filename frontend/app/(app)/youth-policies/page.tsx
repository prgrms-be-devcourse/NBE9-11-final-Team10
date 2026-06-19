import { Clock, Building2 } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

// Placeholder cards for future policy data
const policyPlaceholders = [
  { title: '청년 도약 계좌', description: '월 최대 70만원 납입, 정부 기여금 지원', tag: '금융' },
  { title: '청년 일자리 도약 장려금', description: '중소기업 취업 청년 지원금 지급', tag: '취업' },
  { title: '청년 주거 급여', description: '임차료 지원 및 주거 안정 지원', tag: '주거' },
  { title: '청년 내일 저축계좌', description: '근로·사업 소득 청년 자산 형성 지원', tag: '저축' },
]

export default function YouthPoliciesPage() {
  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">청년 정책</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            청년을 위한 다양한 정부 지원 정책을 확인하세요.
          </p>
        </div>
        <Badge variant="secondary" className="flex items-center gap-1">
          <Clock className="size-3" />
          준비 중
        </Badge>
      </div>

      {/* Coming soon notice */}
      <Card className="border-border bg-muted/50">
        <CardContent className="py-8 text-center">
          <Building2 className="size-10 text-muted-foreground mx-auto mb-3" />
          <p className="text-base font-semibold text-foreground mb-1">서비스 준비 중입니다</p>
          <p className="text-sm text-muted-foreground leading-relaxed">
            청년 정책 정보 서비스는 현재 개발 중입니다.
            <br />
            곧 다양한 정부 지원 정책 정보를 제공할 예정입니다.
          </p>
        </CardContent>
      </Card>

      {/* Preview placeholder cards */}
      <div>
        <p className="text-xs font-medium text-muted-foreground mb-3">곧 제공 예정 정책 미리보기</p>
        <div className="flex flex-col gap-3">
          {policyPlaceholders.map((policy) => (
            <PolicyPlaceholderCard key={policy.title} {...policy} />
          ))}
        </div>
      </div>
    </div>
  )
}

function PolicyPlaceholderCard({
  title,
  description,
  tag,
}: {
  title: string
  description: string
  tag: string
}) {
  return (
    <Card className="border-border opacity-60">
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-semibold text-foreground">{title}</CardTitle>
          <Badge variant="outline" className="text-xs">
            {tag}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">{description}</p>
      </CardContent>
    </Card>
  )
}
