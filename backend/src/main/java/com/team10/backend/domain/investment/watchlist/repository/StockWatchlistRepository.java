package com.team10.backend.domain.investment.watchlist.repository;

import com.team10.backend.domain.investment.watchlist.entity.StockWatchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockWatchlistRepository extends JpaRepository<StockWatchlist, Long> {

}
