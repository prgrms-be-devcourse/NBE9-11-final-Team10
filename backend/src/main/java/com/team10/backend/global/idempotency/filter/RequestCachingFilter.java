package com.team10.backend.global.idempotency.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

// OncePerRequestFilter: HTTP 요청마다 한 번씩 실행되는 필터
// Spring의 Filter Chain에 자동으로 들어가서 요청이 컨트롤러/AOP에 도달하기 전에 실행
// HttpServletRequest의 body input stream은 원래 한 번 읽으면 다시 읽기 어렵다.
// 그래서 멱등성 AOP에서 request body를 hash에 포함하려면, 요청 body를 캐싱할 수 있는 wrapper가 필요하다.
// ContentCachingRequestWrapper가 body를 “미리 읽어두는” 건 아니고, downstream에서 body가 읽히면 그 내용을 캐시에 저장하는 구조
@Component
public class RequestCachingFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(request, 1024 * 1024); // request body를 최대 1MB까지 캐싱

        filterChain.doFilter(wrappedRequest, response);
    }
}
