package com.lightalm.auth.api;

import com.lightalm.auth.dto.SignupRequest;
import com.lightalm.auth.service.SelfSignupService;
import com.lightalm.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-011 §1.2. POST /api/auth/signup — 공개(permitAll).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SelfSignupController {

    private final SelfSignupService selfSignupService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        return selfSignupService.signup(request);
    }
}
