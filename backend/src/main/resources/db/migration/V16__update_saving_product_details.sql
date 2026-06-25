UPDATE saving_product
SET name = 'KB 국민든든 정기예금',
    interest_rate = 3.7,
    period_month = 12,
    min_amount = 1000000,
    max_amount = 50000000,
    monthly_limit = NULL,
    terms = '개인 고객 대상 12개월 정기예금입니다. 100만원 이상 가입 가능하며 만기 시 원금과 이자를 함께 지급합니다. 중도해지 시 약정 금리보다 낮은 중도해지이율이 적용됩니다.',
    updated_at = NOW(6)
WHERE bank_code = 'KB'
  AND type = 'DEPOSIT';

UPDATE saving_product
SET name = '신한 쏠편한 정기예금',
    interest_rate = 3.95,
    period_month = 24,
    min_amount = 500000,
    max_amount = 100000000,
    monthly_limit = NULL,
    terms = '비대면 가입 가능한 24개월 정기예금입니다. 50만원 이상 가입 가능하고 최대 1억원까지 예치할 수 있습니다. 만기 전 해지 시 중도해지이율이 적용됩니다.',
    updated_at = NOW(6)
WHERE bank_code = 'SHINHAN'
  AND type = 'DEPOSIT';

UPDATE saving_product
SET name = '우리 목표달성 적금',
    interest_rate = 4.2,
    period_month = 12,
    min_amount = 10000,
    max_amount = NULL,
    monthly_limit = 1000000,
    terms = '매월 1만원 이상, 월 최대 100만원까지 납입 가능한 12개월 적금입니다. 자동이체를 통해 꾸준히 납입하고 만기 시 원금과 이자를 지급받습니다.',
    updated_at = NOW(6)
WHERE bank_code = 'WOORI'
  AND type = 'INSTALLMENT';

UPDATE saving_product
SET name = '하나 차곡차곡 적금',
    interest_rate = 4.5,
    period_month = 24,
    min_amount = 10000,
    max_amount = NULL,
    monthly_limit = 500000,
    terms = '매월 1만원 이상, 월 최대 50만원까지 납입 가능한 24개월 적금입니다. 장기 저축 목표에 맞춰 자동이체로 납입하며 만기 전 해지 시 중도해지이율이 적용됩니다.',
    updated_at = NOW(6)
WHERE bank_code = 'HANA'
  AND type = 'INSTALLMENT';
