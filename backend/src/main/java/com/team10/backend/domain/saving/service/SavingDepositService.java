package com.team10.backend.domain.saving.service;

import com.team10.backend.domain.account.entity.Account;
import com.team10.backend.domain.account.exception.AccountErrorCode;
import com.team10.backend.domain.account.repository.AccountRepository;
import com.team10.backend.domain.saving.dto.req.*;
import com.team10.backend.domain.saving.dto.res.*;
import com.team10.backend.domain.saving.entity.Deposit;
import com.team10.backend.domain.saving.entity.Installment;
import com.team10.backend.domain.saving.entity.SavingProduct;
import com.team10.backend.domain.saving.exception.SavingErrorCode;
import com.team10.backend.domain.saving.repository.DepositRepository;
import com.team10.backend.domain.saving.repository.InstallmentRepository;
import com.team10.backend.domain.saving.repository.SavingProductRepository;
import com.team10.backend.domain.saving.type.DepositStatus;
import com.team10.backend.domain.saving.type.InstallmentStatus;
import com.team10.backend.domain.saving.type.SavingProductType;
import com.team10.backend.domain.transaction.entity.TransactionHistory;
import com.team10.backend.domain.transaction.repository.TransactionHistoryRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavingDepositService {
    private static final int MONTHS_IN_YEAR = 12;
    private static final int PERCENT_DIVISOR = 100;
    private static final int EARLY_CANCEL_INTEREST_RATE_DIVISOR = 2;
    private static final String DEPOSIT_CANCEL_REFUND_MEMO = "예금 중도 해지 반환";
    private static final String INSTALLMENT_CANCEL_REFUND_MEMO = "적금 중도 해지 반환";
    private static final String DEPOSIT_MATURITY_PAYOUT_MEMO = "예금 만기 지급";
    private static final String INSTALLMENT_MATURITY_PAYOUT_MEMO = "적금 만기 지급";

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final DepositRepository depositRepository;
    private final SavingProductRepository savingProductRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final InstallmentRepository installmentRepository;

    @Transactional
    public DepositCreateRes createDeposit(Long userId, DepositCreateReq request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AccountErrorCode.USER_NOT_FOUND));

        SavingProduct savingProduct = savingProductRepository
                .findByIdAndTypeAndActiveTrue(request.productId(),
                        SavingProductType.DEPOSIT)
                .orElseThrow(() -> new BusinessException(SavingErrorCode.SAVING_PRODUCT_NOT_FOUND));

        Account withdrawAccount = accountRepository
                .findByIdAndUserId(request.withdrawAccountId(), userId)
                .orElseThrow(() -> new BusinessException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        if (!withdrawAccount.isActive()) {
            throw new BusinessException(AccountErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        if (request.amount() < savingProduct.getMinAmount()) {
            throw new BusinessException(SavingErrorCode.INVALID_DEPOSIT_AMOUNT);
        }

        if (savingProduct.getMaxAmount() != null
                && request.amount() > savingProduct.getMaxAmount()) {
            throw new BusinessException(SavingErrorCode.INVALID_DEPOSIT_AMOUNT);
        }

        LocalDate maturityDate = LocalDate.now()
                .plusMonths(savingProduct.getPeriodMonth());

        withdrawAccount.withdraw(request.amount());

        // 원금 × 연이율 × 가입개월수 ÷ 1년 개월수 ÷ 퍼센트 변환값
        Long expectedInterest = (long) (
                request.amount()
                        * savingProduct.getInterestRate()
                        * savingProduct.getPeriodMonth()
                        / MONTHS_IN_YEAR
                        / PERCENT_DIVISOR
        );

        Deposit deposit = Deposit.create(
                user,
                savingProduct,
                withdrawAccount,
                request.amount(),
                savingProduct.getInterestRate(),
                maturityDate,
                expectedInterest
        );

        Deposit savedDeposit = depositRepository.save(deposit);

        return DepositCreateRes.from(savedDeposit);
    }

    @Transactional
    public InstallmentCreateRes createInstallment(Long userId, InstallmentCreateReq request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AccountErrorCode.USER_NOT_FOUND));

        SavingProduct savingProduct = savingProductRepository
                .findByIdAndTypeAndActiveTrue(request.productId(),
                        SavingProductType.INSTALLMENT)
                .orElseThrow(() -> new BusinessException(SavingErrorCode.SAVING_PRODUCT_NOT_FOUND));

        Account withdrawAccount = accountRepository
                .findByIdAndUserId(request.withdrawAccountId(), userId)
                .orElseThrow(() -> new BusinessException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        if (!withdrawAccount.isActive()) {
            throw new BusinessException(AccountErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        if (request.monthlyAmount() < savingProduct.getMinAmount()) {
            throw new BusinessException(SavingErrorCode.INVALID_INSTALLMENT_AMOUNT);
        }

        if (savingProduct.getMonthlyLimit() != null
                && request.monthlyAmount() > savingProduct.getMonthlyLimit()) {
            throw new BusinessException(SavingErrorCode.INVALID_INSTALLMENT_AMOUNT);
        }

        Long expectedTargetAmount =
                request.monthlyAmount() * savingProduct.getPeriodMonth();

        if (!request.targetAmount().equals(expectedTargetAmount)) {
            throw new BusinessException(SavingErrorCode.INVALID_TARGET_AMOUNT);
        }

        LocalDate maturityDate = LocalDate.now()
                .plusMonths(savingProduct.getPeriodMonth());

        // 적금 가입할 때 출금 계좌에서 1회차 월 납입액을 빼는 코드
        withdrawAccount.withdraw(request.monthlyAmount());

        Installment installment = Installment.create(
                user,
                savingProduct,
                withdrawAccount,
                request.monthlyAmount(),
                request.targetAmount(),
                savingProduct.getInterestRate(),
                maturityDate,
                request.autoTransferYn()
        );

        Installment savedInstallment = installmentRepository.save(installment);

        return InstallmentCreateRes.from(savedInstallment);
    }

    public List<DepositSummaryRes> getDeposits(Long userId, DepositStatus status) {
        List<Deposit> deposits;

        if (status == null) {
            deposits = depositRepository.findAllByUserIdWithProduct(userId);
        } else {
            deposits = depositRepository.findAllByUserIdAndStatusWithProduct(userId, status);
        }

        return deposits.stream()
                .map(DepositSummaryRes::from)
                .toList();
    }

    public DepositDetailRes getDeposit(Long userId, Long depositId) {
        Deposit deposit =
                depositRepository.findByIdAndUserIdWithProduct(depositId, userId)
                        .orElseThrow(() -> new
                                BusinessException(SavingErrorCode.DEPOSIT_NOT_FOUND));

        return DepositDetailRes.from(deposit);
    }

    public List<InstallmentSummaryRes> getInstallments(Long userId, InstallmentStatus status) {
        List<Installment> installments;

        if (status == null) {
            installments =
                    installmentRepository.findAllByUserIdWithProduct(userId);
        } else {
            installments =
                    installmentRepository.findAllByUserIdAndStatusWithProduct(userId, status);
        }

        return installments.stream()
                .map(InstallmentSummaryRes::from)
                .toList();
    }

    public InstallmentDetailRes getInstallment(Long userId, Long installmentId) {
        Installment installment =
                installmentRepository.findByIdAndUserIdWithProduct(installmentId, userId)
                        .orElseThrow(() -> new
                                BusinessException(SavingErrorCode.INSTALLMENT_NOT_FOUND));

        return InstallmentDetailRes.from(installment);
    }

    public InterestPreviewRes getInterestPreview(
            Long userId,
            Long savingId,
            SavingProductType savingType
    ) {
        if (savingType == SavingProductType.DEPOSIT) {
            // 예금 조회
            Deposit deposit = depositRepository.findByIdAndUserIdWithProduct(savingId, userId)
                    .orElseThrow(() -> new
                            BusinessException(SavingErrorCode.DEPOSIT_NOT_FOUND));
            // 총 수령액 계산
            Long expectedTotalAmount = deposit.getPrincipal() + deposit.getExpectedInterest();

            return InterestPreviewRes.fromDeposit(deposit, expectedTotalAmount);
        }

        if (savingType == SavingProductType.INSTALLMENT) {
            // 적금 조회
            Installment installment = installmentRepository.findByIdAndUserIdWithProduct(savingId, userId)
                    .orElseThrow(() -> new
                            BusinessException(SavingErrorCode.INSTALLMENT_NOT_FOUND));

            int periodMonth = installment.getSavingProduct().getPeriodMonth(); // 적금 가입기간(개월)

            Long expectedInterest = calculateInstallmentExpectedInterest(installment);

            Long expectedTotalAmount =
                    installment.getTargetAmount() + expectedInterest; // 만기 예상 수령액 = 목표 금액 + 예상 이자


            return InterestPreviewRes.fromInstallment(installment, expectedInterest, expectedTotalAmount);
        }

        // 이상한 타입이면 예외
        throw new BusinessException(SavingErrorCode.INVALID_SAVING_TYPE);
    }

    @Transactional
    public WithdrawalLockRes updateWithdrawalLock(
            Long userId,
            Long savingId,
            WithdrawalLockReq request
    ) {
        if (!request.lockYn() && (request.reason() == null ||
                request.reason().isBlank())) {
            throw new
                    BusinessException(SavingErrorCode.WITHDRAWAL_UNLOCK_REASON_REQUIRED);
        }

        if (request.savingType() == SavingProductType.DEPOSIT) {
            Deposit deposit = depositRepository.findByIdAndUserIdWithProduct(savingId,
                            userId)
                    .orElseThrow(() -> new
                            BusinessException(SavingErrorCode.DEPOSIT_NOT_FOUND));

            deposit.updateWithdrawalLock(request.lockYn(), request.reason());

            return WithdrawalLockRes.fromDeposit(deposit);
        }

        if (request.savingType() == SavingProductType.INSTALLMENT) {
            Installment installment =
                    installmentRepository.findByIdAndUserIdWithProduct(savingId, userId)
                            .orElseThrow(() -> new
                                    BusinessException(SavingErrorCode.INSTALLMENT_NOT_FOUND));

            installment.updateWithdrawalLock(request.lockYn(), request.reason());

            return WithdrawalLockRes.fromInstallment(installment);
        }

        throw new BusinessException(SavingErrorCode.INVALID_SAVING_TYPE);
    }


    @Transactional
    public EarlyCancelRes cancelSaving(
            Long userId,
            Long savingId,
            EarlyCancelReq request
    ) {
        if (request.savingType() == SavingProductType.DEPOSIT) {
            Deposit deposit =
                    depositRepository.findByIdAndUserIdWithProduct(savingId, userId)
                            .orElseThrow(() -> new
                                    BusinessException(SavingErrorCode.DEPOSIT_NOT_FOUND));

            if (deposit.getStatus() != DepositStatus.ACTIVE) {
                throw new
                        BusinessException(SavingErrorCode.SAVING_CANCEL_NOT_ALLOWED);
            }

            Long interestAmount =
                    deposit.getExpectedInterest() /
                            EARLY_CANCEL_INTEREST_RATE_DIVISOR;

            Long refundAmount =
                    deposit.getPrincipal() + interestAmount;

            Account withdrawAccount = deposit.getWithdrawAccount(); // 반환금을 받을 연결 계좌
            Long balanceBefore = withdrawAccount.getBalance(); // 입금 전 계좌 잔액

            withdrawAccount.deposit(refundAmount); // 중도 해지 반환금 입금

            Long balanceAfter = withdrawAccount.getBalance(); // 입금 후 계좌 잔액

            TransactionHistory transactionHistory =
                    TransactionHistory.createSavingCancelRefund(
                            withdrawAccount,
                            refundAmount,
                            balanceBefore,
                            balanceAfter,
                            DEPOSIT_CANCEL_REFUND_MEMO,
                            LocalDateTime.now()
                    );

            transactionHistoryRepository.save(transactionHistory);

            deposit.cancel();

            return EarlyCancelRes.fromDeposit(deposit, interestAmount, refundAmount);
        }

        if (request.savingType() == SavingProductType.INSTALLMENT) {
            Installment installment =
                    installmentRepository.findByIdAndUserIdWithProduct(savingId,
                                    userId)
                            .orElseThrow(() -> new
                                    BusinessException(SavingErrorCode.INSTALLMENT_NOT_FOUND)
                            );

            if (installment.getStatus() != InstallmentStatus.ACTIVE) {
                throw new
                        BusinessException(SavingErrorCode.SAVING_CANCEL_NOT_ALLOWED);
            }

            Long expectedInterest =
                    calculateInstallmentExpectedInterest(installment);

            Long interestAmount =
                    expectedInterest / EARLY_CANCEL_INTEREST_RATE_DIVISOR;

            Long refundAmount =
                    installment.getPaidAmount() + interestAmount;

            Account withdrawAccount = installment.getWithdrawAccount(); // 반환금을 받을 연결 계좌
            Long balanceBefore = withdrawAccount.getBalance(); // 입금 전 계좌 잔액

            withdrawAccount.deposit(refundAmount); // 중도 해지 반환금 입금

            Long balanceAfter = withdrawAccount.getBalance(); // 입금 후 계좌 잔액

            TransactionHistory transactionHistory =
                    TransactionHistory.createSavingCancelRefund(
                            withdrawAccount,
                            refundAmount,
                            balanceBefore,
                            balanceAfter,
                            INSTALLMENT_CANCEL_REFUND_MEMO,
                            LocalDateTime.now()
                    );

            transactionHistoryRepository.save(transactionHistory);

            installment.cancel();

            return EarlyCancelRes.fromInstallment(installment, interestAmount, refundAmount);
        }

        throw new BusinessException(SavingErrorCode.INVALID_SAVING_TYPE);
    }

    private Long calculateInstallmentExpectedInterest(Installment installment) {
        int periodMonth =
                installment.getSavingProduct().getPeriodMonth(); // 적금 가입 기간(개월)

        return (long) (
                installment.getMonthlyAmount()      // 매월 납입하는 금액
                        * installment.getInterestRate() // 연 이율(%). 예: 3.0
                        / PERCENT_DIVISOR          // 3.0%를 0.03으로 바꾸기 위해 100으로 나눔
                        / MONTHS_IN_YEAR           // 연 이율을 월 이율로 바꾸기 위해 12로 나눔
                        * periodMonth              // 가입 기간 개월 수. 예: 12개월
                        * (periodMonth + 1)        // 12 + 11 + ... + 1 계산을 위한 값
                        / 2                        // n * (n + 1) / 2 공식으로 이자 적용 개월 수 합계 계산
        );
    }

    @Transactional
    public MaturityRes matureSaving(
            Long userId,
            Long savingId,
            MaturityReq request
    ) {
        if (request.savingType() == SavingProductType.DEPOSIT) {
            Deposit deposit =
                    depositRepository.findByIdAndUserIdWithProduct(savingId, userId)
                            .orElseThrow(() -> new
                                    BusinessException(SavingErrorCode.DEPOSIT_NOT_FOUND));

            if (deposit.getStatus() != DepositStatus.ACTIVE) {
                throw new BusinessException(SavingErrorCode.SAVING_MATURITY_NOT_ALLOWED);
            }

            if (deposit.getMaturityDate().isAfter(LocalDate.now())) {
                throw new BusinessException(SavingErrorCode.SAVING_NOT_MATURED_YET);
            }

            Long interestAmount = deposit.getExpectedInterest();

            Long payoutAmount =
                    deposit.getPrincipal() + interestAmount;

            Account withdrawAccount = deposit.getWithdrawAccount();
            Long balanceBefore = withdrawAccount.getBalance();

            withdrawAccount.deposit(payoutAmount);

            Long balanceAfter = withdrawAccount.getBalance();

            TransactionHistory transactionHistory =
                    TransactionHistory.createSavingMaturityPayout(
                            withdrawAccount,
                            payoutAmount,
                            balanceBefore,
                            balanceAfter,
                            DEPOSIT_MATURITY_PAYOUT_MEMO,
                            LocalDateTime.now()
                    );

            transactionHistoryRepository.save(transactionHistory);

            deposit.mature();

            return MaturityRes.fromDeposit(deposit, interestAmount, payoutAmount);
        }

        if (request.savingType() == SavingProductType.INSTALLMENT) {
            Installment installment =
                    installmentRepository.findByIdAndUserIdWithProduct(savingId, userId)
                            .orElseThrow(() -> new
                                    BusinessException(SavingErrorCode.INSTALLMENT_NOT_FOUND));

            if (installment.getStatus() != InstallmentStatus.ACTIVE) {
                throw new BusinessException(SavingErrorCode.SAVING_MATURITY_NOT_ALLOWED);
            }

            if (installment.getMaturityDate().isAfter(LocalDate.now())) {
                throw new BusinessException(SavingErrorCode.SAVING_NOT_MATURED_YET);
            }

            Long interestAmount =
                    calculateInstallmentExpectedInterest(installment);

            Long payoutAmount =
                    installment.getPaidAmount() + interestAmount;

            Account withdrawAccount = installment.getWithdrawAccount();
            Long balanceBefore = withdrawAccount.getBalance();

            withdrawAccount.deposit(payoutAmount);

            Long balanceAfter = withdrawAccount.getBalance();

            TransactionHistory transactionHistory =
                    TransactionHistory.createSavingMaturityPayout(
                            withdrawAccount,
                            payoutAmount,
                            balanceBefore,
                            balanceAfter,
                            INSTALLMENT_MATURITY_PAYOUT_MEMO,
                            LocalDateTime.now()
                    );

            transactionHistoryRepository.save(transactionHistory);

            installment.mature();

            return MaturityRes.fromInstallment(installment, interestAmount,
                    payoutAmount);
        }

        throw new BusinessException(SavingErrorCode.INVALID_SAVING_TYPE);
    }
}

