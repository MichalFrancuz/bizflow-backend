
package com.crudapp.fullstackbackend.controller.project;

import com.crudapp.fullstackbackend.model.Employee;
import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.TimeEntry;
import com.crudapp.fullstackbackend.service.project.TimeEntryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("bizflow-test")
class TimeEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeEntryService timeEntryService;

    private TimeEntry createResponseTimeEntry() {
        TimeEntry entry = new TimeEntry();

        ReflectionTestUtils.setField(entry, "id", 50L);

        Project project = new Project();
        ReflectionTestUtils.setField(project, "id", 20L);

        Employee employee = new Employee();
        ReflectionTestUtils.setField(employee, "id", 30L);
        employee.setName("John");
        employee.setFamilyname("Smith");

        entry.setProject(project);
        entry.setEmployee(employee);
        entry.setDescription("Backend development");
        entry.setWorkDate(LocalDate.of(2026, 11, 5));
        entry.setHours(new BigDecimal("8.00"));
        entry.setHourlyRate(new BigDecimal("100.00"));
        entry.setCurrency("PLN");

        return entry;
    }

    @Test
    void shouldReturnProjectTimeEntries() throws Exception {
        when(timeEntryService.getTimeEntries(10L, 20L))
                .thenReturn(List.of(createResponseTimeEntry()));

        mockMvc.perform(get(
                        "/api/organizations/10/projects/20/time-entries"
                ).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[0].projectId").value(20))
                .andExpect(jsonPath("$[0].employeeId").value(30))
                .andExpect(jsonPath("$[0].employeeName").value("John"))
                .andExpect(jsonPath("$[0].employeeFamilyName").value("Smith"))
                .andExpect(jsonPath("$[0].hours").value(8.00))
                .andExpect(jsonPath("$[0].hourlyRate").value(100.00))
                .andExpect(jsonPath("$[0].totalCost").value(800.00));

        verify(timeEntryService).getTimeEntries(10L, 20L);
    }

    @Test
    void shouldReturnTimeEntriesByDateRange() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);

        when(timeEntryService.getTimeEntriesByDateRange(
                10L, 20L, startDate, endDate
        )).thenReturn(List.of(createResponseTimeEntry()));

        mockMvc.perform(get(
                        "/api/organizations/10/projects/20/time-entries/date-range"
                )
                        .with(jwt())
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(50));

        verify(timeEntryService).getTimeEntriesByDateRange(
                10L, 20L, startDate, endDate
        );
    }

    @Test
    void shouldReturnEmployeeTimeEntries() throws Exception {
        when(timeEntryService.getEmployeeTimeEntries(10L, 30L))
                .thenReturn(List.of(createResponseTimeEntry()));

        mockMvc.perform(get(
                        "/api/organizations/10/employees/30/time-entries"
                ).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employeeId").value(30))
                .andExpect(jsonPath("$[0].employeeName").value("John"));

        verify(timeEntryService).getEmployeeTimeEntries(10L, 30L);
    }

    @Test
    void shouldCreateTimeEntry() throws Exception {
        when(timeEntryService.createTimeEntry(
                eq(10L),
                eq(20L),
                eq(30L),
                any(TimeEntry.class)
        )).thenReturn(createResponseTimeEntry());

        mockMvc.perform(post(
                        "/api/organizations/10/projects/20/time-entries"
                )
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "employeeId": 30,
                                    "description": "Backend development",
                                    "workDate": "2026-11-05",
                                    "hours": 8.00,
                                    "hourlyRate": 100.00,
                                    "currency": "PLN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.projectId").value(20))
                .andExpect(jsonPath("$.employeeId").value(30))
                .andExpect(jsonPath("$.description").value("Backend development"))
                .andExpect(jsonPath("$.totalCost").value(800.00));

        verify(timeEntryService).createTimeEntry(
                eq(10L),
                eq(20L),
                eq(30L),
                any(TimeEntry.class)
        );
    }

    @Test
    void shouldRejectInvalidTimeEntryRequest() throws Exception {
        mockMvc.perform(post(
                        "/api/organizations/10/projects/20/time-entries"
                )
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "employeeId": null,
                                    "description": "",
                                    "workDate": null,
                                    "hours": 25.00,
                                    "hourlyRate": -100.00,
                                    "currency": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(timeEntryService);
    }

    @Test
    void shouldUpdateTimeEntry() throws Exception {
        when(timeEntryService.updateTimeEntry(
                eq(10L),
                eq(20L),
                eq(50L),
                eq(30L),
                any(TimeEntry.class)
        )).thenReturn(createResponseTimeEntry());

        mockMvc.perform(put(
                        "/api/organizations/10/projects/20/time-entries/50"
                )
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "employeeId": 30,
                                    "description": "Updated backend development",
                                    "workDate": "2026-11-06",
                                    "hours": 6.00,
                                    "hourlyRate": 120.00,
                                    "currency": "PLN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.projectId").value(20));

        verify(timeEntryService).updateTimeEntry(
                eq(10L),
                eq(20L),
                eq(50L),
                eq(30L),
                any(TimeEntry.class)
        );
    }

    @Test
    void shouldDeleteTimeEntry() throws Exception {
        mockMvc.perform(delete(
                        "/api/organizations/10/projects/20/time-entries/50"
                ).with(jwt()))
                .andExpect(status().isNoContent());

        verify(timeEntryService).deleteTimeEntry(10L, 20L, 50L);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get(
                        "/api/organizations/10/projects/20/time-entries"
                ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(timeEntryService);
    }
}
