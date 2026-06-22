package com.team10.backend.domain.investment.watchlist.repository;

import com.team10.backend.domain.investment.watchlist.entity.StockWatchlist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockWatchlistRepository extends JpaRepository<StockWatchlist, Long> {

    boolean existsByUserIdAndStockId(Long userId, Long stockId);

    long countByUserId(Long userId);

    @Query("""
            select w
            from StockWatchlist w
            join fetch w.stock
            where w.user.id = :userId
            order by w.createdAt desc
            """)
    List<StockWatchlist> findAllByUserIdWithStock(@Param("userId") Long userId);

    long deleteByUserIdAndStockId(Long userId, Long stockId);
}
