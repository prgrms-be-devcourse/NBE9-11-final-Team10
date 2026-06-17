package com.team10.backend.domain.transfer.service;

import com.team10.backend.domain.transfer.dto.res.TransferRes;
import com.team10.backend.domain.transfer.entity.TransferIdempotency;

public record TransferIdempotencyReserveResult(
        boolean replay,
        TransferIdempotency idempotency,
        TransferRes storedResponse
) {
    public static TransferIdempotencyReserveResult reserved(TransferIdempotency idempotency) {
        return new TransferIdempotencyReserveResult(false, idempotency, null);
    }

    public static TransferIdempotencyReserveResult replay(TransferRes storedResponse) {
        return new TransferIdempotencyReserveResult(true, null, storedResponse);
    }
}
