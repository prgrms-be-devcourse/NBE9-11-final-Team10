# KIS WebSocket 접속키 정책

## 개요

KIS WebSocket API 사용 시 AppKey/AppSecret 대신 WebSocket 전용 접속키(Approval Key)를 사용한다.

WebSocket 연결 수립 시 최초 1회 인증 용도로만 사용된다.

---

## 발급 API

### Endpoint

```http
POST /oauth2/Approval
```

### 실전 서버

```text
https://openapi.koreainvestment.com:9443
```

### 모의 서버

```text
https://openapivts.koreainvestment.com:29443
```

---

## 요청

### Header

```http
Content-Type: application/json
```

### Body

```json
{
  "grant_type": "client_credentials",
  "appkey": "{APP_KEY}",
  "secretkey": "{APP_SECRET}"
}
```

---

## 응답

```json
{
  "approval_key": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

---

# Approval Key 관리 정책

## 발급 시점

애플리케이션 시작 시 1회 발급한다.

```text
Application Start
↓
Approval Key 발급
↓
메모리 저장
↓
WebSocket 연결
```

---

## 저장 위치

Approval Key는 DB에 저장하지 않는다.

```text
In-Memory
```

로 관리한다.

---

## 유효 기간

```text
24시간
```

---

## 중요 사항

Approval Key는 WebSocket 연결 수립 시 최초 1회만 사용된다.

따라서

```text
Approval Key 인증
↓
WebSocket 연결 성공
↓
세션 유지
```

상태라면 Approval Key 유효 기간이 만료되어도 연결은 유지된다.

즉,

```text
Approval Key 유효기간 만료
≠
WebSocket 연결 종료
```

이다.

---

## 재발급 정책

다음 상황에서만 재발급한다.

### 1. 애플리케이션 재시작

```text
서버 재기동
↓
Approval Key 재발급
↓
WebSocket 재연결
```

---

### 2. WebSocket 연결 상실

```text
WebSocket Disconnect
↓
Approval Key 재발급
↓
WebSocket 재연결
```

---

## 운영 정책

Approval Key 만료 시간을 기준으로 주기적인 재발급 스케줄링을 수행하지 않는다.

### 이유

Approval Key는 세션 생성 시점에만 사용된다.

이미 연결된 WebSocket 세션에는 영향을 주지 않는다.

따라서

```text
24시간마다 재발급
```

과 같은 스케줄링은 불필요하다.

---

## 구현 예시

```java

@Component
public class KisWebSocketKeyManager {

    private volatile String approvalKey;

    @PostConstruct
    public void initialize() {
        approvalKey = requestApprovalKey();
    }

    public String getApprovalKey() {
        return approvalKey;
    }

    public synchronized void refresh() {
        approvalKey = requestApprovalKey();
    }
}
```

---

## 장애 복구 흐름

```text
WebSocket Disconnect
↓
Approval Key 재발급
↓
WebSocket 재연결
↓
실시간 종목 재구독
↓
정상 서비스 복구
```