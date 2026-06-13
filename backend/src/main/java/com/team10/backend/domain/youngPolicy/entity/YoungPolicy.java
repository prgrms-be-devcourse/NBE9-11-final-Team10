package com.team10.backend.domain.youngPolicy.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YoungPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 번호를 1부터 자동으로 1씩 증가시킵니다.
    private Long id;
}
