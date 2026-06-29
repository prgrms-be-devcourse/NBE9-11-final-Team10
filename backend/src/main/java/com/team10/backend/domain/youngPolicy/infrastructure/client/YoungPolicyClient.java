package com.team10.backend.domain.youngPolicy.infrastructure.client;

import com.team10.backend.domain.youngPolicy.application.dto.req.YoungPolicyReq;
import com.team10.backend.domain.youngPolicy.application.dto.res.YoungPolicyExternalRes;

// 청년정책 외부 API 호출을 담당합니다.
public interface YoungPolicyClient {
    YoungPolicyExternalRes fetchPolicies(YoungPolicyReq request);
}
