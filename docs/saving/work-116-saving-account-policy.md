# WORK-116 예/적금 전용 계좌 정책 정리

## 목적

예금/적금 가입 정보를 단순 Saving 엔티티의 금액 필드로만 관리하지 않고, 별도 `Account`와 연결해 실제 돈의 이동 흐름을 계좌 단위로 추적한다.

예/적금 전용 계좌는 일반 입금/송금 계좌가 아니라 Saving 도메인의 가입, 자동이체, 중도해지, 만기 흐름에서만 금액이 이동하는 계좌로 사용한다.

## 적용 범위

| 도메인 | 적용 내용 |
| --- | --- |
| Account | 예금/적금 전용 계좌 타입 추가 |
| Saving | 예금/적금 가입 시 전용 Account 생성 및 참조 |
| Transfer | 일반 송금/입금에서 예금/적금 계좌 차단 |
| Transaction | 가입/납입/해지/만기 시 양쪽 계좌 거래내역 기록 |
| Flyway | AccountType, Saving Account 참조 컬럼, 거래내역 타입 추가 |

## 계좌 타입 정책

`AccountType`은 다음 값을 사용한다.

| 타입 | 의미 | 일반 계좌 개설 API | 일반 송금 출금 | 일반 송금 수취/입금 |
| --- | --- | --- | --- | --- |
| `DEPOSIT` | 입출금계좌 | 가능 | 가능 | 가능 |
| `SAVING_DEPOSIT` | 예금 전용 계좌 | 불가 | 불가 | 불가 |
| `SAVING_INSTALLMENT` | 적금 전용 계좌 | 불가 | 불가 | 불가 |

일반 계좌 개설 API는 `DEPOSIT` 계좌만 생성한다.

예금/적금 전용 계좌는 Saving 가입 로직에서만 생성한다.

## 예금 가입 흐름

```text
1. 사용자의 입출금계좌(DEPOSIT)를 조회하고 락을 건다.
2. 가입 금액을 검증한다.
3. 입출금계좌에서 가입 금액을 출금한다.
4. 예금 전용 Account(SAVING_DEPOSIT)를 생성한다.
5. 예금 전용 Account에 가입 금액을 입금한다.
6. Deposit 엔티티가 입출금계좌와 예금 전용 Account를 함께 참조한다.
7. 입출금계좌 OUT 거래내역과 예금 전용 Account IN 거래내역을 저장한다.
```

## 적금 가입 흐름

```text
1. 사용자의 입출금계좌(DEPOSIT)를 조회하고 락을 건다.
2. 월 납입액과 목표 금액을 검증한다.
3. 입출금계좌에서 1회차 월 납입액을 출금한다.
4. 적금 전용 Account(SAVING_INSTALLMENT)를 생성한다.
5. 적금 전용 Account에 1회차 월 납입액을 입금한다.
6. Installment 엔티티가 입출금계좌와 적금 전용 Account를 함께 참조한다.
7. 입출금계좌 OUT 거래내역과 적금 전용 Account IN 거래내역을 저장한다.
```

## 자동이체 흐름

적금 자동이체는 일반 송금이 아니라 Saving 도메인의 납입 흐름으로 처리한다.

```text
1. 납입 대상 Installment를 조회하고 입출금계좌/적금 전용 Account를 함께 락 처리한다.
2. 입출금계좌가 ACTIVE인지 확인한다.
3. 입출금계좌 잔액이 월 납입액 이상인지 확인한다.
4. 입출금계좌에서 월 납입액을 출금한다.
5. 적금 전용 Account에 월 납입액을 입금한다.
6. paidAmount를 증가시킨다.
7. 양쪽 계좌 거래내역을 저장한다.
```

## 중도해지 흐름

예금/적금 중도해지는 Saving 도메인에서만 처리한다.

```text
1. Deposit 또는 Installment를 조회하고 입출금계좌/예적금 전용 Account를 함께 락 처리한다.
2. 상태가 ACTIVE인지 확인한다.
4. 중도해지 이자를 계산한다.
5. 예적금 전용 Account에서 원금 또는 납입금을 출금한다.
6. 예적금 전용 Account를 CLOSED 상태로 변경한다.
7. 연결 입출금계좌에 원금/납입금 + 이자를 입금한다.
8. 양쪽 계좌 거래내역을 저장한다.
9. Saving 상태를 CANCELLED로 변경한다.
```

## 만기 처리 흐름

만기 처리는 사용자 요청 또는 스케줄러에서 Saving 도메인 로직으로 처리한다.

