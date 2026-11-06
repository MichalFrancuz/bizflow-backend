package com.crudapp.fullstackbackend.repository;

import com.crudapp.fullstackbackend.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findByActiveTrue();

    boolean existsByTaxId(String taxId);
}