package com.lightalm.web;

import com.lightalm.dto.MyDashboardResponse;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/me/dashboard")
    public MyDashboardResponse myDashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.myDashboard(principal);
    }
}