```text
1. 만기 대상 Deposit 또는 Installment를 조회하고 입출금계좌/예적금 전용 Account를 함께 락 처리한다.
2. 상태가 ACTIVE인지 확인한다.
3. 만기일이 지났는지 확인한다.
4. 만기 이자를 계산한다.
5. 예적금 전용 Account에서 원금 또는 납입금을 출금한다.
6. 예적금 전용 Account를 CLOSED 상태로 변경한다.
7. 연결 입출금계좌에 원금/납입금 + 이자를 입금한다.
8. 양쪽 계좌 거래내역을 저장한다.
9. Saving 상태를 MATURED로 변경한다.
```

## Transfer 제한 정책

예금/적금은 추가 입금 불가 상품으로 정의한다.

따라서 일반 입금/송금 API에서는 입출금계좌만 사용할 수 있다.

| 상황 | 허용 여부 | 실패 코드 |
| --- | --- | --- |
| `DEPOSIT` 계좌에서 송금 출금 | 허용 | - |
| `SAVING_DEPOSIT`, `SAVING_INSTALLMENT` 계좌에서 송금 출금 | 차단 | `INVALID_SENDER_ACCOUNT_TYPE` |
| `DEPOSIT` 계좌로 송금 수취 | 허용 | - |
| `SAVING_DEPOSIT`, `SAVING_INSTALLMENT` 계좌로 송금 수취 | 차단 | `INVALID_RECEIVER_ACCOUNT_TYPE` |
| `DEPOSIT` 계좌로 일반 입금(topUp) | 허용 | - |
| `SAVING_DEPOSIT`, `SAVING_INSTALLMENT` 계좌로 일반 입금(topUp) | 차단 | `INVALID_RECEIVER_ACCOUNT_TYPE` |

## 거래내역 정책

| 흐름 | 출금 계좌 내역 | 입금 계좌 내역 |
| --- | --- | --- |
| 예금 가입 | 입출금계좌 `OUT` | 예금 전용 Account `IN` |
| 적금 가입 | 입출금계좌 `OUT` | 적금 전용 Account `IN` |
| 적금 자동이체 | 입출금계좌 `OUT` | 적금 전용 Account `IN` |
| 중도해지 | 예적금 전용 Account `OUT` | 입출금계좌 `IN` |
| 만기 | 예적금 전용 Account `OUT` | 입출금계좌 `IN` |

거래 후 잔액(`balanceAfter`)은 각 계좌 기준으로 저장한다.

## Flyway 변경 사항

`V12__extend_account_type_for_saving.sql`에서 다음 변경을 함께 적용한다.

1. `accounts.account_type` enum에 `SAVING_DEPOSIT`, `SAVING_INSTALLMENT` 추가
2. `deposits.saving_account_id` 컬럼 추가
3. `installments.saving_account_id` 컬럼 추가
4. 각 Saving 엔티티와 Account 간 FK/UNIQUE 제약 추가
5. `transaction_histories.type` enum에 Saving 거래 타입 추가

주의:

- PR 전 브랜치 기준에서는 `V13`을 별도로 유지하지 않고 `V12`에 합친다.
- 이미 다른 환경에 적용된 마이그레이션이면 기존 파일 수정 대신 새 버전 파일을 추가해야 한다.
- 현재 정리는 팀원이 아직 해당 `V12`를 적용하지 않았다는 전제를 둔다.

## 테스트 기준

다음 케이스를 검증한다.

- 예금 가입 시 예금 전용 Account 생성
- 적금 가입 시 적금 전용 Account 생성
- 예/적금 가입 시 입출금계좌와 전용 Account 양쪽 거래내역 저장
- 적금 자동이체 시 입출금계좌 출금, 적금 전용 Account 입금, `paidAmount` 증가
- 예/적금 전용 Account는 일반 송금 출금 실패
- 예/적금 전용 Account는 일반 송금 수취 실패
- 예/적금 전용 Account는 일반 입금(topUp) 실패
- 중도해지 시 예/적금 전용 Account 출금 및 CLOSED 처리
- 중도해지 시 연결 입출금계좌로 반환
- 만기 시 예/적금 전용 Account 출금 및 CLOSED 처리
- 만기 시 연결 입출금계좌로 지급

## 후속 작업

- PR 설명에 “예/적금은 추가 입금 불가 상품으로 정의” 정책 명시
- 기존 개발 DB에 예전 V12/V13이 적용된 경우 로컬 Flyway checksum 정리 필요
- Saving API 명세서가 필요하면 별도 문서로 분리
