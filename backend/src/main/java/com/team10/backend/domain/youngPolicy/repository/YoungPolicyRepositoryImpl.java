package com.team10.backend.domain.youngPolicy.repository;

import static com.team10.backend.domain.youngPolicy.entity.QYoungPolicy.youngPolicy;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.team10.backend.domain.youngPolicy.type.Region;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicySearchReq;
import com.team10.backend.domain.youngPolicy.entity.YoungPolicy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class YoungPolicyRepositoryImpl implements YoungPolicyRepositoryCustom {

    private static final String NATIONAL_REGION_NAME = "전국";
    private static final String NATIONAL_REGION_CODE = "3001";

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<YoungPolicy> search(YoungPolicySearchReq filter, Pageable pageable) {
        BooleanBuilder condition = buildSearchCondition(filter);

        List<YoungPolicy> content = queryFactory
                .selectFrom(youngPolicy)
                .where(condition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(youngPolicy.count())
                .from(youngPolicy)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private BooleanBuilder buildSearchCondition(YoungPolicySearchReq filter) {
        return new BooleanBuilder()
                .and(ageLoeAndGoe(filter.age()))
                .and(regionContains(filter.region()))
                .and(categoryEq(filter.category()))
                .and(keywordContains(filter.keyword()));
    }

    private BooleanBuilder ageLoeAndGoe(Integer age) {
        if (age == null) {
            return null;
        }
        BooleanBuilder builder = new BooleanBuilder();
        // 최소연령 조건: 최소연령 정보가 없거나, 0세이거나, 입력한 나이가 최소연령 이상인 경우
        builder.and(youngPolicy.minAge.isNull()
                .or(youngPolicy.minAge.eq(0))
                .or(youngPolicy.minAge.loe(age)));
        
        // 최대연령 조건: 최대연령 정보가 없거나, 0세이거나, 99세이거나, 입력한 나이가 최대연령 이하인 경우
        builder.and(youngPolicy.maxAge.isNull()
                .or(youngPolicy.maxAge.eq(0))
                .or(youngPolicy.maxAge.eq(99))
                .or(youngPolicy.maxAge.goe(age)));
        
        return builder;
    }

    private BooleanExpression regionContains(String regionName) {
        if (!StringUtils.hasText(regionName)) {
            return null;
        }

        // 1. regionName에 해당하는 Region enum 매칭 (영문/한글 모두 지원)
        Region targetRegion = null;
        for (Region r : Region.values()) {
            if (r.name().equalsIgnoreCase(regionName.trim())) {
                targetRegion = r;
                break;
            }
        }

        if (targetRegion == null) {
            for (Region r : Region.values()) {
                for (String name : r.getNames()) {
                    if (regionName.contains(name)) {
                        targetRegion = r;
                        break;
                    }
                }
                if (targetRegion != null) {
                    break;
                }
            }
        }

        // 2. 매칭되는 Region이 존재하는 경우 한글 지명 및 시도 코드로 맵핑 조건 생성
        if (targetRegion != null) {
            BooleanExpression exp = null;
            for (String name : targetRegion.getNames()) {
                BooleanExpression nameCondition = youngPolicy.regionCode.containsIgnoreCase(name);
                exp = (exp == null) ? nameCondition : exp.or(nameCondition);
            }
            if (targetRegion.getCode() != null) {
                String code = targetRegion.getCode();
                exp = exp.or(youngPolicy.regionCode.startsWith(code))
                         .or(youngPolicy.regionCode.like("%," + code + "%"));
            }

            return exp.or(youngPolicy.regionCode.contains(NATIONAL_REGION_NAME))
                    .or(youngPolicy.regionCode.contains(NATIONAL_REGION_CODE))
                    .or(youngPolicy.regionCode.isNull())
                    .or(youngPolicy.regionCode.isEmpty());
        }

        // 3. 만약 해당하는 Region 매핑이 없으면 한글 검색어로 직접 포함 여부 검사
        return youngPolicy.regionCode.containsIgnoreCase(regionName)
                .or(youngPolicy.regionCode.contains(NATIONAL_REGION_NAME))
                .or(youngPolicy.regionCode.contains(NATIONAL_REGION_CODE))
                .or(youngPolicy.regionCode.isNull())
                .or(youngPolicy.regionCode.isEmpty());
    }

    private BooleanExpression categoryEq(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        return youngPolicy.category.eq(category);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return youngPolicy.title.containsIgnoreCase(keyword)
                .or(youngPolicy.description.containsIgnoreCase(keyword));
    }
}
