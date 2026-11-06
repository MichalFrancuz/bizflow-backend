
package com.crudapp.fullstackbackend.controller.organization;

import com.crudapp.fullstackbackend.dto.organization.CreateOrganizationRequest;
import com.crudapp.fullstackbackend.dto.organization.OrganizationResponse;
import com.crudapp.fullstackbackend.service.organization.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request
    ) {
        Long organizationId = organizationService.createOrganization(
                request.name(),
                request.taxId()
        );

        return Map.of("organizationId", organizationId);
    }

    @GetMapping
    public List<OrganizationResponse> getCurrentUserOrganizations() {
        return organizationService.getCurrentUserOrganizations();
    }
}
