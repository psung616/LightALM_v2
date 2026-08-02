package com.lightalm.web;

import com.lightalm.dto.ChangePasswordRequest;
import com.lightalm.dto.CreateUserRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.UpdateUserRequest;
import com.lightalm.dto.UserResponse;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> list(Pageable pageable) {
        return userService.list(pageable);
    }

    @PostMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PutMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        userService.deactivate(id);
    }

    @PutMapping("/api/users/me/password")
    public void changeMyPassword(@AuthenticationPrincipal UserPrincipal principal,
                                  @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
    }
}
