package com.team10.backend.domain.investment.realtime.service;

import com.team10.backend.domain.investment.client.realtime.KisOrderbookWebSocketClient;
import com.team10.backend.domain.investment.realtime.repository.RealtimeOrderbookLeaderLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeOrderbookKisLeaderService {

    private final RealtimeOrderbookLeaderLockRepository leaderLockRepository;
    private final RealtimeOrderbookInstanceIdProvider instanceIdProvider;
    private final RealtimeOrderbookKisSubscriptionCoordinator kisSubscriptionCoordinator;
    private final KisOrderbookWebSocketClient kisOrderbookWebSocketClient;

    public void reconcileStockIfLeader(String stockCode) {
        LeadershipStatus leadershipStatus = ensureLeadership();

        if (leadershipStatus == LeadershipStatus.NOT_LEADER) {
            return;
        }

        if (leadershipStatus == LeadershipStatus.ACQUIRED) {
            kisSubscriptionCoordinator.reconcileAll();
            return;
        }

        // ALREADY_LEADER
        kisSubscriptionCoordinator.reconcileStock(stockCode);
    }

    public void reconcileAllIfLeader() {
        if (ensureLeadership() == LeadershipStatus.NOT_LEADER) {
            return;
        }

        kisSubscriptionCoordinator.reconcileAll();
    }

    private LeadershipStatus ensureLeadership() {
        String instanceId = instanceIdProvider.getInstanceId();

        try {
            if (leaderLockRepository.renew(instanceId)) {
                return LeadershipStatus.ALREADY_LEADER;
            }

            if (leaderLockRepository.tryAcquire(instanceId)) {
                log.info("Realtime orderbook KIS WebSocket leader acquired. instanceId={}", instanceId);
                return LeadershipStatus.ACQUIRED;
            }
        } catch (RuntimeException e) {
            /** leader 여부를 확신할 수 없는 문제 상황 시 오류 발생 방지를 위해 연결 해제 시도 */
            disconnectLocalKisClientIfNecessary();
            throw e;
        }

        disconnectLocalKisClientIfNecessary();
        return LeadershipStatus.NOT_LEADER;
    }

    public void releaseLeadership() {
        String instanceId = instanceIdProvider.getInstanceId();
        if (leaderLockRepository.release(instanceId)) {
            disconnectLocalKisClientIfNecessary();
            log.info("Realtime orderbook KIS WebSocket leader released. instanceId={}", instanceId);
        }
    }

    private void disconnectLocalKisClientIfNecessary() {
        if (kisOrderbookWebSocketClient.isConnected()
                || !kisOrderbookWebSocketClient.subscribedStockCodes().isEmpty()) {
            kisOrderbookWebSocketClient.disconnect();
        }
    }

    private enum LeadershipStatus {
        ALREADY_LEADER,
        ACQUIRED,
        NOT_LEADER
    }
}
