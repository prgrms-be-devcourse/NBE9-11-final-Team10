package com.team10.backend.domain.investment.stock.repository;

import com.team10.backend.domain.investment.stock.entity.Stock;
import com.team10.backend.domain.investment.stock.type.StockMarket;
import com.team10.backend.domain.investment.stock.type.StockStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    @Query("""
            select s
            from Stock s
            where s.market = :market
              and s.status = :status
              and (
                s.stockName like concat('%', :keyword, '%')
                or s.stockCode like concat('%', :keyword, '%')
              )
            order by s.stockName asc
            """)
    List<Stock> search(
            @Param("keyword") String keyword,
            @Param("market") StockMarket market,
            @Param("status") StockStatus status,
            Pageable pageable
    );
}
