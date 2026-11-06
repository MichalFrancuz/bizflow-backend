
package com.crudapp.fullstackbackend.controller.organization;

import com.crudapp.fullstackbackend.dto.organization.member.AddOrganizationMemberRequest;
import com.crudapp.fullstackbackend.dto.organization.member.OrganizationMemberResponse;
import com.crudapp.fullstackbackend.dto.organization.member.UpdateOrganizationMemberRoleRequest;
import com.crudapp.fullstackbackend.service.organization.OrganizationMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/members")
public class OrganizationMemberController {

    private final OrganizationMemberService organizationMemberService;

    public OrganizationMemberController(
            OrganizationMemberService organizationMemberService
    ) {
        this.organizationMemberService = organizationMemberService;
    }

    @GetMapping
    public ResponseEntity<List<OrganizationMemberResponse>> getMembers(
            @PathVariable Long organizationId
    ) {
        return ResponseEntity.ok(
                organizationMemberService.getOrganizationMembers(organizationId)
        );
    }

    @PostMapping
    public ResponseEntity<OrganizationMemberResponse> addMember(
            @PathVariable Long organizationId,
            @Valid @RequestBody AddOrganizationMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationMemberService.addMember(organizationId, request));
    }

    @PutMapping("/{membershipId}")
    public ResponseEntity<OrganizationMemberResponse> updateMemberRole(
            @PathVariable Long organizationId,
            @PathVariable Long membershipId,
            @Valid @RequestBody UpdateOrganizationMemberRoleRequest request
    ) {
        return ResponseEntity.ok(
                organizationMemberService.updateMemberRole(
                        organizationId,
                        membershipId,
                        request
                )
        );
    }

    @DeleteMapping("/{membershipId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long organizationId,
            @PathVariable Long membershipId
    ) {
        organizationMemberService.removeMember(organizationId, membershipId);

        return ResponseEntity.noContent().build();
    }
}
