package com.piotrcapecki.bakelivery.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.auth.config.AppConfig;
import com.piotrcapecki.bakelivery.auth.dto.CreateEmployeeRequest;
import com.piotrcapecki.bakelivery.auth.model.Bakery;
import com.piotrcapecki.bakelivery.auth.model.Role;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.service.AuthService;
import com.piotrcapecki.bakelivery.auth.service.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AppConfig.class)
class UserManagementControllerTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuthService authService;
    @MockitoBean private UserManagementService userManagementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void createEmployee_rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("employee@test.com", "temporary123", Role.DRIVER)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));

        verify(userManagementService, never()).createEmployee(eq(null), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void createEmployee_rejectsDispatcherRole() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("employee@test.com", "temporary123", Role.DRIVER)))
                .andExpect(status().isForbidden());

        verify(userManagementService, never()).createEmployee(eq(null), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createEmployee_allowsBakeryAdminWithBakery() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();
        Bakery bakery = Bakery.builder()
                .id(bakeryId)
                .name("Tenant Bakery")
                .slug("tenant-bakery")
                .build();
        User actor = User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .passwordHash("hashedPw")
                .role(Role.BAKERY_ADMIN)
                .bakery(bakery)
                .build();
        User created = User.builder()
                .id(createdId)
                .email("driver@test.com")
                .passwordHash("encoded-temporary")
                .role(Role.DRIVER)
                .bakery(bakery)
                .build();
        when(userManagementService.createEmployee(eq(bakeryId), org.mockito.ArgumentMatchers.any(CreateEmployeeRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/admin/users")
                        .with(authentication(authToken(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("driver@test.com", "temporary123", Role.DRIVER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId.toString()))
                .andExpect(jsonPath("$.email").value("driver@test.com"))
                .andExpect(jsonPath("$.role").value("DRIVER"));

        ArgumentCaptor<CreateEmployeeRequest> requestCaptor = ArgumentCaptor.forClass(CreateEmployeeRequest.class);
        verify(userManagementService).createEmployee(eq(bakeryId), requestCaptor.capture());
        assertThat(requestCaptor.getValue().email()).isEqualTo("driver@test.com");
        assertThat(requestCaptor.getValue().role()).isEqualTo(Role.DRIVER);
    }

    @Test
    void createEmployee_rejectsActorWithoutBakery() throws Exception {
        User actor = User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .passwordHash("hashedPw")
                .role(Role.BAKERY_ADMIN)
                .build();

        mockMvc.perform(post("/api/admin/users")
                        .with(authentication(authToken(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("driver@test.com", "temporary123", Role.DRIVER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Cross-bakery operations not allowed via this endpoint"));

        verify(userManagementService, never()).createEmployee(eq(null), org.mockito.ArgumentMatchers.any());
    }

    private UsernamePasswordAuthenticationToken authToken(User actor) {
        return new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities());
    }

    private String employeeJson(String email, String temporaryPassword, Role role) throws Exception {
        return objectMapper.writeValueAsString(new CreateEmployeeRequest(email, temporaryPassword, role));
    }
}
