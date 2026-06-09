package com.team10.backend.domain.transfer.service;

import com.team10.backend.domain.transfer.dto.req.DepositReq;
import com.team10.backend.domain.transfer.dto.req.TransferReq;
import com.team10.backend.domain.transfer.dto.res.DepositRes;
import com.team10.backend.domain.transfer.dto.res.TransferRes;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class TransferService {

    @Transactional
    public DepositRes deposit(DepositReq request) {
        // TODO: account 도메인 확정 후 계좌 소유자/상태 검증, 잔액 증가, DEPOSIT 거래내역 저장을 구현한다.
        throw new UnsupportedOperationException("Deposit service is not implemented yet.");
    }

    @Transactional
    public TransferRes transfer(TransferReq request) {
        // TODO: account/transaction 도메인 확정 후 출금/수취 계좌 검증, 잔액 변경, 거래내역 2건 저장을 구현한다.
        throw new UnsupportedOperationException("Transfer service is not implemented yet.");
    }
}
