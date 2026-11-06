
package com.crudapp.fullstackbackend.controller.project;

import com.crudapp.fullstackbackend.dto.project.ProjectFinancialSummaryResponse;
import com.crudapp.fullstackbackend.service.project.ProjectFinancialSummaryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/organizations/{organizationId}/projects/{projectId}/financial-summary"
)
public class ProjectFinancialSummaryController {

    private final ProjectFinancialSummaryService financialSummaryService;

    public ProjectFinancialSummaryController(
            ProjectFinancialSummaryService financialSummaryService
    ) {
        this.financialSummaryService = financialSummaryService;
    }

    @GetMapping
    public ProjectFinancialSummaryResponse getFinancialSummary(
            @PathVariable Long organizationId,
            @PathVariable Long projectId
    ) {
        return financialSummaryService.getFinancialSummary(
                organizationId,
                projectId
        );
    }
}
