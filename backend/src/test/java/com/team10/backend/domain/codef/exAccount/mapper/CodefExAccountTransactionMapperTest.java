package com.team10.backend.domain.codef.exAccount.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountTransactionSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodefExAccountTransactionMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CodefExAccountTransactionMapper mapper = new CodefExAccountTransactionMapper();

    @Test
    void fallbackTransactionKeyDoesNotExposeRawAccountNumber() throws Exception {
        String accountNumber = "1234567890";
        JsonNode data = objectMapper.readTree("""
                {
                  "resTrHistoryList": [{
                    "resAccountTrDate": "20260618",
                    "resAccountTrTime": "143000",
                    "resAccountOut": "45000",
                    "resAccountDesc1": "스타벅스"
                  }]
                }
                """);

        List<CodefExAccountTransactionSnapshot> snapshots =
                mapper.toSnapshots("0004", accountNumber, data);

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).transactionKey())
                .hasSize(64)
                .doesNotContain(accountNumber)
                .matches("[0-9a-f]{64}");
    }
}
