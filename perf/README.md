# 로컬 성능 및 부하 테스트

## 1. 백엔드 실행

MySQL/Redis를 먼저 실행한 뒤, 로컬 secret 파일이 준비된 상태에서 Spring Boot 애플리케이션을 실행합니다.

```bash
cd backend
docker compose up -d mysql redis
./gradlew bootRun
```

애플리케이션이 Prometheus 메트릭을 노출하는지 확인합니다.

```bash
curl http://localhost:8080/actuator/prometheus
```

## 2. Prometheus와 Grafana 실행

Prometheus:

```bash
prometheus --config.file=perf/prometheus/prometheus.local.yml
```

Grafana:

```bash
grafana-server
```

Grafana는 `http://localhost:3000`에서 접속할 수 있습니다. 접속 후 Prometheus를 데이터 소스로 추가합니다.

```text
http://localhost:9090
```

## 3. k6 스모크 테스트 실행

테스트 사용자가 없다면 공개 health 엔드포인트부터 확인합니다.

```bash
k6 run perf/k6/health-smoke.js
```

기존 로컬 테스트 사용자가 있다면 인증이 필요한 사용자 플로우를 실행합니다. 기본 대상 서버는 `http://localhost:8080`입니다.

```bash
TEST_EMAIL=user@example.com \
TEST_PASSWORD=Password1234 \
k6 run perf/k6/banking-smoke.js
```

계좌 ID를 함께 전달하면 거래내역 조회까지 호출합니다.

```bash
TEST_EMAIL=user@example.com \
TEST_PASSWORD=Password1234 \
ACCOUNT_ID=1 \
k6 run perf/k6/banking-smoke.js
```

## 4. k6 부하 테스트 실행

```bash
TEST_EMAIL=user@example.com \
TEST_PASSWORD=Password1234 \
TARGET_VUS=20 \
k6 run perf/k6/banking-load.js
```

처음에는 작은 `TARGET_VUS`로 시작하고, Grafana에서 Spring, JVM, DB, Redis 메트릭을 확인하면서 점진적으로 값을 높입니다.
