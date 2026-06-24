package com.team10.backend.global.idempotency.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.GlobalErrorCode;
import com.team10.backend.global.idempotency.annotation.Idempotent;
import com.team10.backend.global.idempotency.entity.Idempotency;
import com.team10.backend.global.idempotency.filter.RequestCachingFilter;
import com.team10.backend.global.idempotency.service.IdempotencyRequestHasher;
import com.team10.backend.global.idempotency.service.IdempotencyReservationService;
import com.team10.backend.global.idempotency.service.IdempotencyReserveResult;
import com.team10.backend.global.idempotency.service.IdempotencyService;
import com.team10.backend.global.idempotency.type.IdempotencyOperationType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.ContentCachingRequestWrapper;
import tools.jackson.databind.exc.JsonNodeException;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;
    private final IdempotencyRequestHasher idempotencyRequestHasher;
    private final IdempotencyReservationService idempotencyReservationService;
    private final TransactionTemplate transactionTemplate;
    private final HttpServletRequest httpServletRequest;
    private final ObjectMapper objectMapper;


    // @Around: 메서드 실행 전, 후, 예외 발생 시점까지 전부 감쌀 수 있는 AOP 방식
    @Around("@annotation(idempotent)")
    public Object handle(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        Long userId = resolveUserId();
        String idempotencyKey = resolveIdempotencyKey();
        String requestHash = generateRequestHash(idempotent.operationType());

        IdempotencyReserveResult<?> reserveResult =
                idempotencyReservationService.reserveOrResolveDuplicate(
                        userId,
                        idempotent.operationType(),
                        idempotencyKey,
                        requestHash,
                        getReturnType(joinPoint)
                );

        if (reserveResult.replay()) {
            return reserveResult.storedResponse();
        }

        Idempotency idempotency = reserveResult.idempotency();

        try {
            return executeBusinessAndCompleteSuccess(joinPoint, idempotency);
        } catch (BusinessException e) {
            idempotencyService.completeFailure(idempotency.getId());
            throw e;
        }
    }

    private Long resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Number number) {
            return number.longValue();
        }

        throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
    }

    private String resolveIdempotencyKey() {
        return httpServletRequest.getHeader("Idempotency-Key");
    }

    private String generateRequestHash(IdempotencyOperationType operationType) {
        String method = httpServletRequest.getMethod();             // POST
        String uri = httpServletRequest.getRequestURI();            // /api/transfers
        String queryString = httpServletRequest.getQueryString();   // validate=true
        String body = resolveRequestBody();                         // {"fromAccountId":1,"toAccountId":2,"amount":10000}

        return idempotencyRequestHasher.generate(
                operationType,
                method,
                uri,
                queryString,
                body
        );
    }

    private String resolveRequestBody() {
        // ContentCachingRequestWrapper는 필터에서 요청 body를 캐싱해 둔 wrapper다.
        // 일반 HttpServletRequest의 InputStream은 한 번 읽으면 다시 읽을 수 없으므로,
        // Aspect에서는 wrapper에 캐싱된 byte 배열만 읽어야 컨트롤러의 @RequestBody 처리와 충돌하지 않는다.
        if (!(httpServletRequest instanceof ContentCachingRequestWrapper wrapper)) {
            return "";
        }

        byte[] content = wrapper.getContentAsByteArray();

        return new String(content, StandardCharsets.UTF_8);
    }

    private String normalizeJsonBody(String rawBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(rawBody);
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            return rawBody;
        }
    }

    private Object executeBusinessAndCompleteSuccess(
            ProceedingJoinPoint joinPoint,
            Idempotency idempotency
    ) throws Throwable {
        // 비즈니스 처리와 멱등성 SUCCESS 기록을 같은 트랜잭션에 묶어 둘 중 하나만 커밋되는 상태를 방지한다.
        try {
            return transactionTemplate.execute(status -> {
                try {
                    Object response = joinPoint.proceed(); // @Idempotent가 붙은 메서드 전체를 실행
                    idempotencyService.completeSuccess(idempotency.getId(), response);
                    return response;
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new ProceedingFailure(e);
                }
            });
        } catch (ProceedingFailure e) {
            throw e.getCause();
        }
    }

    // IdempotencyService.reserve()는 저장된 JSON 응답을 다시 DTO로 복원해야 하므로 반환 타입이 필요함
    // TransferService.transfer() 반환 타입이 TransferRes -> TransferRes.class
    private Class<?> getReturnType(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getReturnType();
    }

    private static class ProceedingFailure extends RuntimeException {

        private ProceedingFailure(Throwable cause) {
            super(cause);
        }
    }
}
