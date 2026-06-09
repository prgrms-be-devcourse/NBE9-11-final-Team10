package com.team10.backend.domain.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.team10.backend.domain.account.entity.Account;
import com.team10.backend.domain.account.repository.AccountRepository;
import com.team10.backend.domain.account.type.AccountType;
import com.team10.backend.domain.transaction.entity.TransactionHistory;
import com.team10.backend.domain.transaction.repository.TransactionHistoryRepository;
import com.team10.backend.domain.transaction.type.TransactionDirection;
import com.team10.backend.domain.transaction.type.TransactionType;
import com.team10.backend.domain.transfer.dto.res.DepositRes;
import com.team10.backend.domain.transfer.dto.res.TransferRes;
import com.team10.backend.domain.transfer.errorcode.TransferErrorCode;
import com.team10.backend.domain.transfer.repository.TransferRepository;
import com.team10.backend.domain.transfer.type.TransferStatus;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TransferServiceIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deposit_success_persistsBalanceAndDepositHistory() {
        Account account = saveAccount(saveUser("sender@example.com", "홍길동"), "100200300001", 10_000L);

        DepositRes response = transferService.deposit(account.getId(), 5_000L, "입금 메모");
        flushAndClear();

        Account savedAccount = accountRepository.findById(account.getId()).orElseThrow();
        List<TransactionHistory> histories = transactionHistoryRepository.findAll();

        assertEquals(15_000L, savedAccount.getBalance());
        assertEquals(1, histories.size());

        TransactionHistory history = histories.getFirst();
        assertEquals(response.transactionId(), history.getId());
        assertEquals(savedAccount.getId(), history.getAccount().getId());
        assertEquals(TransactionType.DEPOSIT, history.getType());
        assertEquals(TransactionDirection.IN, history.getDirection());
        assertEquals(5_000L, history.getAmount());
        assertEquals(10_000L, history.getBalanceBefore());
        assertEquals(15_000L, history.getBalanceAfter());
        assertEquals("입금 메모", history.getMemo());
        assertNotNull(history.getTransactedAt());
    }

    @Test
    void transfer_success_persistsBalancesTransferAndTwoHistories() {
        User sender = saveUser("sender@example.com", "송금자");
        User receiver = saveUser("receiver@example.com", "수취자");
        Account senderAccount = saveAccount(sender, "100200300001", 100_000L);
        Account receiverAccount = saveAccount(receiver, "100200300002", 10_000L);

        TransferRes response = transferService.transfer(senderAccount.getId(), "100200300002", 50_000L, "점심값");
        flushAndClear();

        Account savedSenderAccount = accountRepository.findById(senderAccount.getId()).orElseThrow();
        Account savedReceiverAccount = accountRepository.findById(receiverAccount.getId()).orElseThrow();
        List<TransactionHistory> histories = transactionHistoryRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(TransactionHistory::getDirection).reversed())
                .toList();

        assertEquals(50_000L, savedSenderAccount.getBalance());
        assertEquals(60_000L, savedReceiverAccount.getBalance());
        assertEquals(1, transferRepository.count());
        assertEquals(2, histories.size());

        assertNotNull(response.transferId());
        assertEquals(TransferStatus.SUCCESS, response.status());
        assertEquals(senderAccount.getId(), response.senderAccountId());
        assertEquals("100200300001", response.senderAccountNumber());
        assertEquals("100200300002", response.receiverAccountNumber());
        assertEquals(50_000L, response.amount());
        assertEquals(50_000L, response.senderBalanceAfter());
        assertEquals("점심값", response.memo());

        TransactionHistory senderHistory = histories.get(0);
        assertEquals(savedSenderAccount.getId(), senderHistory.getAccount().getId());
        assertEquals(TransactionType.TRANSFER, senderHistory.getType());
        assertEquals(TransactionDirection.OUT, senderHistory.getDirection());
        assertEquals(50_000L, senderHistory.getAmount());
        assertEquals(100_000L, senderHistory.getBalanceBefore());
        assertEquals(50_000L, senderHistory.getBalanceAfter());
        assertEquals("100200300002", senderHistory.getCounterpartyAccountNumber());
        assertEquals("수취자", senderHistory.getCounterpartyName());
        assertEquals(response.transferId(), senderHistory.getTransfer().getId());

        TransactionHistory receiverHistory = histories.get(1);
        assertEquals(savedReceiverAccount.getId(), receiverHistory.getAccount().getId());
        assertEquals(TransactionType.TRANSFER, receiverHistory.getType());
        assertEquals(TransactionDirection.IN, receiverHistory.getDirection());
        assertEquals(50_000L, receiverHistory.getAmount());
        assertEquals(10_000L, receiverHistory.getBalanceBefore());
        assertEquals(60_000L, receiverHistory.getBalanceAfter());
        assertEquals("100200300001", receiverHistory.getCounterpartyAccountNumber());
        assertEquals("송금자", receiverHistory.getCounterpartyName());
        assertEquals(response.transferId(), receiverHistory.getTransfer().getId());
    }

    @Test
    void transfer_invalidAmount_doesNotPersistAnything() {
        Account account = saveAccount(saveUser("sender@example.com", "홍길동"), "100200300001", 10_000L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(account.getId(), "100200300002", 0L, "잘못된 송금")
        );
        flushAndClear();

        Account savedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(TransferErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        assertEquals(10_000L, savedAccount.getBalance());
        assertEquals(0, transferRepository.count());
        assertEquals(0, transactionHistoryRepository.count());
    }

    @Test
    void transfer_insufficientBalance_rollsBackBalanceAndHistory() {
        User sender = saveUser("sender@example.com", "송금자");
        User receiver = saveUser("receiver@example.com", "수취자");
        Account senderAccount = saveAccount(sender, "100200300001", 10_000L);
        Account receiverAccount = saveAccount(receiver, "100200300002", 10_000L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(senderAccount.getId(), "100200300002", 50_000L, "잔액 부족")
        );
        flushAndClear();

        Account savedSenderAccount = accountRepository.findById(senderAccount.getId()).orElseThrow();
        Account savedReceiverAccount = accountRepository.findById(receiverAccount.getId()).orElseThrow();
        assertEquals(TransferErrorCode.TRANSFER_FAILED, exception.getErrorCode());
        assertEquals(10_000L, savedSenderAccount.getBalance());
        assertEquals(10_000L, savedReceiverAccount.getBalance());
        assertEquals(0, transferRepository.count());
        assertEquals(0, transactionHistoryRepository.count());
    }

    private Account saveAccount(User user, String accountNumber, Long balance) {
        Account account = Account.create(user, accountNumber, "테스트 계좌", AccountType.DEPOSIT);
        account.deposit(balance);
        return accountRepository.save(account);
    }

    private User saveUser(String email, String name) {
        User user = newUser();
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "name", name);
        ReflectionTestUtils.setField(user, "phoneNumber", "01012345678");
        ReflectionTestUtils.setField(user, "birthDate", LocalDate.of(1990, 1, 1));
        ReflectionTestUtils.setField(user, "identityVerified", true);
        entityManager.persist(user);
        return user;
    }

    private User newUser() {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("User test fixture creation failed", e);
        }
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
