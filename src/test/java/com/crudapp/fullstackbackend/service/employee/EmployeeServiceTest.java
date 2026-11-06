
package com.crudapp.fullstackbackend.service.employee;

import com.crudapp.fullstackbackend.model.Employee;
import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import com.crudapp.fullstackbackend.repository.EmployeeRepository;
import com.crudapp.fullstackbackend.service.organization.OrganizationAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final Set<Role> MANAGEMENT_ROLES =
            Set.of(Role.OWNER, Role.ADMIN, Role.MANAGER);

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrganizationAccessService organizationAccessService;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void shouldCreateEmployeeWithNormalizedData() {
        Organization organization = new Organization();
        organization.setActive(true);

        when(organizationAccessService.requireRole(1L, MANAGEMENT_ROLES))
                .thenReturn(organization);

        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> {
                    Employee employee = invocation.getArgument(0);
                    setEmployeeId(employee, 100L);
                    return employee;
                });

        Long employeeId = employeeService.createEmployee(
                1L,
                "  John  ",
                "  Smith  ",
                "  JOHN@EXAMPLE.COM  ",
                " 123456789 ",
                new BigDecimal("7500.00"),
                " pln "
        );

        assertEquals(100L, employeeId);

        ArgumentCaptor<Employee> captor =
                ArgumentCaptor.forClass(Employee.class);

        verify(employeeRepository).save(captor.capture());

        Employee savedEmployee = captor.getValue();

        assertSame(organization, savedEmployee.getOrganization());
        assertEquals("John", savedEmployee.getName());
        assertEquals("Smith", savedEmployee.getFamilyname());
        assertEquals("JOHN@EXAMPLE.COM", savedEmployee.getEmail());
        assertEquals("123456789", savedEmployee.getPhonenumber());
        assertEquals(new BigDecimal("7500.00"), savedEmployee.getSalary());
        assertEquals("PLN", savedEmployee.getSalaryCurrency());

        verify(organizationAccessService)
                .requireRole(1L, MANAGEMENT_ROLES);
    }

    @Test
    void shouldRejectEmployeeCreationWithoutManagementPermission() {
        doThrow(new AccessDeniedException("Insufficient permissions"))
                .when(organizationAccessService)
                .requireRole(1L, MANAGEMENT_ROLES);

        assertThrows(
                AccessDeniedException.class,
                () -> employeeService.createEmployee(
                        1L, "John", null, null, null, null, null
                )
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void shouldRejectEmptyEmployeeName() {
        when(organizationAccessService.requireRole(1L, MANAGEMENT_ROLES))
                .thenReturn(new Organization());

        assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.createEmployee(
                        1L, " ", null, null, null, null, null
                )
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void shouldRejectNegativeSalary() {
        when(organizationAccessService.requireRole(1L, MANAGEMENT_ROLES))
                .thenReturn(new Organization());

        assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.createEmployee(
                        1L, "John", null, null, null,
                        new BigDecimal("-100.00"), "PLN"
                )
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void shouldRejectInvalidSalaryCurrency() {
        when(organizationAccessService.requireRole(1L, MANAGEMENT_ROLES))
                .thenReturn(new Organization());

        assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.createEmployee(
                        1L, "John", null, null, null,
                        new BigDecimal("5000.00"), "PL"
                )
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void shouldReturnActiveEmployeesForOrganizationMember() {
        List<Employee> employees = List.of(
                new Employee(),
                new Employee()
        );

        when(employeeRepository.findByOrganizationIdAndActiveTrue(1L))
                .thenReturn(employees);

        List<Employee> result = employeeService.getActiveEmployees(1L);

        assertEquals(2, result.size());
        assertSame(employees, result);

        verify(organizationAccessService).requireMembership(1L);
        verify(employeeRepository)
                .findByOrganizationIdAndActiveTrue(1L);
    }

    @Test
    void shouldRejectListingEmployeesForNonMember() {
        doThrow(new AccessDeniedException("No organization access"))
                .when(organizationAccessService)
                .requireMembership(1L);

        assertThrows(
                AccessDeniedException.class,
                () -> employeeService.getActiveEmployees(1L)
        );

        verify(employeeRepository, never())
                .findByOrganizationIdAndActiveTrue(any());
    }

    @Test
    void shouldReturnEmployeeWithinOrganization() {
        Employee employee = new Employee();

        when(employeeRepository.findByIdAndOrganizationId(100L, 1L))
                .thenReturn(Optional.of(employee));

        Employee result = employeeService.getEmployee(100L, 1L);

        assertSame(employee, result);

        verify(organizationAccessService).requireMembership(1L);
        verify(employeeRepository).findByIdAndOrganizationId(100L, 1L);
    }

    @Test
    void shouldRejectGettingEmployeeForNonMember() {
        doThrow(new AccessDeniedException("No organization access"))
                .when(organizationAccessService)
                .requireMembership(1L);

        assertThrows(
                AccessDeniedException.class,
                () -> employeeService.getEmployee(100L, 1L)
        );

        verify(employeeRepository, never())
                .findByIdAndOrganizationId(any(), any());
    }

    @Test
    void shouldRejectEmployeeFromDifferentOrganization() {
        when(employeeRepository.findByIdAndOrganizationId(100L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                EmptyResultDataAccessException.class,
                () -> employeeService.getEmployee(100L, 2L)
        );
    }

    @Test
    void shouldUpdateEmployeeData() {
        Employee employee = new Employee();
        employee.setActive(true);

        when(employeeRepository.findByIdAndOrganizationId(100L, 1L))
                .thenReturn(Optional.of(employee));

        employeeService.updateEmployee(
                100L,
                1L,
                "  John Updated ",
                " Smith ",
                " john@example.com ",
                " 987654321 ",
                new BigDecimal("8500.00"),
                "eur"
        );

        assertEquals("John Updated", employee.getName());
        assertEquals("Smith", employee.getFamilyname());
        assertEquals("john@example.com", employee.getEmail());
        assertEquals("987654321", employee.getPhonenumber());
        assertEquals(new BigDecimal("8500.00"), employee.getSalary());
        assertEquals("EUR", employee.getSalaryCurrency());

        verify(organizationAccessService)
                .requireRole(1L, MANAGEMENT_ROLES);
    }

    @Test
    void shouldRejectUpdatingEmployeeWithoutManagementPermission() {
        doThrow(new AccessDeniedException("Insufficient permissions"))
                .when(organizationAccessService)
                .requireRole(1L, MANAGEMENT_ROLES);

        assertThrows(
                AccessDeniedException.class,
                () -> employeeService.updateEmployee(
                        100L, 1L, "John", null, null, null, null, null
                )
        );

        verify(employeeRepository, never())
                .findByIdAndOrganizationId(any(), any());
    }

    @Test
    void shouldRejectUpdatingInactiveEmployee() {
        Employee employee = new Employee();
        employee.setActive(false);

        when(employeeRepository.findByIdAndOrganizationId(100L, 1L))
                .thenReturn(Optional.of(employee));

        assertThrows(
                IllegalStateException.class,
                () -> employeeService.updateEmployee(
                        100L, 1L, "John", null, null, null, null, null
                )
        );

        assertNull(employee.getName());
    }

    @Test
    void shouldDeactivateEmployee() {
        Employee employee = new Employee();
        employee.setActive(true);

        when(employeeRepository.findByIdAndOrganizationId(100L, 1L))
                .thenReturn(Optional.of(employee));

        employeeService.deactivateEmployee(100L, 1L);

        assertFalse(employee.isActive());

        verify(organizationAccessService)
                .requireRole(1L, MANAGEMENT_ROLES);
    }

    @Test
    void shouldRejectDeactivatingEmployeeWithoutManagementPermission() {
        doThrow(new AccessDeniedException("Insufficient permissions"))
                .when(organizationAccessService)
                .requireRole(1L, MANAGEMENT_ROLES);

        assertThrows(
                AccessDeniedException.class,
                () -> employeeService.deactivateEmployee(100L, 1L)
        );

        verify(employeeRepository, never())
                .findByIdAndOrganizationId(any(), any());
    }

    private void setEmployeeId(Employee employee, Long id) {
        try {
            var idField = Employee.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(employee, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
