package com.team10.backend.domain.transfer.domain.event;

public record TransferFailedEvent(
        Long senderAccountId,
        Long receiverAccountId,
        Long amount,
        String memo
) {
}
