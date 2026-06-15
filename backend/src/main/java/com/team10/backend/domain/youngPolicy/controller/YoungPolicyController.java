package com.team10.backend.domain.youngPolicy.controller;

import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyReq;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyDetailRes;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicySummaryRes;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicySyncRes;
import com.team10.backend.domain.youngPolicy.service.YoungPolicyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/youth-policies")
public class YoungPolicyController {

    private final YoungPolicyService youngPolicyService;

    // DB에 저장된 청년 정책 목록 조회
    @GetMapping
    public ResponseEntity<List<YoungPolicySummaryRes>> getPolicies() {
        return ResponseEntity.ok(youngPolicyService.getPolicies());
    }

    // DB에 저장된 청년 정책 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<YoungPolicyDetailRes> getPolicy(@PathVariable Long id) {
        return ResponseEntity.ok(youngPolicyService.getPolicy(id));
    }

    // 외부 청년 정책 API를 호출하여 DB에 저장/갱신
    @PostMapping("/sync")
    public ResponseEntity<YoungPolicySyncRes> syncPolicies(@Valid @RequestBody YoungPolicyReq request) {
        return ResponseEntity.ok(youngPolicyService.syncPolicies(request));
    }
}
