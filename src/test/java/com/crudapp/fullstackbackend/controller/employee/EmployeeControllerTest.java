
package com.crudapp.fullstackbackend.controller.employee;

import com.crudapp.fullstackbackend.model.Employee;
import com.crudapp.fullstackbackend.service.employee.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("bizflow-test")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void shouldRejectRequestWithoutAuthentication() throws Exception {

        mockMvc.perform(get("/api/organizations/10/employees"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(employeeService);
    }

    @Test
    void shouldReturnActiveEmployees() throws Exception {

        Employee employee = createEmployee(100L);

        when(employeeService.getActiveEmployees(10L))
                .thenReturn(List.of(employee));

        mockMvc.perform(get("/api/organizations/10/employees")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].name").value("John"))
                .andExpect(jsonPath("$[0].familyname").value("Smith"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"))
                .andExpect(jsonPath("$[0].salary").value(7500.00))
                .andExpect(jsonPath("$[0].salaryCurrency").value("PLN"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(employeeService).getActiveEmployees(10L);
    }

    @Test
    void shouldReturnEmployeeDetails() throws Exception {

        Employee employee = createEmployee(100L);

        when(employeeService.getEmployee(100L, 10L))
                .thenReturn(employee);

        mockMvc.perform(get("/api/organizations/10/employees/100")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.familyname").value("Smith"));

        verify(employeeService).getEmployee(100L, 10L);
    }

    @Test
    void shouldCreateEmployeeSuccessfully() throws Exception {

        Employee employee = createEmployee(100L);

        when(employeeService.createEmployee(
                10L,
                "John",
                "Smith",
                "john@example.com",
                "123456789",
                new BigDecimal("7500.00"),
                "PLN"
        )).thenReturn(100L);

        when(employeeService.getEmployee(100L, 10L))
                .thenReturn(employee);

        mockMvc.perform(post("/api/organizations/10/employees")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "John",
                                    "familyname": "Smith",
                                    "email": "john@example.com",
                                    "phonenumber": "123456789",
                                    "salary": 7500.00,
                                    "salaryCurrency": "PLN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.salary").value(7500.00));

        verify(employeeService).createEmployee(
                10L,
                "John",
                "Smith",
                "john@example.com",
                "123456789",
                new BigDecimal("7500.00"),
                "PLN"
        );
    }

    @Test
    void shouldRejectInvalidEmployeeName() throws Exception {

        mockMvc.perform(post("/api/organizations/10/employees")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": " ",
                                    "familyname": "Smith"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(employeeService);
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {

        mockMvc.perform(post("/api/organizations/10/employees")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "John",
                                    "email": "invalid-email"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(employeeService);
    }

    @Test
    void shouldUpdateEmployeeSuccessfully() throws Exception {

        Employee employee = createEmployee(100L);

        when(employeeService.getEmployee(100L, 10L))
                .thenReturn(employee);

        mockMvc.perform(put("/api/organizations/10/employees/100")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "John Updated",
                                    "familyname": "Smith",
                                    "email": "john@example.com",
                                    "phonenumber": "123456789",
                                    "salary": 8500.00,
                                    "salaryCurrency": "EUR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));

        verify(employeeService).updateEmployee(
                100L,
                10L,
                "John Updated",
                "Smith",
                "john@example.com",
                "123456789",
                new BigDecimal("8500.00"),
                "EUR"
        );
    }

    @Test
    void shouldDeactivateEmployeeSuccessfully() throws Exception {

        mockMvc.perform(delete("/api/organizations/10/employees/100")
                        .with(jwt()))
                .andExpect(status().isNoContent());

        verify(employeeService).deactivateEmployee(100L, 10L);
    }

    private Employee createEmployee(Long id) {
        Employee employee = new Employee();

        try {
            var idField = Employee.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(employee, id);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }

        employee.setName("John");
        employee.setFamilyname("Smith");
        employee.setEmail("john@example.com");
        employee.setPhonenumber("123456789");
        employee.setSalary(new BigDecimal("7500.00"));
        employee.setSalaryCurrency("PLN");
        employee.setActive(true);

        return employee;
    }
}
