# 예/적금 전용 Account와 Transfer 락 구조

## 문서 목적

예/적금 가입 구조가 변경되면서 Transfer 도메인에서 어떤 기준으로 일반 송금/입금을 허용하거나 차단하는지 정리한다.

또한 금액 변경 시 어떤 row를 락 잡는지, 왜 Deposit/Installment row를 일반 송금에서 추가로 락 잡지 않는지 설명한다.

## 현재 정책

예/적금은 입출금계좌에 금액을 함께 묶어두는 방식이 아니라, 별도 Account를 생성해 관리한다.

```text
입출금계좌: AccountType.DEPOSIT
예금계좌: AccountType.SAVING_DEPOSIT
적금계좌: AccountType.SAVING_INSTALLMENT
```

일반 송금/입금 정책은 다음과 같다.

| 계좌 타입 | 일반 송금 출금 | 일반 송금 수취 | 일반 입금(topUp) |
| --- | --- | --- | --- |
| `DEPOSIT` | 가능 | 가능 | 가능 |
| `SAVING_DEPOSIT` | 불가 | 불가 | 불가 |
| `SAVING_INSTALLMENT` | 불가 | 불가 | 불가 |

즉, 입출금계좌 자체를 송금 불가로 만드는 정책이 아니라, 예/적금 전용 Account의 일반 송금/입금을 차단하는 정책이다.

## Account와 Deposit/Installment의 역할

예금 가입 시 다음 두 정보가 함께 생긴다.

```text
Account
- 실제 돈이 들어있는 계좌
- accountType = SAVING_DEPOSIT
- balance = 예금 계좌 잔액

Deposit
- 예금 가입 정보
- 상품, 원금, 금리, 만기일, 상태 등을 저장
- savingAccount로 예금 전용 Account를 참조
```

적금도 같은 구조다.

```text
Account
- 실제 돈이 들어있는 계좌
- accountType = SAVING_INSTALLMENT
- balance = 적금 계좌 잔액

Installment
- 적금 가입 정보
- 월 납입액, 목표 금액, 납입 금액, 만기일, 상태 등을 저장
- savingAccount로 적금 전용 Account를 참조
```

정리하면 다음과 같다.

```text
Account = 실제 돈과 계좌 상태 관리
Deposit/Installment = 예/적금 가입 조건과 상태 관리
```

## Transfer에서의 판단 기준

Transfer 도메인은 일반 송금/입금 가능 여부를 `Account.accountType`으로 판단한다.

```java
validateSenderCanTransferOut(senderAccount);
validateReceiverCanTransferIn(receiverAccount);
```

현재 Transfer 도메인은 일반 송금 가능 여부 판단을 위해 `Deposit` 또는 `Installment` 상태를 조회하지 않는다.

중도해지/만기 가능 여부는 Saving 도메인에서 `DepositStatus`, `InstallmentStatus`로 판단한다.

## 일반 입금(topUp) 락 흐름

일반 입금은 대상 Account row를 비관적 락으로 조회한다.

```java
accountRepository.findByIdAndUserIdForUpdate(accountId, userId)
```

Repository에는 `PESSIMISTIC_WRITE`가 적용되어 있다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select a from Account a where a.id = :accountId and a.user.id = :userId")
Optional<Account> findByIdAndUserIdForUpdate(Long accountId, Long userId);
```

처리 순서는 다음과 같다.

```text
1. Account row 락 획득
2. 계좌 ACTIVE 여부 확인
3. accountType이 DEPOSIT인지 확인
4. DEPOSIT이면 입금 진행
5. SAVING_DEPOSIT 또는 SAVING_INSTALLMENT이면 차단
```

## 일반 송금(transfer) 락 흐름

일반 송금은 sender Account와 receiver Account 두 개의 잔액이 함께 변경된다.

따라서 두 Account row를 모두 비관적 락으로 조회한다.

```java
Account first = accountRepository.findByIdForUpdate(firstId);
Account second = accountRepository.findByIdForUpdate(secondId);
```

두 계좌는 항상 ID가 작은 계좌부터 락을 잡는다.

```java
Long firstId = Math.min(senderAccountId, receiverAccountId);
Long secondId = Math.max(senderAccountId, receiverAccountId);
```

이유는 동시에 반대 방향 송금이 들어올 때 데드락 가능성을 줄이기 위해서다.

예:

```text
요청 A: 1번 계좌 → 2번 계좌 송금
요청 B: 2번 계좌 → 1번 계좌 송금
```

항상 작은 ID부터 락을 잡으면 두 요청 모두 같은 순서로 락을 기다리게 된다.

## 왜 Deposit/Installment row를 일반 송금에서 락 잡지 않는가

현재 일반 송금 가능 여부는 Deposit/Installment 상태로 판단하지 않는다.

판단 기준은 Account의 타입이다.

```text
송금 판단에 필요한 값: Account.accountType
락 잡는 대상: Account row
```

즉, 판단 기준이 Account에 있고, Transfer는 이미 Account row를 락 잡고 있다.

따라서 일반 송금에서 Deposit/Installment row를 추가로 조회하거나 락 잡지 않는다.

## 구조 선택 이유

입출금계좌에 예/적금 금액을 함께 묶어두고 Deposit/Installment 상태로 송금 가능 여부를 판단하면, 일반 송금에서도 예/적금 row 상태를 함께 고려해야 한다.

이 경우 다음 고민이 생긴다.

```text
- Account row와 Deposit/Installment row를 어떤 순서로 락 잡을지
- 다른 예/적금 처리 로직과 락 순서가 충돌하지 않는지
- 상태 변경과 송금 처리를 어떻게 직렬화할지
```

현재 구현은 이 복잡도를 줄이기 위해 예/적금 금액을 별도 Account로 분리한다.

```text
입출금계좌 돈: DEPOSIT Account
예금 돈: SAVING_DEPOSIT Account
적금 돈: SAVING_INSTALLMENT Account
```

그리고 Transfer는 Account 단위로만 일반 송금/입금 가능 여부를 판단한다.

## 요약

```text
1. 예/적금 가입 시 전용 Account를 생성한다.
2. 예/적금 전용 Account는 accountType으로 구분한다.
3. 일반 송금/입금은 DEPOSIT Account만 허용한다.
4. Transfer는 Account row를 비관적 락으로 잡고 accountType을 검사한다.
5. Deposit/Installment row는 일반 송금 판단 기준이 아니므로 Transfer에서 락 잡지 않는다.
```
