
package com.crudapp.fullstackbackend.controller.project;

import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.Project.Status;
import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.service.project.ProjectService;
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
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    private Project createResponseProject() {
        Project project = new Project();

        ReflectionTestUtils.setField(project, "id", 1L);

        project.setName("Website Development");
        project.setDescription("Corporate website");
        project.setStatus(Status.PLANNED);
        project.setBudget(new BigDecimal("25000.00"));
        project.setCurrency("PLN");
        project.setStartDate(LocalDate.of(2026, 12, 1));
        project.setEndDate(LocalDate.of(2027, 4, 30));

        Organization organization = new Organization();
        ReflectionTestUtils.setField(organization, "id", 10L);
        organization.setName("Test Organization");

        project.setOrganization(organization);

        return project;
    }

    @Test
    void shouldReturnOrganizationProjects() throws Exception {
        when(projectService.getProjects(10L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/organizations/10/projects")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(projectService).getProjects(10L);
    }

    @Test
    void shouldReturnProjectDetails() throws Exception {
        when(projectService.getProject(10L, 1L))
                .thenReturn(createResponseProject());

        mockMvc.perform(get("/api/organizations/10/projects/1")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Website Development"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.organizationId").value(10));

        verify(projectService).getProject(10L, 1L);
    }

    @Test
    void shouldCreateProject() throws Exception {
        when(projectService.createProject(
                eq(10L),
                anyString(),
                any(),
                any(),
                anyString(),
                any(),
                any()
        )).thenReturn(createResponseProject());

        mockMvc.perform(post("/api/organizations/10/projects")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Website Development",
                                    "description": "Corporate website",
                                    "budget": 25000.00,
                                    "currency": "PLN",
                                    "startDate": "2026-12-01",
                                    "endDate": "2027-04-30"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("Website Development"))
                .andExpect(jsonPath("$.budget").value(25000.00))
                .andExpect(jsonPath("$.currency").value("PLN"));

        verify(projectService).createProject(
                eq(10L),
                anyString(),
                any(),
                any(),
                anyString(),
                any(),
                any()
        );
    }

    @Test
    void shouldRejectInvalidProjectRequest() throws Exception {
        mockMvc.perform(post("/api/organizations/10/projects")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "",
                                    "budget": -100,
                                    "currency": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(projectService);
    }

    @Test
    void shouldUpdateProject() throws Exception {
        when(projectService.updateProject(
                eq(10L),
                eq(1L),
                anyString(),
                any(),
                any(),
                anyString(),
                any(),
                any()
        )).thenReturn(createResponseProject());

        mockMvc.perform(put("/api/organizations/10/projects/1")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Website Development",
                                    "description": "Updated description",
                                    "budget": 30000.00,
                                    "currency": "PLN",
                                    "startDate": "2026-12-01",
                                    "endDate": "2027-04-30"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(projectService).updateProject(
                eq(10L),
                eq(1L),
                anyString(),
                any(),
                any(),
                anyString(),
                any(),
                any()
        );
    }

    @Test
    void shouldUpdateProjectStatus() throws Exception {
        when(projectService.updateProjectStatus(
                10L,
                1L,
                Status.ACTIVE
        )).thenAnswer(invocation -> {
            Project project = createResponseProject();
            project.setStatus(Status.ACTIVE);
            return project;
        });

        mockMvc.perform(patch("/api/organizations/10/projects/1/status")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(projectService).updateProjectStatus(
                10L,
                1L,
                Status.ACTIVE
        );
    }

    @Test
    void shouldDeleteProject() throws Exception {
        mockMvc.perform(delete("/api/organizations/10/projects/1")
                        .with(jwt()))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(10L, 1L);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/organizations/10/projects"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(projectService);
    }
}
