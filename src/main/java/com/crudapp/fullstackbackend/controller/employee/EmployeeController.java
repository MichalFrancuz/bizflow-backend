
package com.crudapp.fullstackbackend.controller.employee;

import com.crudapp.fullstackbackend.dto.employee.CreateEmployeeRequest;
import com.crudapp.fullstackbackend.dto.employee.EmployeeResponse;
import com.crudapp.fullstackbackend.model.Employee;
import com.crudapp.fullstackbackend.service.employee.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse createEmployee(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        Long employeeId = employeeService.createEmployee(
                organizationId,
                request.name(),
                request.familyname(),
                request.email(),
                request.phonenumber(),
                request.salary(),
                request.salaryCurrency()
        );

        Employee employee = employeeService.getEmployee(
                employeeId,
                organizationId
        );

        return toResponse(employee);
    }

    @GetMapping
    public List<EmployeeResponse> getActiveEmployees(
            @PathVariable Long organizationId
    ) {
        return employeeService.getActiveEmployees(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{employeeId}")
    public EmployeeResponse getEmployee(
            @PathVariable Long organizationId,
            @PathVariable Long employeeId
    ) {
        Employee employee = employeeService.getEmployee(
                employeeId,
                organizationId
        );

        return toResponse(employee);
    }

    @PutMapping("/{employeeId}")
    public EmployeeResponse updateEmployee(
            @PathVariable Long organizationId,
            @PathVariable Long employeeId,
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        employeeService.updateEmployee(
                employeeId,
                organizationId,
                request.name(),
                request.familyname(),
                request.email(),
                request.phonenumber(),
                request.salary(),
                request.salaryCurrency()
        );

        Employee employee = employeeService.getEmployee(
                employeeId,
                organizationId
        );

        return toResponse(employee);
    }

    @DeleteMapping("/{employeeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateEmployee(
            @PathVariable Long organizationId,
            @PathVariable Long employeeId
    ) {
        employeeService.deactivateEmployee(
                employeeId,
                organizationId
        );
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getFamilyname(),
                employee.getEmail(),
                employee.getPhonenumber(),
                employee.getSalary(),
                employee.getSalaryCurrency(),
                employee.isActive(),
                employee.getCreatedAt()
        );
    }
}
