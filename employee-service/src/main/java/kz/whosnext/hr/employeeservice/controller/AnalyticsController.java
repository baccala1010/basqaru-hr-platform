package kz.whosnext.hr.employeeservice.controller;

import kz.whosnext.hr.employeeservice.model.dto.response.AnalyticsResponse;
import kz.whosnext.hr.employeeservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public AnalyticsResponse getOverview() {
        return analyticsService.getOverview();
    }
}

