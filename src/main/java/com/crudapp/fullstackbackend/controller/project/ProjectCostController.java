
package com.crudapp.fullstackbackend.controller.project;

import com.crudapp.fullstackbackend.dto.project.CreateProjectCostRequest;
import com.crudapp.fullstackbackend.dto.project.ProjectCostResponse;
import com.crudapp.fullstackbackend.mapper.ProjectCostMapper;
import com.crudapp.fullstackbackend.model.ProjectCost;
import com.crudapp.fullstackbackend.service.project.ProjectCostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/api/organizations/{organizationId}/projects/{projectId}/costs"
)
public class ProjectCostController {

    private final ProjectCostService projectCostService;
    private final ProjectCostMapper projectCostMapper;

    public ProjectCostController(
            ProjectCostService projectCostService,
            ProjectCostMapper projectCostMapper
    ) {
        this.projectCostService = projectCostService;
        this.projectCostMapper = projectCostMapper;
    }

    @GetMapping
    public List<ProjectCostResponse> getCosts(
            @PathVariable Long organizationId,
            @PathVariable Long projectId
    ) {
        return projectCostService.getCosts(organizationId, projectId)
                .stream()
                .map(projectCostMapper::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectCostResponse createCost(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProjectCostRequest request
    ) {
        ProjectCost cost = new ProjectCost();
        cost.setCategory(request.category());
        cost.setDescription(request.description());
        cost.setAmount(request.amount());
        cost.setCurrency(request.currency());
        cost.setCostDate(request.costDate());

        ProjectCost createdCost = projectCostService.createCost(
                organizationId,
                projectId,
                cost
        );

        return projectCostMapper.toResponse(createdCost);
    }

    @PutMapping("/{costId}")
    public ProjectCostResponse updateCost(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @PathVariable Long costId,
            @Valid @RequestBody CreateProjectCostRequest request
    ) {
        ProjectCost updatedCost = new ProjectCost();
        updatedCost.setCategory(request.category());
        updatedCost.setDescription(request.description());
        updatedCost.setAmount(request.amount());
        updatedCost.setCurrency(request.currency());
        updatedCost.setCostDate(request.costDate());

        ProjectCost result = projectCostService.updateCost(
                organizationId,
                projectId,
                costId,
                updatedCost
        );

        return projectCostMapper.toResponse(result);
    }

    @DeleteMapping("/{costId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCost(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @PathVariable Long costId
    ) {
        projectCostService.deleteCost(
                organizationId,
                projectId,
                costId
        );
    }
}
