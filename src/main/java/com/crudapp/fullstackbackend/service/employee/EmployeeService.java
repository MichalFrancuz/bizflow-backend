
package com.crudapp.fullstackbackend.service.employee;

import com.crudapp.fullstackbackend.model.Employee;
import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import com.crudapp.fullstackbackend.repository.EmployeeRepository;
import com.crudapp.fullstackbackend.service.organization.OrganizationAccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class EmployeeService {

    private static final Set<Role> EMPLOYEE_MANAGEMENT_ROLES =
            Set.of(Role.OWNER, Role.ADMIN, Role.MANAGER);

    private final EmployeeRepository employeeRepository;
    private final OrganizationAccessService organizationAccessService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            OrganizationAccessService organizationAccessService
    ) {
        this.employeeRepository = employeeRepository;
        this.organizationAccessService = organizationAccessService;
    }

    @Transactional
    public Long createEmployee(
            Long organizationId,
            String name,
            String familyname,
            String email,
            String phonenumber,
            BigDecimal salary,
            String salaryCurrency
    ) {
        Organization organization =
                organizationAccessService.requireRole(
                        organizationId,
                        EMPLOYEE_MANAGEMENT_ROLES
                );

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Employee name cannot be empty"
            );
        }

        validateSalary(salary, salaryCurrency);

        Employee employee = new Employee();
        employee.setOrganization(organization);
        employee.setName(name.trim());
        employee.setFamilyname(normalizeOptionalText(familyname));
        employee.setEmail(normalizeOptionalText(email));
        employee.setPhonenumber(normalizeOptionalText(phonenumber));
        employee.setSalary(salary);
        employee.setSalaryCurrency(normalizeCurrency(salaryCurrency));

        return employeeRepository.save(employee).getId();
    }

    @Transactional(readOnly = true)
    public List<Employee> getActiveEmployees(Long organizationId) {
        organizationAccessService.requireMembership(organizationId);

        return employeeRepository.findByOrganizationIdAndActiveTrue(
                organizationId
        );
    }

    @Transactional(readOnly = true)
    public Employee getEmployee(
            Long employeeId,
            Long organizationId
    ) {
        organizationAccessService.requireMembership(organizationId);

        return employeeRepository.findByIdAndOrganizationId(
                employeeId,
                organizationId
        ).orElseThrow(() ->
                new EntityNotFoundException("Employee not found")
        );
    }

    @Transactional
    public void updateEmployee(
            Long employeeId,
            Long organizationId,
            String name,
            String familyname,
            String email,
            String phonenumber,
            BigDecimal salary,
            String salaryCurrency
    ) {
        organizationAccessService.requireRole(
                organizationId,
                EMPLOYEE_MANAGEMENT_ROLES
        );

        Employee employee = findEmployee(employeeId, organizationId);

        if (!employee.isActive()) {
            throw new IllegalStateException("Employee is inactive");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Employee name cannot be empty"
            );
        }

        validateSalary(salary, salaryCurrency);

        employee.setName(name.trim());
        employee.setFamilyname(normalizeOptionalText(familyname));
        employee.setEmail(normalizeOptionalText(email));
        employee.setPhonenumber(normalizeOptionalText(phonenumber));
        employee.setSalary(salary);
        employee.setSalaryCurrency(normalizeCurrency(salaryCurrency));
    }

    @Transactional
    public void deactivateEmployee(
            Long employeeId,
            Long organizationId
    ) {
        organizationAccessService.requireRole(
                organizationId,
                EMPLOYEE_MANAGEMENT_ROLES
        );

        Employee employee = findEmployee(employeeId, organizationId);
        employee.setActive(false);
    }

    private Employee findEmployee(
            Long employeeId,
            Long organizationId
    ) {
        return employeeRepository.findByIdAndOrganizationId(
                employeeId,
                organizationId
        ).orElseThrow(() ->
                new EntityNotFoundException("Employee not found")
        );
    }

    private void validateSalary(
            BigDecimal salary,
            String salaryCurrency
    ) {
        if (salary != null && salary.signum() < 0) {
            throw new IllegalArgumentException(
                    "Salary cannot be negative"
            );
        }

        if (salary != null) {
            String normalizedCurrency = normalizeCurrency(salaryCurrency);

            if (normalizedCurrency == null ||
                    !normalizedCurrency.matches("[A-Z]{3}")) {
                throw new IllegalArgumentException(
                        "A valid three-letter currency is required when salary is provided"
                );
            }
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }

        return currency.trim().toUpperCase();
    }
}
