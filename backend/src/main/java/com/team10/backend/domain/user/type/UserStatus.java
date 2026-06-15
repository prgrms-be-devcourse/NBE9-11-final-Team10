package com.team10.backend.domain.user.type;

/**
 * 사용자 계정 상태.
 *
 * <ul>
 *   <li>{@link #ACTIVE}   — 정상 활성 계정</li>
 *   <li>{@link #DORMANT}  — 장기 미접속(휴면) 계정 (로그인 불가)</li>
 *   <li>{@link #WITHDRAWN} — 탈퇴 처리된 계정 (로그인 불가)</li>
 * </ul>
 */
public enum UserStatus {
    ACTIVE,
    DORMANT,
    WITHDRAWN
}
