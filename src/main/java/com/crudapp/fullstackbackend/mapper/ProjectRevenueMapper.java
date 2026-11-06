
package com.crudapp.fullstackbackend.mapper;

import com.crudapp.fullstackbackend.dto.project.ProjectRevenueResponse;
import com.crudapp.fullstackbackend.model.ProjectRevenue;
import org.springframework.stereotype.Component;

@Component
public class ProjectRevenueMapper {

    public ProjectRevenueResponse toResponse(ProjectRevenue revenue) {
        return new ProjectRevenueResponse(
                revenue.getId(),
                revenue.getCategory(),
                revenue.getDescription(),
                revenue.getAmount(),
                revenue.getCurrency(),
                revenue.getRevenueDate(),
                revenue.getProject().getId(),
                revenue.getCreatedAt()
        );
    }
}
