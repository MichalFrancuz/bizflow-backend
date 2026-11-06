
package com.crudapp.fullstackbackend.controller.project;

import com.crudapp.fullstackbackend.dto.project.CreateProjectRequest;
import com.crudapp.fullstackbackend.dto.project.ProjectResponse;
import com.crudapp.fullstackbackend.dto.project.UpdateProjectRequest;
import com.crudapp.fullstackbackend.dto.project.UpdateProjectStatusRequest;
import com.crudapp.fullstackbackend.mapper.ProjectMapper;
import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.service.project.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;

    public ProjectController(
            ProjectService projectService,
            ProjectMapper projectMapper
    ) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects(
            @PathVariable Long organizationId
    ) {
        List<ProjectResponse> projects = projectService
                .getProjects(organizationId)
                .stream()
                .map(projectMapper::toResponse)
                .toList();

        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long organizationId,
            @PathVariable Long projectId
    ) {
        Project project = projectService.getProject(
                organizationId,
                projectId
        );

        return ResponseEntity.ok(projectMapper.toResponse(project));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        Project project = projectService.createProject(
                organizationId,
                request.name(),
                request.description(),
                request.budget(),
                request.currency(),
                request.startDate(),
                request.endDate()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectMapper.toResponse(project));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        Project project = projectService.updateProject(
                organizationId,
                projectId,
                request.name(),
                request.description(),
                request.budget(),
                request.currency(),
                request.startDate(),
                request.endDate()
        );

        return ResponseEntity.ok(projectMapper.toResponse(project));
    }

    @PatchMapping("/{projectId}/status")
    public ResponseEntity<ProjectResponse> updateProjectStatus(
            @PathVariable Long organizationId,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectStatusRequest request
    ) {
        Project project = projectService.updateProjectStatus(
                organizationId,
                projectId,
                request.status()
        );

        return ResponseEntity.ok(projectMapper.toResponse(project));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long organizationId,
            @PathVariable Long projectId
    ) {
        projectService.deleteProject(organizationId, projectId);

        return ResponseEntity.noContent().build();
    }
}
