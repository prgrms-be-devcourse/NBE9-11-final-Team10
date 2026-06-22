package com.team10.backend.domain.transfer.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.account.entity.Account;
import com.team10.backend.domain.account.repository.AccountRepository;
import com.team10.backend.domain.account.type.AccountType;
import com.team10.backend.domain.transaction.entity.TransactionHistory;
import com.team10.backend.domain.transaction.repository.TransactionHistoryRepository;
import com.team10.backend.domain.transaction.type.TransactionDirection;
import com.team10.backend.domain.transaction.type.TransactionType;
import com.team10.backend.domain.transfer.dto.req.TransferReq;
import com.team10.backend.domain.transfer.entity.Transfer;
import com.team10.backend.domain.transfer.repository.TransferRepository;
import com.team10.backend.domain.transfer.type.TransferStatus;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.idempotency.entity.Idempotency;
import com.team10.backend.global.idempotency.repository.IdempotencyRepository;
import com.team10.backend.global.idempotency.type.IdempotencyStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    @DisplayName("정상 송금 E2E - API 응답, 계좌 잔액, 송금 기록, 양방향 거래내역, 멱등성 성공 상태를 검증한다")
    void transfer_success_persistsConsistentState() throws Exception {
        // given 1. 실제 사용자와 계좌를 DB에 준비한다.
        // - 송금자 계좌: 100,000원
        // - 수취자 계좌: 10,000원
        User sender = saveUser("sender@example.com", "송금자");
        User receiver = saveUser("receiver@example.com", "수취자");
        Account senderAccount = saveAccount(sender, "100200300001", 100_000L);
        Account receiverAccount = saveAccount(receiver, "100200300002", 10_000L);
        String idempotencyKey = "flow-success-key";

        TransferReq request = new TransferReq(
                senderAccount.getId(),
                receiverAccount.getAccountNumber(),
                50_000L,
                "점심값"
        );

        // when. 인증된 송금자 관점에서 실제 송금 API를 호출한다.
        // 컨트롤러 -> 서비스 -> 멱등성 AOP -> 트랜잭션 -> DB 저장 흐름을 모두 통과한다.
        mockMvc.perform(post("/api/v1/transfers")
                        .with(authentication(authenticatedUser(sender.getId())))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then 1. HTTP 응답은 성공 송금 결과를 반환해야 한다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.senderAccountId").value(senderAccount.getId()))
                .andExpect(jsonPath("$.senderAccountNumber").value(senderAccount.getAccountNumber()))
                .andExpect(jsonPath("$.receiverAccountNumber").value(receiverAccount.getAccountNumber()))
                .andExpect(jsonPath("$.amount").value(50_000L))
                .andExpect(jsonPath("$.senderBalanceAfter").value(50_000L))
                .andExpect(jsonPath("$.memo").value("점심값"))
                .andExpect(jsonPath("$.transferredAt").exists());

        entityManager.clear();

        // then 2. 계좌 잔액은 원자적으로 반영되어야 한다.
        Account savedSenderAccount = accountRepository.findById(senderAccount.getId()).orElseThrow();
        Account savedReceiverAccount = accountRepository.findById(receiverAccount.getId()).orElseThrow();

        assertEquals(50_000L, savedSenderAccount.getBalance());
        assertEquals(60_000L, savedReceiverAccount.getBalance());

        // then 3. 송금 원장에는 SUCCESS 송금 1건만 저장되어야 한다.
        List<Transfer> transfers = transferRepository.findAll();
        assertEquals(1, transfers.size());

        Transfer transfer = transfers.getFirst();
        assertEquals(TransferStatus.SUCCESS, transfer.getStatus());
        assertEquals(savedSenderAccount.getId(), transfer.getSenderAccount().getId());
        assertEquals(savedReceiverAccount.getId(), transfer.getReceiverAccount().getId());
        assertEquals(50_000L, transfer.getAmount());
        assertEquals("점심값", transfer.getMemo());

        // then 4. 거래내역은 송금자 OUT 1건, 수취자 IN 1건으로 쌍을 이뤄야 한다.
        List<TransactionHistory> histories = transactionHistoryRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(TransactionHistory::getDirection).reversed())
                .toList();

        assertEquals(2, histories.size());

        TransactionHistory senderHistory = histories.get(0);
        assertEquals(savedSenderAccount.getId(), senderHistory.getAccount().getId());
        assertEquals(transfer.getId(), senderHistory.getTransfer().getId());
        assertEquals(TransactionType.TRANSFER, senderHistory.getType());
        assertEquals(TransactionDirection.OUT, senderHistory.getDirection());
        assertEquals(50_000L, senderHistory.getAmount());
        assertEquals(100_000L, senderHistory.getBalanceBefore());
        assertEquals(50_000L, senderHistory.getBalanceAfter());
        assertEquals(receiverAccount.getAccountNumber(), senderHistory.getCounterpartyAccountNumber());
        assertEquals("수취자", senderHistory.getCounterpartyName());
        assertEquals("점심값", senderHistory.getMemo());
        assertNotNull(senderHistory.getTransactedAt());

        TransactionHistory receiverHistory = histories.get(1);
        assertEquals(savedReceiverAccount.getId(), receiverHistory.getAccount().getId());
        assertEquals(transfer.getId(), receiverHistory.getTransfer().getId());
        assertEquals(TransactionType.TRANSFER, receiverHistory.getType());
        assertEquals(TransactionDirection.IN, receiverHistory.getDirection());
        assertEquals(50_000L, receiverHistory.getAmount());
        assertEquals(10_000L, receiverHistory.getBalanceBefore());
        assertEquals(60_000L, receiverHistory.getBalanceAfter());
        assertEquals(senderAccount.getAccountNumber(), receiverHistory.getCounterpartyAccountNumber());
        assertEquals("송금자", receiverHistory.getCounterpartyName());
        assertEquals("점심값", receiverHistory.getMemo());
        assertNotNull(receiverHistory.getTransactedAt());

        // then 5. 멱등성 레코드는 SUCCESS로 완료되고, 재시도 응답에 쓸 responseBody를 보관해야 한다.
        Idempotency idempotency = idempotencyRepository
                .findByUser_IdAndIdempotencyKey(sender.getId(), idempotencyKey)
                .orElseThrow();

        assertEquals(IdempotencyStatus.SUCCESS, idempotency.getStatus());
        assertNotNull(idempotency.getResponseBody());
        assertNotNull(idempotency.getCompletedAt());
    }

    private UsernamePasswordAuthenticationToken authenticatedUser(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Account saveAccount(User user, String accountNumber, Long balance) {
        Account account = Account.create(user, accountNumber, "테스트 계좌", AccountType.DEPOSIT);
        account.deposit(balance);
        return transactionTemplate.execute(status -> accountRepository.save(account));
    }

    private User saveUser(String email, String name) {
        return transactionTemplate.execute(status -> {
            User user = newUser();
            ReflectionTestUtils.setField(user, "email", email);
            ReflectionTestUtils.setField(user, "password", "password");
            ReflectionTestUtils.setField(user, "name", name);
            ReflectionTestUtils.setField(user, "phoneNumber", "01012345678");
            ReflectionTestUtils.setField(user, "birthDate", LocalDate.of(1990, 1, 1));
            ReflectionTestUtils.setField(user, "identityVerified", true);
            entityManager.persist(user);
            return user;
        });
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

    private void cleanDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM transaction_histories").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM transfers").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM idempotency_keys").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM accounts").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            entityManager.clear();
        });
    }
}
