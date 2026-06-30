-- birth_year 컬럼 추가 (임시로 NULL 허용)
ALTER TABLE user_profiles ADD COLUMN birth_year INT;

-- 기존 age_group 데이터를 기반으로 birth_year 값 대략적으로 채워넣기
UPDATE user_profiles SET birth_year = 2010 WHERE age_group = 'TEENS';
UPDATE user_profiles SET birth_year = 2000 WHERE age_group = 'TWENTIES';
UPDATE user_profiles SET birth_year = 1990 WHERE age_group = 'THIRTIES';
UPDATE user_profiles SET birth_year = 1980 WHERE age_group = 'FORTIES';
UPDATE user_profiles SET birth_year = 1970 WHERE age_group = 'FIFTIES_PLUS';

-- 혹시 매핑되지 않은 임의의 NULL 값이 있으면 기본값 1990으로 채우기
UPDATE user_profiles SET birth_year = 1990 WHERE birth_year IS NULL;

-- birth_year 컬럼을 NOT NULL로 제약 조건 변경
ALTER TABLE user_profiles MODIFY COLUMN birth_year INT NOT NULL;

-- 기존 age_group 컬럼 제거
ALTER TABLE user_profiles DROP COLUMN age_group;
