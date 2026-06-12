package com.team10.backend.domain.user.controller;

import com.team10.backend.domain.user.dto.req.LoginReq;
import com.team10.backend.domain.user.dto.req.TokenRefreshReq;
import com.team10.backend.domain.user.dto.req.UserCreateReq;
import com.team10.backend.domain.user.dto.res.LoginRes;
import com.team10.backend.domain.user.dto.res.TokenRefreshRes;
import com.team10.backend.domain.user.dto.res.UserRes;
import com.team10.backend.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    public ResponseEntity<UserRes> signup(@Valid @RequestBody UserCreateReq request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signup(request));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "Access Token(1h) + Refresh Token(7일) 발급")
    public ResponseEntity<LoginRes> login(@Valid @RequestBody LoginReq request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Access Token 재발급",
            description = "만료된 AT + RT를 전송하면 새 AT + 새 RT를 반환합니다 (Rotation)."
    )
    public ResponseEntity<TokenRefreshRes> refresh(@Valid @RequestBody TokenRefreshReq request) {
        return ResponseEntity.ok(userService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Redis의 Refresh Token을 삭제합니다. 만료된 토큰도 허용됩니다.")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long userId) {
        userService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
