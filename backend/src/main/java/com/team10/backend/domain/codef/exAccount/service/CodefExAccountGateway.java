package com.team10.backend.domain.codef.exAccount.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.team10.backend.domain.codef.exAccount.client.CodefExAccountClient;
import com.team10.backend.domain.codef.exAccount.crypto.CodefConnectedIdEncryptor;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountConnectionPayload;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountConnectionResult;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountListRequest;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountSnapshot;
import com.team10.backend.domain.codef.exAccount.dto.req.CodefExAccountConnectionCreateReq;
import com.team10.backend.domain.codef.exAccount.mapper.CodefExAccountConnectionPayloadMapper;
import com.team10.backend.domain.codef.exAccount.mapper.CodefExAccountSnapshotMapper;
import com.team10.backend.domain.exAccount.entity.value.EncryptedConnectedId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CodefExAccountGateway {

    private final CodefExAccountConnectionPayloadMapper connectionPayloadMapper;
    private final CodefExAccountClient codefExAccountClient;
    private final CodefConnectedIdEncryptor connectedIdEncryptor;
    private final CodefExAccountSnapshotMapper snapshotMapper;

    public EncryptedConnectedId register(CodefExAccountConnectionCreateReq request) {
        CodefExAccountConnectionPayload payload = connectionPayloadMapper.toPayload(request);
        CodefExAccountConnectionResult result = codefExAccountClient.createConnection(payload);
        return connectedIdEncryptor.encrypt(result.connectedId());
    }

    public List<CodefExAccountSnapshot> getAccountSnapshots(
            String organization,
            EncryptedConnectedId encryptedConnectedId
    ) {
        String connectedId = connectedIdEncryptor.decrypt(encryptedConnectedId);
        CodefExAccountListRequest request = CodefExAccountListRequest.of(
                organization,
                connectedId,
                ""
        );
        JsonNode data = codefExAccountClient.getAccountList(request);
        return snapshotMapper.toSnapshots(organization, data);
    }
}
