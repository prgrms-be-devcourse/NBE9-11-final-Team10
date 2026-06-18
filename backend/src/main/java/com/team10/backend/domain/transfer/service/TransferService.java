package com.team10.backend.domain.transfer.service;

import com.team10.backend.domain.account.repository.AccountRepository;
import com.team10.backend.domain.transaction.repository.TransactionHistoryRepository;
import com.team10.backend.domain.transfer.dto.res.TopUpRes;
import com.team10.backend.domain.transfer.dto.res.TransferRes;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.idempotency.entity.Idempotency;
import com.team10.backend.global.idempotency.service.IdempotencyRequestHasher;
import com.team10.backend.global.idempotency.service.IdempotencyReserveResult;
import com.team10.backend.global.idempotency.service.IdempotencyService;
import com.team10.backend.global.idempotency.type.IdempotencyOperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final IdempotencyService idempotencyService;
    private final IdempotencyRequestHasher idempotencyRequestHasher;
    private final TransferBusinessService transferBusinessService;

    public TopUpRes topUp(Long userId, String idempotencyKey, Long accountId, Long amount, String memo) {
        IdempotencyReserveResult<TopUpRes> reserveResult = idempotencyService.reserve(
                userId,
                IdempotencyOperationType.DEPOSIT,
                idempotencyKey,
                idempotencyRequestHasher.generate(accountId, amount, memo),
                TopUpRes.class
        );

        if (reserveResult.replay()) {
            return reserveResult.storedResponse();
        }

        Idempotency idempotency = reserveResult.idempotency();

        try {

            TopUpRes response = transferBusinessService.executeTopUp(userId, accountId, amount, memo);

            idempotencyService.completeSuccess(idempotency.getId(), response);

            return response;
        } catch (BusinessException e) {
            idempotencyService.completeFailure(idempotency.getId());
            throw e;
        }
    }

    // 오케스트레이터 패턴 -> @Transactional 제거
    public TransferRes transfer(Long userId, String idempotencyKey, Long senderAccountId, String receiverAccountNumber, Long amount, String memo) {
        // 송금 시작 전에 멱등성 검증/선점
        IdempotencyReserveResult<TransferRes> reserveResult = idempotencyService.reserve(
                userId,
                IdempotencyOperationType.TRANSFER,
                idempotencyKey,
                idempotencyRequestHasher.generate(senderAccountId, receiverAccountNumber, amount, memo),
                TransferRes.class);

        // 만약 이전에 들어왔던 요청이면 저장된 응답객체 반환하고 종료
        if (reserveResult.replay()) {
            return reserveResult.storedResponse();
        }

        Long idempotencyId = reserveResult.idempotency().getId();

        try{

            TransferRes response = transferBusinessService.executeTransfer(userId,
                    senderAccountId,
                    receiverAccountNumber,
                    amount,
                    memo);

            // Idempotency 객체의 상태를 SUCCESS로 변경 및 JSON 송금 성공 당시의 응답객체 저장
            idempotencyService.completeSuccess(idempotencyId, response);
            return response;
        } catch (BusinessException e) { // 비즈니스 예외가 발생했을 때에만(멱등 검증은 통과한 경우) 송금 멱등성 실패상태 기록
            idempotencyService.completeFailure(idempotencyId);
            throw e;
        }

    }

}
