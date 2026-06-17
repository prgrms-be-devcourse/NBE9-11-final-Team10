package com.team10.backend.domain.investment.order.type;

public enum InvestmentOrderStatus {
    PENDING, // 주문만 등록 , 아직 미체결 [ 지정가 ]
    PARTIALLY_FILLED, // 지정가 부분 체결
    FILLED, // 거래 완전 체결
    CANCELLED // 거래 취소 [ 시장가 수량 부족, 지정가 예약 취소 .. ]
}
