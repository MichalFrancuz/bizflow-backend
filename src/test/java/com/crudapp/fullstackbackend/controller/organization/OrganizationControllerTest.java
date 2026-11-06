
package com.crudapp.fullstackbackend.controller.organization;

import com.crudapp.fullstackbackend.dto.organization.OrganizationResponse;
import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.service.organization.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("bizflow-test")
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    @Test
    void shouldRejectRequestWithoutAuthentication() throws Exception {

        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(organizationService);
    }

    @Test
    void shouldReturnCurrentUserOrganizations() throws Exception {

        when(organizationService.getCurrentUserOrganizations())
                .thenReturn(List.of(
                        new OrganizationResponse(
                                10L,
                                "BizFlow Ltd",
                                "1234567890",
                                OrganizationMember.Role.OWNER
                        )
                ));

        mockMvc.perform(get("/api/organizations")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("BizFlow Ltd"))
                .andExpect(jsonPath("$[0].taxId").value("1234567890"))
                .andExpect(jsonPath("$[0].role").value("OWNER"));

        verify(organizationService).getCurrentUserOrganizations();
    }

    @Test
    void shouldCreateOrganizationSuccessfully() throws Exception {

        when(organizationService.createOrganization(
                "BizFlow Ltd",
                "1234567890"
        )).thenReturn(10L);

        mockMvc.perform(post("/api/organizations")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "BizFlow Ltd",
                                    "taxId": "1234567890"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationId").value(10));

        verify(organizationService).createOrganization(
                "BizFlow Ltd",
                "1234567890"
        );
    }

    @Test
    void shouldRejectInvalidOrganizationName() throws Exception {

        mockMvc.perform(post("/api/organizations")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": " ",
                                    "taxId": "1234567890"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(organizationService);
    }
}
