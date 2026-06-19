package com.team10.backend.domain.saving.service;

import com.team10.backend.domain.account.entity.Account;
import com.team10.backend.domain.account.exception.AccountErrorCode;
import com.team10.backend.domain.account.repository.AccountRepository;
import com.team10.backend.domain.saving.dto.req.DepositCreateReq;
import com.team10.backend.domain.saving.dto.req.InstallmentCreateReq;
import com.team10.backend.domain.saving.dto.req.WithdrawalLockReq;
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
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavingDepositService {
    private static final int MONTHS_IN_YEAR = 12;
    private static final int PERCENT_DIVISOR = 100;

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

            // 적금은 매달 돈을 넣는다.
            // 먼저 넣은 돈은 이자를 오래 받고, 나중에 넣은 돈은 이자를 짧게 받는다.
            // 그래서 각 납입금의 이자 적용 개월 수 합계를 사용한다.
            Long expectedInterest = (long) (
                    installment.getMonthlyAmount()      // 매월 납입 금액
                            * installment.getInterestRate() // 연 이율(%)
                            / PERCENT_DIVISOR          // 퍼센트 값을 소수로 변환
                            / MONTHS_IN_YEAR           // 연 이율을 월 이율로 변환
                            * periodMonth              // 가입 기간 개월 수
                            * (periodMonth + 1)        // 1부터 가입 기간까지의 합 계산용
                            / 2                        // 등차수열 합 공식: n * (n + 1) / 2
            );

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
}
