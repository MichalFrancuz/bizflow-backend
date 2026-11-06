
package com.crudapp.fullstackbackend.controller.project;

import com.crudapp.fullstackbackend.dto.project.CreateProjectRevenueRequest;
import com.crudapp.fullstackbackend.dto.project.ProjectRevenueResponse;
import com.crudapp.fullstackbackend.mapper.ProjectRevenueMapper;
import com.crudapp.fullstackbackend.model.ProjectRevenue;
import com.crudapp.fullstackbackend.service.project.ProjectRevenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/api/organizations/{organizationId}/projects/{projectId}/revenues"
)
public class ProjectRevenueController {

    private final ProjectRevenueService projectRevenueService;
    private final ProjectRevenueMapper projectRevenueMapper;

    public ProjectRevenueController(
            ProjectRevenueService projectRevenueService,
            ProjectRevenueMapper projectRevenueMapper
    ) {
        this.projectRevenueService = projectRevenueService;
        this.projectRevenueMapper = projectRevenueMapper;
    }

    @GetMapping
    public List<ProjectRevenueResponse> getRevenues(
            @PathVariable Long organizationId,
            @PathVariable Long projectId
    ) {
        return projectRevenueService.getRevenues(organizationId, projectId)
                .stream()
                .map(projectRevenueMapper::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectRevenueResponse createRevenue(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProjectRevenueRequest request
    ) {
        ProjectRevenue revenue = new ProjectRevenue();
        revenue.setCategory(request.category());
        revenue.setDescription(request.description());
        revenue.setAmount(request.amount());
        revenue.setCurrency(request.currency());
        revenue.setRevenueDate(request.revenueDate());

        ProjectRevenue createdRevenue = projectRevenueService.createRevenue(
                organizationId,
                projectId,
                revenue
        );

        return projectRevenueMapper.toResponse(createdRevenue);
    }

    @PutMapping("/{revenueId}")
    public ProjectRevenueResponse updateRevenue(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @PathVariable Long revenueId,
            @Valid @RequestBody CreateProjectRevenueRequest request
    ) {
        ProjectRevenue updatedRevenue = new ProjectRevenue();
        updatedRevenue.setCategory(request.category());
        updatedRevenue.setDescription(request.description());
        updatedRevenue.setAmount(request.amount());
        updatedRevenue.setCurrency(request.currency());
        updatedRevenue.setRevenueDate(request.revenueDate());

        ProjectRevenue result = projectRevenueService.updateRevenue(
                organizationId,
                projectId,
                revenueId,
                updatedRevenue
        );

        return projectRevenueMapper.toResponse(result);
    }

    @DeleteMapping("/{revenueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRevenue(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @PathVariable Long revenueId
    ) {
        projectRevenueService.deleteRevenue(
                organizationId,
                projectId,
                revenueId
        );
    }
}
