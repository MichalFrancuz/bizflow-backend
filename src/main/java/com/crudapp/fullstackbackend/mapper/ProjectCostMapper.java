
package com.crudapp.fullstackbackend.mapper;

import com.crudapp.fullstackbackend.dto.project.ProjectCostResponse;
import com.crudapp.fullstackbackend.model.ProjectCost;
import org.springframework.stereotype.Component;

@Component
public class ProjectCostMapper {

    public ProjectCostResponse toResponse(ProjectCost cost) {
        return new ProjectCostResponse(
                cost.getId(),
                cost.getCategory(),
                cost.getDescription(),
                cost.getAmount(),
                cost.getCurrency(),
                cost.getCostDate(),
                cost.getProject().getId(),
                cost.getCreatedAt()
        );
    }
}
