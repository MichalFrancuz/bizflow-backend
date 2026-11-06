package com.crudapp.fullstackbackend.repository;

import com.crudapp.fullstackbackend.model.TimeEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimeEntryRepository
        extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByProjectOrganizationId(Long organizationId);

    @EntityGraph(attributePaths = {"project", "employee"})
    List<TimeEntry> findByProjectId(Long projectId);

    @EntityGraph(attributePaths = {"project", "employee"})
    List<TimeEntry> findByProjectIdAndEmployeeId(
            Long projectId,
            Long employeeId
    );

    @EntityGraph(attributePaths = {"project", "employee"})
    List<TimeEntry> findByProjectIdAndWorkDateBetween(
            Long projectId,
            LocalDate startDate,
            LocalDate endDate
    );

    @EntityGraph(attributePaths = {"project", "employee"})
    List<TimeEntry> findByEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = {"project", "employee"})
    Optional<TimeEntry> findByIdAndProjectId(
            Long id,
            Long projectId
    );

    boolean existsByIdAndProjectId(Long id, Long projectId);
}