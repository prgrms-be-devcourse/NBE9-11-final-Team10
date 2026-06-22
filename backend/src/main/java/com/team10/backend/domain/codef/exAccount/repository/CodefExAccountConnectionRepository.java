package com.team10.backend.domain.codef.exAccount.repository;

import com.team10.backend.domain.codef.exAccount.entity.CodefExAccountConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodefExAccountConnectionRepository
        extends JpaRepository<CodefExAccountConnection, Long> {

    Optional<CodefExAccountConnection> findByUserIdAndOrganization(
            Long userId,
            String organization
    );

    Optional<CodefExAccountConnection> findByIdAndUserId(Long connectionId, Long userId);
}
