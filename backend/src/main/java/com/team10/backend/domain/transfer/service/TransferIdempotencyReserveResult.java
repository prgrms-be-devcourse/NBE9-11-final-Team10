package com.team10.backend.domain.transfer.service;

import com.team10.backend.domain.transfer.dto.res.TransferRes;
import com.team10.backend.domain.transfer.entity.TransferIdempotency;

public record TransferIdempotencyReserveResult(
        boolean replay, // 기존에 처리된 같은 요청인지 여부 (false -> 새 요청, 송금로직 계속 진행|true -> 이미 성공한 요청, 저장된 응답 반환)
        TransferIdempotency idempotency, // 새 요청을 PROCESSING으로 선점했을 때의 멱등성 레코드
        TransferRes storedResponse       // 이미 성공한 요청이 저장된 최초 응답
) {
    // 새 요청을 선점한 경우
    public static TransferIdempotencyReserveResult reserved(TransferIdempotency idempotency) {
        return new TransferIdempotencyReserveResult(false, idempotency, null);
    }
    // 이미 성공한 요청을 재요청한 경우
    public static TransferIdempotencyReserveResult replay(TransferRes storedResponse) {
        return new TransferIdempotencyReserveResult(true, null, storedResponse);
    }
}
