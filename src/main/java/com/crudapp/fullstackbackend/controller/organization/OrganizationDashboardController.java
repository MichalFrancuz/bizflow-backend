
package com.crudapp.fullstackbackend.controller.organization;

import com.crudapp.fullstackbackend.dto.dashboard.OrganizationDashboardResponse;
import com.crudapp.fullstackbackend.service.dashboard.OrganizationDashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/{organizationId}/dashboard")
public class OrganizationDashboardController {

    private final OrganizationDashboardService dashboardService;

    public OrganizationDashboardController(
            OrganizationDashboardService dashboardService
    ) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public OrganizationDashboardResponse getDashboard(
            @PathVariable Long organizationId
    ) {
        return dashboardService.getDashboard(organizationId);
    }
}
