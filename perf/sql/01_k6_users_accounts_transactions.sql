-- k6 은행/계좌 조회 테스트용 기본 데이터입니다.
-- 테스트 사용자 2명, 입출금 계좌 4개, 계좌별 거래내역 20,000건을 생성합니다.
-- 로그인 비밀번호 평문: Password1!
-- 계좌 비밀번호 평문: 123456

SET @k6_user_password_hash = '$2y$10$MT4iEl8KDbq/n4iQB8uUYeGHAXlrn9wUH1metPptpZgxg8ORo4X.O';
SET @k6_account_password_hash = '$2y$10$JLOUv1hc6oGsQYLvePh8FOBuoWZRgbRnUgdwdY3pl81J9TmopxBTO';

INSERT INTO users
(id, birth_date, identity_verified, identity_verified_at, created_at, updated_at, phone_number, name, email, password, status)
VALUES
(900001, '1999-01-01', 1, NOW(6), NOW(6), NOW(6), '01090000001', 'k6 테스트 사용자 1', 'k6-user1@0bank.test', @k6_user_password_hash, 'ACTIVE'),
(900002, '1998-02-02', 1, NOW(6), NOW(6), NOW(6), '01090000002', 'k6 테스트 사용자 2', 'k6-user2@0bank.test', @k6_user_password_hash, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  birth_date = VALUES(birth_date),
  identity_verified = VALUES(identity_verified),
  identity_verified_at = VALUES(identity_verified_at),
  updated_at = NOW(6),
  phone_number = VALUES(phone_number),
  name = VALUES(name),
  password = VALUES(password),
  status = VALUES(status);

INSERT INTO identity_verifications
(id, created_at, updated_at, user_id, ocr_issue_date, ocr_resident_number, ocr_resident_number_hash, ocr_name, failure_reason, status)
VALUES
(900001, NOW(6), NOW(6), 900001, '20200101', NULL, 'k6_identity_hash_900001', 'k6 테스트 사용자 1', NULL, 'COMPLETED'),
(900002, NOW(6), NOW(6), 900002, '20200101', NULL, 'k6_identity_hash_900002', 'k6 테스트 사용자 2', NULL, 'COMPLETED')
ON DUPLICATE KEY UPDATE
  updated_at = NOW(6),
  status = VALUES(status);

INSERT INTO user_profiles
(id, created_at, updated_at, user_id, region, age_group, occupation_status)
VALUES
(900001, NOW(6), NOW(6), 900001, 'SEOUL', 'TWENTIES', 'STUDENT'),
(900002, NOW(6), NOW(6), 900002, 'GYEONGGI', 'TWENTIES', 'EMPLOYED')
ON DUPLICATE KEY UPDATE
  updated_at = NOW(6),
  region = VALUES(region),
  age_group = VALUES(age_group),
  occupation_status = VALUES(occupation_status);

INSERT INTO user_consents
(agreed, agreed_at, created_at, updated_at, user_id, terms_type)
VALUES
(1, NOW(6), NOW(6), NOW(6), 900001, 'SERVICE_TERMS'),
(1, NOW(6), NOW(6), NOW(6), 900001, 'PERSONAL_INFO'),
(1, NOW(6), NOW(6), NOW(6), 900001, 'FINANCIAL_INFO'),
(1, NOW(6), NOW(6), NOW(6), 900002, 'SERVICE_TERMS'),
(1, NOW(6), NOW(6), NOW(6), 900002, 'PERSONAL_INFO'),
(1, NOW(6), NOW(6), NOW(6), 900002, 'FINANCIAL_INFO')
ON DUPLICATE KEY UPDATE
  agreed = VALUES(agreed),
  agreed_at = VALUES(agreed_at),
  updated_at = NOW(6);

INSERT INTO accounts
(id, balance, created_at, updated_at, user_id, account_number, nickname, account_type, account_password_hash, status)
VALUES
(900001, 1000000000, NOW(6), NOW(6), 900001, '900000000001', 'k6 user1 main', 'DEPOSIT', @k6_account_password_hash, 'ACTIVE'),
(900002, 1000000000, NOW(6), NOW(6), 900001, '900000000002', 'k6 user1 sub', 'DEPOSIT', @k6_account_password_hash, 'ACTIVE'),
(900003, 1000000000, NOW(6), NOW(6), 900002, '900000000003', 'k6 user2 main', 'DEPOSIT', @k6_account_password_hash, 'ACTIVE'),
(900004, 1000000000, NOW(6), NOW(6), 900002, '900000000004', 'k6 user2 sub', 'DEPOSIT', @k6_account_password_hash, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  balance = VALUES(balance),
  updated_at = NOW(6),
  user_id = VALUES(user_id),
  nickname = VALUES(nickname),
  account_type = VALUES(account_type),
  account_password_hash = VALUES(account_password_hash),
  status = VALUES(status);

INSERT INTO transaction_histories
(account_id, amount, balance_after, balance_before, created_at, transacted_at, transfer_id, updated_at,
 counterparty_account_number, counterparty_name, memo, direction, type)
SELECT
  a.id,
  1000 + (seq.n % 100000) AS amount,
  a.balance + seq.n AS balance_after,
  a.balance + seq.n - (1000 + (seq.n % 100000)) AS balance_before,
  DATE_SUB(NOW(6), INTERVAL seq.n MINUTE) AS created_at,
  DATE_SUB(NOW(6), INTERVAL seq.n MINUTE) AS transacted_at,
  NULL AS transfer_id,
  DATE_SUB(NOW(6), INTERVAL seq.n MINUTE) AS updated_at,
  CONCAT('800', LPAD((seq.n % 1000000000), 9, '0')) AS counterparty_account_number,
  CONCAT('k6 거래처 ', seq.n % 200) AS counterparty_name,
  CONCAT('k6 거래내역 ', a.id, '-', seq.n) AS memo,
  CASE WHEN seq.n % 2 = 0 THEN 'IN' ELSE 'OUT' END AS direction,
  CASE
    WHEN seq.n % 5 = 0 THEN 'TRANSFER'
    WHEN seq.n % 5 = 1 THEN 'PAYMENT'
    ELSE 'DEPOSIT'
  END AS type
FROM accounts a
JOIN (
  SELECT ones.i + tens.i * 10 + hundreds.i * 100 + thousands.i * 1000 + ten_thousands.i * 10000 + 1 AS n
  FROM
    (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
    CROSS JOIN (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
    CROSS JOIN (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) hundreds
    CROSS JOIN (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) thousands
    CROSS JOIN (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2) ten_thousands
) seq
WHERE a.id BETWEEN 900001 AND 900004
  AND seq.n <= 20000;
