
package com.crudapp.fullstackbackend.controller.project;

import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.ProjectCost;
import com.crudapp.fullstackbackend.service.project.ProjectCostService;
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
class ProjectCostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectCostService projectCostService;

    private ProjectCost createResponseCost() {
        ProjectCost cost = new ProjectCost();

        ReflectionTestUtils.setField(cost, "id", 30L);

        cost.setCategory(ProjectCost.Category.LICENSES);
        cost.setDescription("Software license");
        cost.setAmount(new BigDecimal("299.99"));
        cost.setCurrency("PLN");
        cost.setCostDate(LocalDate.of(2026, 11, 5));

        Project project = new Project();
        ReflectionTestUtils.setField(project, "id", 20L);

        cost.setProject(project);

        return cost;
    }

    @Test
    void shouldReturnProjectCosts() throws Exception {
        when(projectCostService.getCosts(10L, 20L))
                .thenReturn(List.of(createResponseCost()));

        mockMvc.perform(get(
                        "/api/organizations/10/projects/20/costs"
                ).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(30))
                .andExpect(jsonPath("$[0].category").value("LICENSES"))
                .andExpect(jsonPath("$[0].amount").value(299.99))
                .andExpect(jsonPath("$[0].currency").value("PLN"))
                .andExpect(jsonPath("$[0].projectId").value(20));

        verify(projectCostService).getCosts(10L, 20L);
    }

    @Test
    void shouldCreateProjectCost() throws Exception {
        when(projectCostService.createCost(
                eq(10L),
                eq(20L),
                any(ProjectCost.class)
        )).thenReturn(createResponseCost());

        mockMvc.perform(post(
                        "/api/organizations/10/projects/20/costs"
                )
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "category": "LICENSES",
                                    "description": "Software license",
                                    "amount": 299.99,
                                    "currency": "PLN",
                                    "costDate": "2026-11-05"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.category").value("LICENSES"))
                .andExpect(jsonPath("$.description").value("Software license"))
                .andExpect(jsonPath("$.amount").value(299.99))
                .andExpect(jsonPath("$.currency").value("PLN"))
                .andExpect(jsonPath("$.projectId").value(20));

        verify(projectCostService).createCost(
                eq(10L),
                eq(20L),
                any(ProjectCost.class)
        );
    }

    @Test
    void shouldRejectInvalidProjectCostRequest() throws Exception {
        mockMvc.perform(post(
                        "/api/organizations/10/projects/20/costs"
                )
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "category": "LICENSES",
                                    "description": "",
                                    "amount": -100,
                                    "currency": "INVALID",
                                    "costDate": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(projectCostService);
    }

    @Test
    void shouldUpdateProjectCost() throws Exception {
        when(projectCostService.updateCost(
                eq(10L),
                eq(20L),
                eq(30L),
                any(ProjectCost.class)
        )).thenReturn(createResponseCost());

        mockMvc.perform(put(
                        "/api/organizations/10/projects/20/costs/30"
                )
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "category": "LICENSES",
                                    "description": "Updated software license",
                                    "amount": 399.99,
                                    "currency": "PLN",
                                    "costDate": "2026-11-06"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.category").value("LICENSES"))
                .andExpect(jsonPath("$.projectId").value(20));

        verify(projectCostService).updateCost(
                eq(10L),
                eq(20L),
                eq(30L),
                any(ProjectCost.class)
        );
    }

    @Test
    void shouldRejectInvalidProjectCostUpdate() throws Exception {
        mockMvc.perform(put(
                        "/api/organizations/10/projects/20/costs/30"
                )
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "category": null,
                                    "description": "",
                                    "amount": 0,
                                    "currency": "PL",
                                    "costDate": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(projectCostService);
    }

    @Test
    void shouldDeleteProjectCost() throws Exception {
        mockMvc.perform(delete(
                        "/api/organizations/10/projects/20/costs/30"
                ).with(jwt()))
                .andExpect(status().isNoContent());

        verify(projectCostService).deleteCost(10L, 20L, 30L);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get(
                        "/api/organizations/10/projects/20/costs"
                ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(projectCostService);
    }
}
