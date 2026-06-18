package com.team10.backend.domain.transfer.service;

import com.team10.backend.domain.account.entity.Account;
import com.team10.backend.domain.account.repository.AccountRepository;
import com.team10.backend.domain.account.type.AccountType;
import com.team10.backend.domain.transaction.entity.TransactionHistory;
import com.team10.backend.domain.transaction.repository.TransactionHistoryRepository;
import com.team10.backend.domain.transaction.type.TransactionDirection;
import com.team10.backend.domain.transaction.type.TransactionType;
import com.team10.backend.domain.transfer.dto.res.TopUpRes;
import com.team10.backend.domain.transfer.dto.res.TransferRes;
import com.team10.backend.domain.transfer.entity.Transfer;
import com.team10.backend.domain.transfer.exception.TransferErrorCode;
import com.team10.backend.domain.transfer.event.TransferFailedEvent;
import com.team10.backend.domain.transfer.repository.TransferRepository;
import com.team10.backend.domain.transfer.type.TransferStatus;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.idempotency.entity.Idempotency;
import com.team10.backend.global.idempotency.service.IdempotencyRequestHasher;
import com.team10.backend.global.idempotency.service.IdempotencyReserveResult;
import com.team10.backend.global.idempotency.service.IdempotencyService;
import com.team10.backend.global.idempotency.type.IdempotencyOperationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private IdempotencyRequestHasher idempotencyRequestHasher;

    @InjectMocks
    private TransferService transferService;

    @Test
    @DisplayName("입금 성공 시 잔액을 증가시키고 입금 거래내역을 저장한다")
    void deposit_success_increasesBalanceAndSavesDepositHistory() {
        Account account = account(1L, user(), "100200300001", 10_000L);
        Idempotency idempotency = idempotency(9L, user(), IdempotencyOperationType.DEPOSIT, "deposit-key");
        when(idempotencyRequestHasher.generate(1L, 5_000L, "입금 메모")).thenReturn("deposit-request-hash");
        when(idempotencyService.reserve(1L, IdempotencyOperationType.DEPOSIT, "deposit-key", "deposit-request-hash", TopUpRes.class))
                .thenReturn(IdempotencyReserveResult.reserved(idempotency));
        when(accountRepository.findByIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(account));
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> {
                    TransactionHistory history = invocation.getArgument(0);
                    ReflectionTestUtils.setField(history, "id", 100L);
                    return history;
                });

        TopUpRes response = transferService.topUp(1L, "deposit-key", 1L, 5_000L, "입금 메모");

        assertEquals(15_000L, account.getBalance());
        assertEquals(100L, response.transactionId());
        assertEquals(1L, response.accountId());
        assertEquals(TransactionType.DEPOSIT, response.type());
        assertEquals(5_000L, response.amount());
        assertEquals(10_000L, response.balanceBefore());
        assertEquals(15_000L, response.balanceAfter());
        assertEquals("입금 메모", response.memo());
        assertNotNull(response.transactedAt());

        ArgumentCaptor<TransactionHistory> historyCaptor = ArgumentCaptor.forClass(TransactionHistory.class);
        verify(transactionHistoryRepository).save(historyCaptor.capture());
        verify(idempotencyService).completeSuccess(9L, response);

        TransactionHistory savedHistory = historyCaptor.getValue();
        assertSame(account, savedHistory.getAccount());
        assertEquals(TransactionType.DEPOSIT, savedHistory.getType());
        assertEquals(TransactionDirection.IN, savedHistory.getDirection());
        assertEquals(5_000L, savedHistory.getAmount());
        assertEquals(10_000L, savedHistory.getBalanceBefore());
        assertEquals(15_000L, savedHistory.getBalanceAfter());
        assertEquals("입금 메모", savedHistory.getMemo());
    }

    @Test
    @DisplayName("입금 금액이 유효하지 않으면 INVALID_INPUT_VALUE 예외를 발생시킨다")
    void deposit_invalidAmount_throwsInvalidInputValue() {
        Idempotency idempotency = idempotency(10L, user(), IdempotencyOperationType.DEPOSIT, "invalid-deposit-key");
        when(idempotencyRequestHasher.generate(1L, 0L, "입금 메모")).thenReturn("invalid-deposit-request-hash");
        when(idempotencyService.reserve(1L, IdempotencyOperationType.DEPOSIT, "invalid-deposit-key", "invalid-deposit-request-hash", TopUpRes.class))
                .thenReturn(IdempotencyReserveResult.reserved(idempotency));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferService.topUp(1L, "invalid-deposit-key", 1L, 0L, "입금 메모")
        );

        assertEquals(TransferErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(accountRepository, never()).findByIdAndUserIdForUpdate(any(), any());
        verify(transactionHistoryRepository, never()).save(any());
        verify(idempotencyService).completeFailure(10L);
    }

    @Test
    @DisplayName("입금 대상 계좌를 찾을 수 없으면 ACCOUNT_NOT_FOUND 예외를 발생시킨다")
    void deposit_accountNotFound_throwsAccountNotFound() {
        Idempotency idempotency = idempotency(11L, user(), IdempotencyOperationType.DEPOSIT, "missing-account-key");
        when(idempotencyRequestHasher.generate(1L, 5_000L, "입금 메모")).thenReturn("missing-account-request-hash");
        when(idempotencyService.reserve(1L, IdempotencyOperationType.DEPOSIT, "missing-account-key", "missing-account-request-hash", TopUpRes.class))
                .thenReturn(IdempotencyReserveResult.reserved(idempotency));
        when(accountRepository.findByIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferService.topUp(1L, "missing-account-key", 1L, 5_000L, "입금 메모")
        );

        assertEquals(TransferErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        verify(transactionHistoryRepository, never()).save(any());
        verify(idempotencyService).completeFailure(11L);
    }

    @Test
    @DisplayName("송금 성공 시 양쪽 계좌 잔액을 변경하고 송금 거래내역 두 건을 저장한다")
    void transfer_success_changesBalancesAndSavesTwoHistories() {
        User sender = user();
        User receiver = user();
        when(sender.getId()).thenReturn(1L);
        when(sender.getName()).thenReturn("송금자");
        when(receiver.getName()).thenReturn("수취자");

        Account senderAccount = account(1L, sender, "100200300001", 100_000L);
        Account receiverAccount = account(2L, receiver, "100200300002", 10_000L);
        Idempotency idempotency = idempotency(12L, sender, IdempotencyOperationType.TRANSFER, "transfer-key");
        when(idempotencyRequestHasher.generate(1L, "100200300002", 50_000L, "점심값")).thenReturn("transfer-request-hash");
        when(idempotencyService.reserve(1L, IdempotencyOperationType.TRANSFER, "transfer-key", "transfer-request-hash", TransferRes.class))
                .thenReturn(IdempotencyReserveResult.reserved(idempotency));
        when(accountRepository.findIdByAccountNumber("100200300002")).thenReturn(Optional.of(2L));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiverAccount));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionHistoryRepository.save(any(TransactionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferRes response = transferService.transfer(1L, "transfer-key", 1L, "100200300002", 50_000L, "점심값");

        assertEquals(50_000L, senderAccount.getBalance());
        assertEquals(60_000L, receiverAccount.getBalance());
        assertEquals(TransferStatus.SUCCESS, response.status());
        assertEquals(1L, response.senderAccountId());
        assertEquals("100200300001", response.senderAccountNumber());
        assertEquals("100200300002", response.receiverAccountNumber());
        assertEquals(50_000L, response.amount());
        assertEquals(50_000L, response.senderBalanceAfter());
        assertEquals("점심값", response.memo());
        assertNotNull(response.transferredAt());

        ArgumentCaptor<TransactionHistory> historyCaptor = ArgumentCaptor.forClass(TransactionHistory.class);
        verify(transactionHistoryRepository, times(2)).save(historyCaptor.capture());
        verify(transferRepository).save(any(Transfer.class));
        verify(idempotencyService).completeSuccess(12L, response);

        List<TransactionHistory> histories = historyCaptor.getAllValues();
        TransactionHistory senderHistory = histories.get(0);
        TransactionHistory receiverHistory = histories.get(1);

        assertSame(senderAccount, senderHistory.getAccount());
        assertEquals(TransactionType.TRANSFER, senderHistory.getType());
        assertEquals(TransactionDirection.OUT, senderHistory.getDirection());
        assertEquals(100_000L, senderHistory.getBalanceBefore());
        assertEquals(50_000L, senderHistory.getBalanceAfter());
        assertEquals("100200300002", senderHistory.getCounterpartyAccountNumber());
        assertEquals("수취자", senderHistory.getCounterpartyName());

        assertSame(receiverAccount, receiverHistory.getAccount());
        assertEquals(TransactionType.TRANSFER, receiverHistory.getType());
        assertEquals(TransactionDirection.IN, receiverHistory.getDirection());
        assertEquals(10_000L, receiverHistory.getBalanceBefore());
        assertEquals(60_000L, receiverHistory.getBalanceAfter());
        assertEquals("100200300001", receiverHistory.getCounterpartyAccountNumber());
        assertEquals("송금자", receiverHistory.getCounterpartyName());
    }

    @Test
    @DisplayName("같은 계좌로 송금하면 INVALID_INPUT_VALUE 예외를 발생시킨다")
    void transfer_sameAccount_throwsInvalidInputValue() {
        User user = user();
        Account account = account(1L, user, "100200300001", 100_000L);
        Idempotency idempotency = idempotency(13L, user, IdempotencyOperationType.TRANSFER, "same-account-key");
        when(idempotencyRequestHasher.generate(1L, "100200300001", 50_000L, "점심값")).thenReturn("same-account-request-hash");
        when(idempotencyService.reserve(1L, IdempotencyOperationType.TRANSFER, "same-account-key", "same-account-request-hash", TransferRes.class))
                .thenReturn(IdempotencyReserveResult.reserved(idempotency));
        when(accountRepository.findIdByAccountNumber("100200300001")).thenReturn(Optional.of(1L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(1L, "same-account-key", 1L, "100200300001", 50_000L, "점심값")
        );

        assertEquals(TransferErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(transactionHistoryRepository, never()).save(any());
        verify(idempotencyService, never()).completeSuccess(any(), any());
        verify(idempotencyService).completeFailure(13L);
    }

    @Test
    @DisplayName("잔액 부족 송금은 실패 송금 이벤트를 발행하고 INSUFFICIENT_BALANCE 예외를 발생시킨다")
    void transfer_insufficientBalance_publishesFailedEvent() {
        User sender = user();
        User receiver = user();
        when(sender.getId()).thenReturn(1L);

        Account senderAccount = account(1L, sender, "100200300001", 10_000L);
        Account receiverAccount = account(2L, receiver, "100200300002", 10_000L);
        Idempotency idempotency = idempotency(14L, sender, IdempotencyOperationType.TRANSFER, "insufficient-key");
        when(idempotencyRequestHasher.generate(1L, "100200300002", 50_000L, "잔액 부족")).thenReturn("insufficient-request-hash");
        when(idempotencyService.reserve(1L, IdempotencyOperationType.TRANSFER, "insufficient-key", "insufficient-request-hash", TransferRes.class))
                .thenReturn(IdempotencyReserveResult.reserved(idempotency));
        when(accountRepository.findIdByAccountNumber("100200300002")).thenReturn(Optional.of(2L));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiverAccount));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(1L, "insufficient-key", 1L, "100200300002", 50_000L, "잔액 부족")
        );

        ArgumentCaptor<TransferFailedEvent> eventCaptor = ArgumentCaptor.forClass(TransferFailedEvent.class);
        assertEquals(TransferErrorCode.INSUFFICIENT_BALANCE, exception.getErrorCode());
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        verify(transferRepository, never()).save(any(Transfer.class));
        verify(transactionHistoryRepository, never()).save(any());
        verify(idempotencyService, never()).completeSuccess(any(), any());
        verify(idempotencyService).completeFailure(14L);

        TransferFailedEvent event = eventCaptor.getValue();
        assertEquals(1L, event.senderAccountId());
        assertEquals(2L, event.receiverAccountId());
        assertEquals(50_000L, event.amount());
        assertEquals("잔액 부족", event.memo());
    }

    private Account account(Long id, User user, String accountNumber, Long balance) {
        Account account = Account.create(user, accountNumber, "테스트 계좌", AccountType.DEPOSIT);
        account.deposit(balance);
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private Idempotency idempotency(Long id, User user, IdempotencyOperationType operationType, String idempotencyKey) {
        Idempotency idempotency = Idempotency.processing(user, operationType, idempotencyKey, "request-hash");
        ReflectionTestUtils.setField(idempotency, "id", id);
        return idempotency;
    }

    private User user() {
        return mock(User.class);
    }
}
