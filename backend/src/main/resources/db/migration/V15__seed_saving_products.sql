INSERT INTO saving_product
(active, interest_rate, period_month, min_amount, max_amount, monthly_limit,
 created_at, updated_at, bank_code, bank_name, name, terms, type)
VALUES
(1, 3.5, 12, 100000, 10000000, NULL,
 NOW(6), NOW(6), 'KB', '국민은행', 'KB 정기예금', '만 19세 이상 가입 가능, 12개월 정기예금 상품', 'DEPOSIT'),
(1, 3.8, 24, 100000, 20000000, NULL,
 NOW(6), NOW(6), 'SHINHAN', '신한은행', '신한 안심정기예금', '24개월 예치 가능, 중도해지 시 약정 이율보다 낮은 이율 적용', 'DEPOSIT'),
(1, 4.2, 12, 50000, NULL, 500000,
 NOW(6), NOW(6), 'WOORI', '우리은행', '우리 목표적금', '월 납입 한도 50만원, 12개월 목표 적금 상품', 'INSTALLMENT'),
(1, 4.5, 24, 50000, NULL, 300000,
 NOW(6), NOW(6), 'HANA', '하나은행', '하나 차곡차곡적금', '월 납입 한도 30만원, 24개월 적금 상품', 'INSTALLMENT');
