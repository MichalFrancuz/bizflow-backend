
package com.crudapp.fullstackbackend.service.project;

import com.crudapp.fullstackbackend.model.Employee;
import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.TimeEntry;
import com.crudapp.fullstackbackend.repository.EmployeeRepository;
import com.crudapp.fullstackbackend.repository.ProjectRepository;
import com.crudapp.fullstackbackend.repository.TimeEntryRepository;
import com.crudapp.fullstackbackend.service.organization.OrganizationAccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TimeEntryService {

    private static final Set<Role> WRITE_ROLES =
            Set.of(Role.OWNER, Role.ADMIN, Role.MANAGER);

    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final OrganizationAccessService organizationAccessService;

    public TimeEntryService(
            TimeEntryRepository timeEntryRepository,
            ProjectRepository projectRepository,
            EmployeeRepository employeeRepository,
            OrganizationAccessService organizationAccessService
    ) {
        this.timeEntryRepository = timeEntryRepository;
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
        this.organizationAccessService = organizationAccessService;
    }

    @Transactional(readOnly = true)
    public List<TimeEntry> getTimeEntries(
            Long organizationId,
            Long projectId
    ) {
        Organization organization =
                organizationAccessService.requireMembership(organizationId);

        Project project = findProject(projectId, organization.getId());

        return timeEntryRepository.findByProjectId(project.getId());
    }

    @Transactional(readOnly = true)
    public List<TimeEntry> getEmployeeTimeEntries(
            Long organizationId,
            Long employeeId
    ) {
        Organization organization =
                organizationAccessService.requireMembership(organizationId);

        Employee employee = findEmployee(employeeId, organization.getId());

        return timeEntryRepository.findByEmployeeId(employee.getId());
    }

    public TimeEntry createTimeEntry(
            Long organizationId,
            Long projectId,
            Long employeeId,
            TimeEntry timeEntry
    ) {
        Organization organization = organizationAccessService.requireRole(
                organizationId,
                WRITE_ROLES
        );

        Project project = findProject(projectId, organization.getId());
        Employee employee = findEmployee(employeeId, organization.getId());

        validateTimeEntry(timeEntry);

        timeEntry.setDescription(timeEntry.getDescription().trim());
        timeEntry.setCurrency(timeEntry.getCurrency().trim().toUpperCase());
        timeEntry.setProject(project);
        timeEntry.setEmployee(employee);

        return timeEntryRepository.save(timeEntry);
    }

    public TimeEntry updateTimeEntry(
            Long organizationId,
            Long projectId,
            Long timeEntryId,
            Long employeeId,
            TimeEntry updatedEntry
    ) {
        Organization organization = organizationAccessService.requireRole(
                organizationId,
                WRITE_ROLES
        );

        Project project = findProject(projectId, organization.getId());
        Employee employee = findEmployee(employeeId, organization.getId());

        TimeEntry existingEntry = timeEntryRepository
                .findByIdAndProjectId(timeEntryId, project.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Time entry not found")
                );

        validateTimeEntry(updatedEntry);

        existingEntry.setEmployee(employee);
        existingEntry.setDescription(updatedEntry.getDescription().trim());
        existingEntry.setWorkDate(updatedEntry.getWorkDate());
        existingEntry.setHours(updatedEntry.getHours());
        existingEntry.setHourlyRate(updatedEntry.getHourlyRate());
        existingEntry.setCurrency(
                updatedEntry.getCurrency().trim().toUpperCase()
        );

        return timeEntryRepository.save(existingEntry);
    }

    public void deleteTimeEntry(
            Long organizationId,
            Long projectId,
            Long timeEntryId
    ) {
        Organization organization = organizationAccessService.requireRole(
                organizationId,
                Set.of(Role.OWNER, Role.ADMIN)
        );

        Project project = findProject(projectId, organization.getId());

        TimeEntry entry = timeEntryRepository
                .findByIdAndProjectId(timeEntryId, project.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Time entry not found")
                );

        timeEntryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public List<TimeEntry> getTimeEntriesByDateRange(
            Long organizationId,
            Long projectId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Organization organization =
                organizationAccessService.requireMembership(organizationId);

        Project project = findProject(projectId, organization.getId());

        if (startDate == null || endDate == null
                || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Invalid date range");
        }

        return timeEntryRepository.findByProjectIdAndWorkDateBetween(
                project.getId(),
                startDate,
                endDate
        );
    }

    private Project findProject(Long projectId, Long organizationId) {
        return projectRepository.findByIdAndOrganizationId(
                projectId,
                organizationId
        ).orElseThrow(() ->
                new EntityNotFoundException("Project not found")
        );
    }

    private Employee findEmployee(Long employeeId, Long organizationId) {
        return employeeRepository.findByIdAndOrganizationId(
                        employeeId,
                        organizationId
                ).filter(Employee::isActive)
                .orElseThrow(() ->
                        new EntityNotFoundException("Active employee not found")
                );
    }

    private void validateTimeEntry(TimeEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Time entry cannot be null");
        }

        if (entry.getDescription() == null
                || entry.getDescription().isBlank()
                || entry.getDescription().length() > 1000) {
            throw new IllegalArgumentException(
                    "Description must contain 1 to 1000 characters"
            );
        }

        if (entry.getWorkDate() == null) {
            throw new IllegalArgumentException("Work date is required");
        }

        if (entry.getHours() == null
                || entry.getHours().compareTo(BigDecimal.ZERO) <= 0
                || entry.getHours().compareTo(new BigDecimal("24.00")) > 0) {
            throw new IllegalArgumentException(
                    "Working hours must be greater than zero and cannot exceed 24"
            );
        }

        if (entry.getHourlyRate() == null
                || entry.getHourlyRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Hourly rate cannot be negative"
            );
        }

        if (entry.getCurrency() == null
                || !entry.getCurrency().trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must contain exactly three letters"
            );
        }
    }
}
