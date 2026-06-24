package com.team10.backend.domain.youngPolicy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyRecommendReq;
import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicySearchReq;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyRecommendItem;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyRecommendRes;
import com.team10.backend.domain.youngPolicy.entity.YoungPolicy;
import com.team10.backend.domain.youngPolicy.repository.YoungPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyRagRecommendService {

    private final YoungPolicyRepository youngPolicyRepository;
    private final GeminiChatService geminiChatService;
    private final ObjectMapper objectMapper;

    public YoungPolicyRecommendRes recommend(YoungPolicyRecommendReq request) {
        // 1. 1차 메타데이터 필터링 (나이, 지역, 카테고리 적용)
        YoungPolicySearchReq searchFilter = new YoungPolicySearchReq(
                request.age(),
                request.region(),
                request.category(),
                null
        );
        Pageable pageable = PageRequest.of(0, 50);
        List<YoungPolicy> filteredPolicies = youngPolicyRepository.search(searchFilter, pageable).getContent();

        if (filteredPolicies.isEmpty()) {
            return new YoungPolicyRecommendRes(List.of());
        }

        // 2. 키워드 매칭 가중치를 이용해 상위 10개 후보군 선별 (LLM 입력 토큰 최적화 및 1차 Rerank)
        String query = request.query();
        List<YoungPolicy> rerankedCandidates = filteredPolicies.stream()
                .sorted(Comparator.comparingDouble((YoungPolicy p) -> calculateMatchScore(p, query)).reversed())
                .limit(10)
                .toList();

        // 3. AI 추천을 위한 프롬프트 가공 (4가지 카드 추천에 특화된 구조)
        String systemInstruction = """
            당신은 대한민국 청년정책 추천 전문 상담사입니다.
            제공된 사용자 정보(나이, 지역, 요구사항)와 엄선된 10개의 청년정책 후보 리스트를 분석하여 다음을 수행해 주세요.
            1. 제공된 후보 정책 중 사용자의 요구사항(질문/고민)에 가장 도움이 되고 부합하는 정책 4가지를 골라주세요.
            2. 반드시 아래 제공되는 JSON 형식으로만 답변하고, 다른 서론이나 설명은 일체 포함하지 마십시오.
            3. 마크다운 코드 블록(```json ... ```) 내에 결과를 넣어주세요.
            
            [응답 JSON 형식 예시]
            ```json
            [
              {"candidateIndex": 1, "reason": "임차료 지원 혜택이 포함되어 있어 초기 창업 비용 부담을 크게 줄일 수 있습니다."},
              {"candidateIndex": 3, "reason": "1인 예비 창업자도 지원이 가능해 1인 기업 시작에 매우 적합합니다."}
            ]
            ```
            
            [주의 사항]
            - candidateIndex는 제공된 후보군 번호(1 ~ 10)를 나타내며, 반드시 정수여야 합니다.
            - reason은 해당 사용자에게 왜 이 정책을 골라주었는지 나타내는 1~2문장의 간결한 맞춤형 추천 사유여야 합니다.
            """;

        StringBuilder policiesContext = new StringBuilder();
        for (int i = 0; i < rerankedCandidates.size(); i++) {
            YoungPolicy p = rerankedCandidates.get(i);
            policiesContext.append(String.format("[%d] 정책명: %s\n - 카테고리: %s - %s\n - 설명: %s\n - 신청조건 연령: 만 %d세 ~ %d세\n\n",
                    i + 1, p.getTitle(), p.getCategory(), p.getSubCategory(), p.getDescription(), p.getMinAge(), p.getMaxAge()));
        }

        String userPrompt = String.format("""
            [사용자 정보]
            - 나이: 만 %d세
            - 거주지역: %s
            - 관심 카테고리: %s
            - 요구사항 및 고민: "%s"
            
            [추천 정책 후보군]
            %s
            
            위 후보군 중 가장 알맞은 4개의 정책을 골라 JSON 형식으로만 답해주세요.
            """, request.age(), request.region(), request.category() != null ? request.category() : "전체", query, policiesContext);

        // 4. Gemini LLM API 호출
        String adviceJson = geminiChatService.generateContent(systemInstruction, userPrompt);

        // 5. LLM 결과 파싱 및 DTO 매핑
        List<YoungPolicyRecommendItem> recommendedItems = parseLlmRecommendation(adviceJson, rerankedCandidates);

        return new YoungPolicyRecommendRes(recommendedItems);
    }

    /**
     * Gemini의 JSON 응답을 파싱하여 실제 추천 카드 리스트로 변환합니다.
     * 파싱 실패 시 Fallback 로직이 가동되어 1차 필터링 상위 4개 정책에 기본 메시지를 실어 응답합니다.
     */
    private List<YoungPolicyRecommendItem> parseLlmRecommendation(String adviceJson, List<YoungPolicy> candidates) {
        List<YoungPolicyRecommendItem> items = new ArrayList<>();
        try {
            if (adviceJson == null || adviceJson.isBlank()) {
                throw new IllegalArgumentException("Advice JSON is empty");
            }

            // 마크다운 백틱 코드블럭 정제
            String cleanJson = adviceJson.trim();
            if (cleanJson.contains("```json")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("```json") + 7);
                cleanJson = cleanJson.substring(0, cleanJson.lastIndexOf("```"));
            } else if (cleanJson.contains("```")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("```") + 3);
                cleanJson = cleanJson.substring(0, cleanJson.lastIndexOf("```"));
            }
            cleanJson = cleanJson.trim();

            List<Map<String, Object>> llmResults = objectMapper.readValue(
                    cleanJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> res : llmResults) {
                Object indexObj = res.get("candidateIndex");
                if (indexObj instanceof Number indexNum) {
                    int index = indexNum.intValue();
                    // 1-based index를 0-based index로 변환
                    int targetIdx = index - 1;
                    if (targetIdx >= 0 && targetIdx < candidates.size()) {
                        YoungPolicy matchedPolicy = candidates.get(targetIdx);
                        String reason = (String) res.get("reason");
                        items.add(YoungPolicyRecommendItem.of(matchedPolicy, reason));
                    }
                }
            }

            // 정상적으로 4개의 결과 카드가 파싱되었으면 반환
            if (!items.isEmpty()) {
                return items;
            }

        } catch (Exception e) {
            log.warn("Failed to parse Gemini RAG JSON response. Falling back to default list. Error: {}", e.getMessage());
        }

        // Fallback: 1차 키워드 랭킹 상위 4개를 뽑고 기본 사유를 적용합니다.
        items.clear();
        int limit = Math.min(candidates.size(), 4);
        for (int i = 0; i < limit; i++) {
            items.add(YoungPolicyRecommendItem.of(
                    candidates.get(i),
                    "사용자의 연령, 지역 조건 및 관심사 분야에 부합하여 추천해 드리는 청년 정책입니다."
            ));
        }
        return items;
    }

    /**
     * 간단한 텍스트 유사도 점수 산출
     */
    private double calculateMatchScore(YoungPolicy policy, String query) {
        if (query == null || query.isBlank()) {
            return 0.0;
        }

        String[] keywords = query.split("\\s+");
        double score = 0.0;

        String title = policy.getTitle() != null ? policy.getTitle() : "";
        String desc = policy.getDescription() != null ? policy.getDescription() : "";
        String subCategory = policy.getSubCategory() != null ? policy.getSubCategory() : "";

        for (String kw : keywords) {
            if (kw.length() < 2) continue; // 1글자 조사 제외

            if (title.contains(kw)) {
                score += 10.0;
            }
            if (subCategory.contains(kw)) {
                score += 5.0;
            }
            if (desc.contains(kw)) {
                score += 2.0;
            }
        }
        return score;
    }
}
