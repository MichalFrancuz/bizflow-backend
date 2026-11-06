
package com.crudapp.fullstackbackend.controller.project;

import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.ProjectRevenue;
import com.crudapp.fullstackbackend.service.project.ProjectRevenueService;
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
class ProjectRevenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectRevenueService projectRevenueService;

    private ProjectRevenue createResponseRevenue() {
        ProjectRevenue revenue = new ProjectRevenue();

        ReflectionTestUtils.setField(revenue, "id", 40L);

        revenue.setCategory(ProjectRevenue.Category.CLIENT_PAYMENT);
        revenue.setDescription("Client payment");
        revenue.setAmount(new BigDecimal("5000.00"));
        revenue.setCurrency("PLN");
        revenue.setRevenueDate(LocalDate.of(2026, 11, 5));

        Project project = new Project();
        ReflectionTestUtils.setField(project, "id", 20L);

        revenue.setProject(project);

        return revenue;
    }

    @Test
    void shouldReturnProjectRevenues() throws Exception {
        when(projectRevenueService.getRevenues(10L, 20L))
                .thenReturn(List.of(createResponseRevenue()));

        mockMvc.perform(get(
                        "/api/organizations/10/projects/20/revenues"
                ).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(40))
                .andExpect(jsonPath("$[0].category").value("CLIENT_PAYMENT"))
                .andExpect(jsonPath("$[0].amount").value(5000.00))
                .andExpect(jsonPath("$[0].currency").value("PLN"))
                .andExpect(jsonPath("$[0].projectId").value(20));

        verify(projectRevenueService).getRevenues(10L, 20L);
    }

    @Test
    void shouldCreateProjectRevenue() throws Exception {
        when(projectRevenueService.createRevenue(
                eq(10L),
                eq(20L),
                any(ProjectRevenue.class)
        )).thenReturn(createResponseRevenue());

        mockMvc.perform(post(
                        "/api/organizations/10/projects/20/revenues"
                )
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "category": "CLIENT_PAYMENT",
                                    "description": "Client payment",
                                    "amount": 5000.00,
                                    "currency": "PLN",
                                    "revenueDate": "2026-11-05"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(40))
                .andExpect(jsonPath("$.category").value("CLIENT_PAYMENT"))
                .andExpect(jsonPath("$.description").value("Client payment"))
                .andExpect(jsonPath("$.amount").value(5000.00))
                .andExpect(jsonPath("$.currency").value("PLN"))
                .andExpect(jsonPath("$.projectId").value(20));

        verify(projectRevenueService).createRevenue(
                eq(10L),
                eq(20L),
                any(ProjectRevenue.class)
        );
    }

    @Test
    void shouldRejectInvalidProjectRevenueRequest() throws Exception {
        mockMvc.perform(post(
                        "/api/organizations/10/projects/20/revenues"
                )
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "category": "CLIENT_PAYMENT",
                                    "description": "",
                                    "amount": -100,
                                    "currency": "INVALID",
                                    "revenueDate": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(projectRevenueService);
    }

    @Test
    void shouldUpdateProjectRevenue() throws Exception {
        when(projectRevenueService.updateRevenue(
                eq(10L),
                eq(20L),
                eq(40L),
                any(ProjectRevenue.class)
        )).thenReturn(createResponseRevenue());

        mockMvc.perform(put(
                        "/api/organizations/10/projects/20/revenues/40"
                )
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "category": "MILESTONE_PAYMENT",
                                    "description": "Updated milestone payment",
                                    "amount": 7500.00,
                                    "currency": "PLN",
                                    "revenueDate": "2026-11-06"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(40))
                .andExpect(jsonPath("$.category").value("CLIENT_PAYMENT"))
                .andExpect(jsonPath("$.projectId").value(20));

        verify(projectRevenueService).updateRevenue(
                eq(10L),
                eq(20L),
                eq(40L),
                any(ProjectRevenue.class)
        );
    }

    @Test
    void shouldRejectInvalidProjectRevenueUpdate() throws Exception {
        mockMvc.perform(put(
                        "/api/organizations/10/projects/20/revenues/40"
                )
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "category": null,
                                    "description": "",
                                    "amount": 0,
                                    "currency": "PL",
                                    "revenueDate": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(projectRevenueService);
    }

    @Test
    void shouldDeleteProjectRevenue() throws Exception {
        mockMvc.perform(delete(
                        "/api/organizations/10/projects/20/revenues/40"
                ).with(jwt()))
                .andExpect(status().isNoContent());

        verify(projectRevenueService).deleteRevenue(10L, 20L, 40L);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get(
                        "/api/organizations/10/projects/20/revenues"
                ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(projectRevenueService);
    }
}
