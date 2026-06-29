package com.team10.backend.domain.youngPolicy.domain.repository;

import com.team10.backend.domain.youngPolicy.domain.entity.YoungPolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YoungPolicyRepository extends JpaRepository<YoungPolicy, Long>, YoungPolicyRepositoryCustom {
    Optional<YoungPolicy> findByPolicyId(String policyId);
}
