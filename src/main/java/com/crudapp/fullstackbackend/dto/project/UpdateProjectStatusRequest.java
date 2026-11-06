
package com.crudapp.fullstackbackend.dto.project;

import com.crudapp.fullstackbackend.model.Project.Status;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectStatusRequest(

        @NotNull(message = "Project status cannot be empty")
        Status status

) {}
