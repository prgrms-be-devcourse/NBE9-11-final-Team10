package com.team10.backend.domain.transfer.repository;

import com.team10.backend.domain.transfer.entity.TransferIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransferIdempotencyRepository extends JpaRepository<TransferIdempotency, Long> {

    // user.id를 명확히 표현하기 위해 메서드명 User_Id로 지정
    Optional<TransferIdempotency> findByUser_IdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Query("""
        select ti
        from TransferIdempotency ti
        where ti.status = com.team10.backend.domain.transfer.type.IdempotencyStatus.PROCESSING
          and ti.createdAt < :threshold
        """)
    // 오래된 PROCESSING 조회
    List<TransferIdempotency> findStaleProcessing(@Param("threshold") LocalDateTime threshold);
}
