
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrganizationAccessService organizationAccessService;

    @InjectMocks
    private TimeEntryService timeEntryService;

    private Organization organization;
    private Project project;
    private Employee employee;

    @BeforeEach
    void setUp() {
        organization = mock(Organization.class);

        project = new Project();
        ReflectionTestUtils.setField(project, "id", 20L);

        employee = new Employee();
        ReflectionTestUtils.setField(employee, "id", 30L);
        employee.setActive(true);
    }

    @Test
    void shouldReturnProjectTimeEntries() {
        when(organizationAccessService.requireMembership(10L))
                .thenReturn(organization);

        when(organization.getId()).thenReturn(10L);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        TimeEntry entry = createTimeEntry();

        when(timeEntryRepository.findByProjectId(20L))
                .thenReturn(List.of(entry));

        List<TimeEntry> result =
                timeEntryService.getTimeEntries(10L, 20L);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("8.00"), result.getFirst().getHours());

        verify(timeEntryRepository).findByProjectId(20L);
    }

    @Test
    void shouldCreateTimeEntryForAuthorizedManager() {
        prepareAuthorizedProjectAndEmployee();

        TimeEntry entry = createTimeEntry();

        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TimeEntry result = timeEntryService.createTimeEntry(
                10L,
                20L,
                30L,
                entry
        );

        assertEquals(project, result.getProject());
        assertEquals(employee, result.getEmployee());
        assertEquals("Development work", result.getDescription());
        assertEquals("PLN", result.getCurrency());

        verify(organizationAccessService).requireRole(
                eq(10L),
                eq(Set.of(Role.OWNER, Role.ADMIN, Role.MANAGER))
        );

        verify(timeEntryRepository).save(entry);
    }

    @Test
    void shouldRejectTimeEntryForEmployeeFromAnotherOrganization() {
        when(organizationAccessService.requireRole(
                eq(10L),
                anySet()
        )).thenReturn(organization);

        when(organization.getId()).thenReturn(10L);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(employeeRepository.findByIdAndOrganizationId(30L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> timeEntryService.createTimeEntry(
                        10L,
                        20L,
                        30L,
                        createTimeEntry()
                )
        );

        verify(timeEntryRepository, never()).save(any());
    }

    @Test
    void shouldRejectInactiveEmployee() {
        employee.setActive(false);

        when(organizationAccessService.requireRole(
                eq(10L),
                anySet()
        )).thenReturn(organization);

        when(organization.getId()).thenReturn(10L);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(employeeRepository.findByIdAndOrganizationId(30L, 10L))
                .thenReturn(Optional.of(employee));

        assertThrows(
                EntityNotFoundException.class,
                () -> timeEntryService.createTimeEntry(
                        10L,
                        20L,
                        30L,
                        createTimeEntry()
                )
        );

        verify(timeEntryRepository, never()).save(any());
    }

    @Test
    void shouldRejectHoursGreaterThan24() {
        prepareAuthorizedProjectAndEmployee();

        TimeEntry entry = createTimeEntry();
        entry.setHours(new BigDecimal("25.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> timeEntryService.createTimeEntry(
                        10L,
                        20L,
                        30L,
                        entry
                )
        );

        verify(timeEntryRepository, never()).save(any());
    }

    @Test
    void shouldRejectNegativeHourlyRate() {
        prepareAuthorizedProjectAndEmployee();

        TimeEntry entry = createTimeEntry();
        entry.setHourlyRate(new BigDecimal("-10.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> timeEntryService.createTimeEntry(
                        10L,
                        20L,
                        30L,
                        entry
                )
        );

        verify(timeEntryRepository, never()).save(any());
    }

    @Test
    void shouldCalculateTotalCost() {
        TimeEntry entry = createTimeEntry();

        assertEquals(
                new BigDecimal("800.00"),
                entry.getTotalCost()
        );
    }

    @Test
    void shouldDeleteTimeEntryForAuthorizedOwner() {
        when(organizationAccessService.requireRole(
                eq(10L),
                anySet()
        )).thenReturn(organization);

        when(organization.getId()).thenReturn(10L);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        TimeEntry entry = createTimeEntry();

        when(timeEntryRepository.findByIdAndProjectId(40L, 20L))
                .thenReturn(Optional.of(entry));

        timeEntryService.deleteTimeEntry(10L, 20L, 40L);

        verify(timeEntryRepository).delete(entry);
    }

    @Test
    void shouldRejectInvalidDateRange() {
        when(organizationAccessService.requireMembership(10L))
                .thenReturn(organization);

        when(organization.getId()).thenReturn(10L);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalArgumentException.class,
                () -> timeEntryService.getTimeEntriesByDateRange(
                        10L,
                        20L,
                        LocalDate.of(2026, 11, 10),
                        LocalDate.of(2026, 11, 1)
                )
        );

        verify(timeEntryRepository, never())
                .findByProjectIdAndWorkDateBetween(
                        anyLong(),
                        any(),
                        any()
                );
    }

    private void prepareAuthorizedProjectAndEmployee() {
        when(organizationAccessService.requireRole(
                eq(10L),
                anySet()
        )).thenReturn(organization);

        when(organization.getId()).thenReturn(10L);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(employeeRepository.findByIdAndOrganizationId(30L, 10L))
                .thenReturn(Optional.of(employee));
    }

    private TimeEntry createTimeEntry() {
        TimeEntry entry = new TimeEntry();

        entry.setDescription("Development work");
        entry.setWorkDate(LocalDate.of(2026, 11, 5));
        entry.setHours(new BigDecimal("8.00"));
        entry.setHourlyRate(new BigDecimal("100.00"));
        entry.setCurrency("pln");

        return entry;
    }
}
