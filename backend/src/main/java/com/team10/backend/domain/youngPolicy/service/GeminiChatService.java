package com.team10.backend.domain.youngPolicy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiChatService {

    private final RestClient restClient;
    private final String apiKey;

    public GeminiChatService(
            RestClient restClient,
            @Value("${gemini.api-key:}") String apiKey
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public String generateContent(String systemInstruction, String promptText) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is missing. Returning fallback mock response.");
            return generateFallbackResponse();
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

            String fullPrompt = systemInstruction + "\n\n[사용자 요청 내용]\n" + promptText;

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", fullPrompt)
                                    )
                            )
                    )
            );

            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("candidates")) {
                List<?> candidates = (List<?>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                    List<?> parts = (List<?>) content.get("parts");
                    if (!parts.isEmpty()) {
                        Map<?, ?> part = (Map<?, ?>) parts.get(0);
                        return (String) part.get("text");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
        }

        return generateFallbackResponse();
    }

    private String generateFallbackResponse() {
        return "### ⚠️ [안내] AI API 연동 대기 중 (필터링 기반 추천 작동)\n" +
                "현재 AI 추천 엔진(Gemini API) 키가 설정되지 않았거나 호출에 실패하였습니다.\n" +
                "하지만 데이터베이스 필터링 검색을 통해 사용자의 연령 및 지역 조건에 맞는 맞춤형 청년 정책들을 엄선하였습니다. " +
                "추천 목록에서 각 정책의 상세 정보를 확인해 보세요.";
    }
}
