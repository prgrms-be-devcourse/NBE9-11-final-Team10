package com.team10.backend.domain.youngPolicy.domain.repository;

import com.team10.backend.domain.youngPolicy.application.dto.req.YoungPolicySearchReq;
import com.team10.backend.domain.youngPolicy.domain.entity.YoungPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface YoungPolicyRepositoryCustom {
    Page<YoungPolicy> search(YoungPolicySearchReq filter, Pageable pageable);
}
