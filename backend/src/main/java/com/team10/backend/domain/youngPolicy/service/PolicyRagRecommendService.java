package com.team10.backend.domain.youngPolicy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyRecommendReq;
import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicySearchReq;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyRecommendItem;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyRecommendRes;
import com.team10.backend.domain.youngPolicy.entity.YoungPolicy;
import com.team10.backend.domain.youngPolicy.repository.YoungPolicyRepository;
import com.team10.backend.domain.youngPolicy.type.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyRagRecommendService {

    private static final TypeReference<List<Map<String, Object>>> RECOMMENDATION_TYPE_REF =
            new TypeReference<>() {};

    private final YoungPolicyRepository youngPolicyRepository;
    private final GeminiChatService geminiChatService;
    private final ObjectMapper objectMapper;

    @Value("classpath:prompts/system-instruction.txt")
    private Resource systemInstructionResource;

    @Value("classpath:prompts/user-prompt.txt")
    private Resource userPromptResource;

    private String systemInstruction;
    private String userPromptTemplate;

    @PostConstruct
    public void init() {
        try {
            this.systemInstruction = StreamUtils.copyToString(
                    systemInstructionResource.getInputStream(),
                    StandardCharsets.UTF_8
            );
            this.userPromptTemplate = StreamUtils.copyToString(
                    userPromptResource.getInputStream(),
                    StandardCharsets.UTF_8
            );
            log.info("Successfully loaded RAG prompt templates from classpath resources.");
        } catch (IOException e) {
            log.error("Failed to load RAG prompt templates from classpath resources.", e);
            throw new IllegalStateException("RAG prompt templates are missing or unreadable.", e);
        }
    }

    public YoungPolicyRecommendRes recommend(YoungPolicyRecommendReq request) {
        // 1차 메타데이터 필터링 (나이, 지역, 카테고리 적용)
        YoungPolicySearchReq searchFilter = new YoungPolicySearchReq(
                request.age(),
                request.region(),
                request.category(),
                null
        );
        Pageable pageable = PageRequest.of(0, 1000);
        List<YoungPolicy> filteredPolicies = youngPolicyRepository.search(searchFilter, pageable).getContent();

        if (filteredPolicies.isEmpty()) {
            return new YoungPolicyRecommendRes(List.of());
        }

        // 키워드 및 지역 매칭 가중치를 이용해 상위 10개 후보군 선별 (LLM 입력 토큰 최적화 및 1차 Rerank)
        String query = request.query();
        String region = request.region();
        List<YoungPolicy> rerankedCandidates = filteredPolicies.stream()
                .sorted(Comparator.comparingDouble((YoungPolicy p) -> calculateMatchScore(p, query, region)).reversed())
                .limit(10)
                .toList();

        // AI 추천을 위한 프롬프트 가공 (4가지 카드 추천에 특화된 구조)
        StringBuilder policiesContext = new StringBuilder();
        for (int i = 0; i < rerankedCandidates.size(); i++) {
            YoungPolicy p = rerankedCandidates.get(i);
            policiesContext.append(String.format("[%d] 정책명: %s\n - 카테고리: %s - %s\n - 설명: %s\n - 신청조건 연령: 만 %d세 ~ %d세\n\n",
                    i + 1, p.getTitle(), p.getCategory(), p.getSubCategory(), p.getDescription(), p.getMinAge(), p.getMaxAge()));
        }

        String userPrompt = String.format(
                userPromptTemplate,
                request.age(),
                request.region(),
                request.category() != null ? request.category() : "전체",
                query,
                policiesContext.toString()
        );

        // Gemini LLM API 호출
        String adviceJson = geminiChatService.generateContent(systemInstruction, userPrompt);

        // LLM 결과 파싱 및 DTO 매핑
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

            List<Map<String, Object>> llmResults = objectMapper.readValue(cleanJson, RECOMMENDATION_TYPE_REF);

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
            log.warn("Failed to parse Gemini RAG JSON response. Falling back to default list.", e);
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
     * 키워드 및 지역 매칭 유사도 점수 산출
     * 제목 가중치를 낮추고, 사용자 지역에 따른 지역 분류 및 가중치를 반영합니다.
     */
    private double calculateMatchScore(YoungPolicy policy, String query, String userRegion) {
        double score = 0.0;

        // 1. 사용자 지역 분류 및 지역 가중치 반영
        if (userRegion != null && !userRegion.isBlank()) {
            String userRegionCode = Region.findCodeByName(userRegion);
            String policyRegionCode = policy.getRegionCode();

            if (policyRegionCode != null) {
                // 정책 지역 코드가 사용자 지역 코드를 포함하는 경우 (예: 서울 "11" 포함)
                if (userRegionCode != null && policyRegionCode.contains(userRegionCode)) {
                    // 전국 정책 코드가 아닌 특정 지자체 코드인 경우 지역 특화 가중치 부여
                    if (!isNationalCode(policyRegionCode)) {
                        score += 50.0; // 지역 특화 정책 가중치 대폭 상향 (15.0 -> 50.0)
                    } else {
                        score += 10.0; // 전국구 정책 가중치 상향 (5.0 -> 10.0)
                    }
                } else if (isNationalCode(policyRegionCode) || policyRegionCode.contains("전국")) {
                    score += 10.0;     // 전국구 정책 가중치
                }
            } else {
                score += 10.0;         // 지역 코드가 없는 경우도 전국구에 준하여 처리
            }
        }

        // 2. 키워드 매칭 가중치 (제목 가중치를 낮춤)
        if (query != null && !query.isBlank()) {
            String[] keywords = query.split("\\s+");
            String title = policy.getTitle() != null ? policy.getTitle() : "";
            String desc = policy.getDescription() != null ? policy.getDescription() : "";
            String subCategory = policy.getSubCategory() != null ? policy.getSubCategory() : "";

            for (String kw : keywords) {
                if (kw.length() < 2) continue; // 1글자 조사 제외

                if (title.contains(kw)) {
                    score += 5.0; // 기존 10.0 -> 5.0으로 하향 조정
                }
                if (subCategory.contains(kw)) {
                    score += 3.0; // 기존 5.0 -> 3.0으로 하향 조정
                }
                if (desc.contains(kw)) {
                    score += 1.0; // 기존 2.0 -> 1.0으로 하향 조정
                }
            }
        }

        return score;
    }

    private boolean isNationalCode(String regionCode) {
        if (regionCode == null) {
            return true;
        }
        return regionCode.contains("전국") ||
               regionCode.contains("3001") ||
               regionCode.contains("003002001") ||
               regionCode.isBlank();
    }
}
