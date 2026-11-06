
package com.crudapp.fullstackbackend.repository;

import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository
        extends JpaRepository<OrganizationMember, Long> {

    @EntityGraph(attributePaths = {"organization", "appUser"})
    List<OrganizationMember> findByAppUserId(Long appUserId);

    @EntityGraph(attributePaths = "appUser")
    List<OrganizationMember> findByOrganizationId(Long organizationId);

    Optional<OrganizationMember> findByAppUserIdAndOrganizationId(
            Long appUserId,
            Long organizationId
    );

    Optional<OrganizationMember> findByIdAndOrganizationId(
            Long id,
            Long organizationId
    );

    boolean existsByAppUserIdAndOrganizationId(
            Long appUserId,
            Long organizationId
    );

    boolean existsByAppUserIdAndOrganizationIdAndRole(
            Long appUserId,
            Long organizationId,
            Role role
    );

    long countByOrganizationIdAndRole(
            Long organizationId,
            Role role
    );
}
