package com.team10.backend.domain.youngPolicy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;

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
            GeminiRequest requestBody = new GeminiRequest(List.of(new Content(List.of(new Part(fullPrompt)))));

            GeminiResponse response = restClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);

            String result = extractText(response);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
        }

        return generateFallbackResponse();
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            return null;
        }
        return candidate.content().parts().get(0).text();
    }

    private String generateFallbackResponse() {
        return "### ⚠️ [안내] AI API 연동 대기 중 (필터링 기반 추천 작동)\n" +
                "현재 AI 추천 엔진(Gemini API) 키가 설정되지 않았거나 호출에 실패하였습니다.\n" +
                "하지만 데이터베이스 필터링 검색을 통해 사용자의 연령 및 지역 조건에 맞는 맞춤형 청년 정책들을 엄선하였습니다. " +
                "추천 목록에서 각 정책의 상세 정보를 확인해 보세요.";
    }

    // Gemini API Request/Response DTOs
    private record GeminiRequest(List<Content> contents) {}
    private record GeminiResponse(List<Candidate> candidates) {}
    private record Candidate(Content content) {}
    private record Content(List<Part> parts) {}
    private record Part(String text) {}
}
