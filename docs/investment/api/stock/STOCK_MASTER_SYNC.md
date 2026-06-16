# KOSPI 종목 마스터 동기화

## 목적

한국투자증권(KIS)에서 제공하는 KOSPI 종목 마스터 파일을 다운로드하여 종목 정보를 최신화한다.

현재 프로젝트에서는 KOSPI 종목만 관리한다.

---

# 다운로드 URL

실전 서버 기준

text https://new.real.download.dws.co.kr/common/master/kospi_code.mst.zip

동기화 시 다음 순서로 수행한다.

text 1. mst.zip 다운로드 2. 압축 해제 3. kospi_code.mst 파싱 4. Stock 테이블 최신화

---

# 파일 구조

압축 해제 시 다음 파일이 생성된다.

text kospi_code.mst

해당 파일은 고정 길이(Fixed Width) 형식이다.

CSV가 아니다.

---

# 실제 파일 예시

```text
000120   KR7000120006CJ대한통운                              ST2001900000000 NN9NNY NNNNNNNNN0NNNNNNYY0000798000000100001NNN00NNN000000100N0900000000873770000000050001956070200000000002281200000000011406172000012       0 NYY00003214500000092100000055400379000005.9220260331000018204720NNN
005610   KR7005610001삼립                                    ST3002700050000 NN0NNN NNNNNNNNN0NNNNNNNN0000404000000100001NNN00NNN000000100N0900000000058580000000050001975050200000000000862900000000004314504500012       0 NNY000008123-00000043-00000082-0068-00000.5320260331000003486   NNN
005680   KR7005680004삼영전자                                ST3002700130000 NN0NNN NNNNNNNNN0NNNNNNNN0000131200000100001NNN00NNN000000100N0900000000542630000000005001976122800000000002000000000000001000000000012       0 NNY00000039200000002100000004300035000000.7520260331000002624   NNN
005690   KR7005690003파미셀                                  ST2002700130000 NN0NNY NNNNYNNNN0NNNNNNNN0000138100000100001NNN00NNN000000100N0900000011129040000000005001988052000000000006000400000000003000200700012       0 NNY00000064900000004700000006300063000007.5920241231000008286   NNN
005720   KR7005720008넥센                                    ST3002700080000 NN0NNN NNNNNNNNN0NNNNNNNN0000061000000100001NNN00NNN000000100N0900000000161430000000005001987121900000000005254300000000002677198850012       0 NNY00000937500000067600000094100731000007.7920260331000003205   NNN
```

---

# 파싱 방식

KIS 공식 샘플 코드를 기준으로 파싱한다.

파일은 두 영역으로 나뉜다.

## Part 1

행 끝에서 228바이트를 제외한 영역

```text
단축코드 표준코드 한글종목명
```

예시

```text
000120 KR7000120006 CJ대한통운
```

---

## Part 2

행 마지막 228바이트

고정 길이 필드(Fixed Width)

KIS에서 제공하는 field specification을 사용하여 파싱한다.

---

# 주요 필드

현재 프로젝트에서 필요한 필드만 저장한다.

## 단축코드

```text
mksc_shrn_iscd
 ```

실제 거래에 사용하는 종목 코드

예)

text 005930 000660 035720

---

## 표준코드(ISIN)

```text
stnd_iscd
```

예)

```text
KR7005930003 
```

---

## 종목명

```text
hts_kor_isnm
```

예)

```text
 삼성전자 SK하이닉스
```

---

## 상장일자

```text
stck_lstn_date
```

---

## 자본금

```text
cpfn
```

---

## 매출액

text sale_account

---

## 당기순이익

text thtr_ntin

---

## 전일 기준 시가총액(억원)

text prdy_avls_scal

---

## 전일 거래량

text prdy_vol

---

## 거래정지 여부

text trht_yn

---

## 관리종목 여부

text mang_issu_yn

---

## 정리매매 여부

text sltr_yn

---

# Stock 엔티티 매핑

| MST 필드         | Stock          |
|----------------|----------------|
| mksc_shrn_iscd | stockCode      |
| stnd_iscd      | standardCode   |
| hts_kor_isnm   | stockName      |
| stck_lstn_date | listedDate     |
| cpfn           | capitalAmount  |
| sale_account   | salesAmount    |
| thtr_ntin      | netIncome      |
| prdy_avls_scal | marketCap      |
| prdy_vol       | previousVolume |
| trht_yn        | status 판단      |
| mang_issu_yn   | status 판단      |
| sltr_yn        | status 판단      |

---

# 종목 상태 판정

현재 프로젝트 enum

java StockStatus

java ACTIVE SUSPENDED DELISTED

다음 기준으로 상태를 결정한다.

## DELISTED

text 정리매매 = Y

java if ("Y".equals(sltrYn)) { return DELISTED; }

---

## SUSPENDED

text 거래정지 = Y 또는 관리종목 = Y

java if ("Y".equals(trhtYn) || "Y".equals(mangIssuYn)) { return SUSPENDED; }

---

## ACTIVE

text 거래정지 = N 관리종목 = N 정리매매 = N

java return ACTIVE;

---

# 동기화 전략

## 기본 원칙

Stock 테이블은 전체 삭제 후 재삽입하지 않는다.

종목은 다음 엔티티에서 참조된다.

text InvestmentOrder InvestmentHolding StockWatchlist

따라서 기존 PK 및 연관관계를 유지해야 한다.

---

## 신규 종목

text DB 없음 → INSERT

---

## 기존 종목

text DB 존재 → UPDATE

업데이트 대상

text 종목명 상태 상장일 자본금 매출액 당기순이익 시가총액 전일 거래량

---

## Upsert 기준

text stockCode

를 기준으로 수행한다.

text 파일 파싱 ↓ stockCode 조회 ↓ 존재하면 updateMaster() ↓ 없으면 create()

---

## 파일에 존재하지 않는 종목

MVP 단계에서는 별도 처리하지 않는다.

다음과 같은 상황에서 오탐 가능성이 있기 때문이다.

text 파일 다운로드 실패 파일 파싱 실패 일시적인 파일 누락

따라서

text 파일에 존재하지 않는다 = 상장폐지

로 판단하지 않는다.

실제 상태 판단은 MST 파일 내

- 거래정지 여부
- 관리종목 여부
- 정리매매 여부

만을 기준으로 수행한다.

---

# 성능 고려사항

KOSPI 종목 수는 약 800~1000개 수준이다.

따라서

text 파일 다운로드 → 전체 파싱 → DB Upsert

방식으로 처리해도 충분하다.

복잡한 배치 프레임워크(Spring Batch)는 사용하지 않는다.

---

# 동기화 결과 처리

MVP 단계에서는 별도의 동기화 이력 테이블을 관리하지 않는다.

동기화 결과는 로그로만 기록한다.

예)

text Stock master sync started Stock master sync completed. total=950 Stock master sync failed

---

# 구현 원칙

- KIS 공식 MST 파일을 Source Of Truth로 사용한다.
- 종목 조회는 DB만 사용한다.
- 사용자 요청 시 KIS 종목 API를 호출하지 않는다.
- 종목 마스터 동기화는 스케줄러 기반으로 수행한다.
- 종목 상태는 MST 파일 기준으로 계산한다.
- 파일 파싱 실패 시 전체 동기화를 실패 처리한다.
- 종목코드(stockCode)를 기준으로 Upsert 수행한다.
- 기존 종목 PK 및 연관관계를 유지한다.