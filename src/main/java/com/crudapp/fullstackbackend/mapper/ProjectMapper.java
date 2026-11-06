
package com.crudapp.fullstackbackend.mapper;

import com.crudapp.fullstackbackend.dto.project.ProjectResponse;
import com.crudapp.fullstackbackend.model.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getBudget(),
                project.getCurrency(),
                project.getStartDate(),
                project.getEndDate(),
                project.getOrganization().getId(),
                project.getCreatedAt()
        );
    }
}
