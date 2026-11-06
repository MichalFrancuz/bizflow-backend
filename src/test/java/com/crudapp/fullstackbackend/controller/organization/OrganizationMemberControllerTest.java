package com.crudapp.fullstackbackend.controller.organization;

import com.crudapp.fullstackbackend.dto.organization.member.AddOrganizationMemberRequest;
import com.crudapp.fullstackbackend.dto.organization.member.OrganizationMemberResponse;
import com.crudapp.fullstackbackend.dto.organization.member.UpdateOrganizationMemberRoleRequest;
import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import com.crudapp.fullstackbackend.service.organization.OrganizationMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("bizflow-test")
class OrganizationMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationMemberService organizationMemberService;

    private OrganizationMemberResponse createResponse() {
        return new OrganizationMemberResponse(
                100L,
                2L,
                "employee@example.com",
                Role.EMPLOYEE,
                LocalDateTime.of(2026, 11, 5, 12, 0)
        );
    }

    @Test
    void shouldReturnOrganizationMembers() throws Exception {
        when(organizationMemberService.getOrganizationMembers(10L))
                .thenReturn(List.of(createResponse()));

        mockMvc.perform(get("/api/organizations/10/members")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].email")
                        .value("employee@example.com"))
                .andExpect(jsonPath("$[0].role").value("EMPLOYEE"));

        verify(organizationMemberService)
                .getOrganizationMembers(10L);
    }

    @Test
    void shouldAddOrganizationMember() throws Exception {
        when(organizationMemberService.addMember(
                eq(10L),
                any(AddOrganizationMemberRequest.class)
        )).thenReturn(createResponse());

        mockMvc.perform(post("/api/organizations/10/members")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "employee@example.com",
                                    "role": "EMPLOYEE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email")
                        .value("employee@example.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        verify(organizationMemberService)
                .addMember(eq(10L), any(AddOrganizationMemberRequest.class));
    }

    @Test
    void shouldRejectInvalidMemberRequest() throws Exception {
        mockMvc.perform(post("/api/organizations/10/members")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "role": "EMPLOYEE"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(organizationMemberService);
    }

    @Test
    void shouldUpdateMemberRole() throws Exception {
        when(organizationMemberService.updateMemberRole(
                eq(10L),
                eq(100L),
                any(UpdateOrganizationMemberRoleRequest.class)
        )).thenReturn(createResponse());

        mockMvc.perform(put("/api/organizations/10/members/100")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "role": "MANAGER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("employee@example.com"));

        verify(organizationMemberService).updateMemberRole(
                eq(10L),
                eq(100L),
                any(UpdateOrganizationMemberRoleRequest.class)
        );
    }

    @Test
    void shouldRemoveMember() throws Exception {
        mockMvc.perform(delete("/api/organizations/10/members/100")
                        .with(jwt()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(organizationMemberService)
                .removeMember(10L, 100L);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/organizations/10/members"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(organizationMemberService);
    }
}
