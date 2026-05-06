package com.piotrcapecki.bakelivery.catalog.controller;

import com.piotrcapecki.bakelivery.catalog.config.AppConfig;
import com.piotrcapecki.bakelivery.catalog.dto.MediaResponse;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.ProductMediaService;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(AppConfig.class)
class ProductMediaControllerTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @MockitoBean private ProductMediaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    private UsernamePasswordAuthenticationToken authToken(CatalogPrincipal p) {
        return new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_" + p.role())));
    }

    // 1. upload as BAKERY_ADMIN → 201
    @Test
    void upload_asBakeryAdmin_returns201() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.upload(eq(bakeryId), eq(productId), any(), isNull(), eq(false)))
                .thenReturn(new MediaResponse(mediaId, "http://minio/presigned", "image/jpeg", 1024, 0, false, Instant.now().plusSeconds(900)));

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[1024]);

        mockMvc.perform(multipart("/api/catalog/admin/products/" + productId + "/media")
                        .file(file)
                        .with(authentication(authToken(admin))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.url").value("http://minio/presigned"));
    }

    // 2. upload as DISPATCHER → 201
    @Test
    void upload_asDispatcher_returns201() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        CatalogPrincipal dispatcher = new CatalogPrincipal(UUID.randomUUID(), "disp@test.com", bakeryId, "DISPATCHER");

        when(service.upload(eq(bakeryId), eq(productId), any(), isNull(), eq(false)))
                .thenReturn(new MediaResponse(mediaId, "http://minio/presigned", "image/png", 2048, 0, true, Instant.now().plusSeconds(900)));

        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[2048]);

        mockMvc.perform(multipart("/api/catalog/admin/products/" + productId + "/media")
                        .file(file)
                        .with(authentication(authToken(dispatcher))))
                .andExpect(status().isCreated());
    }

    // 3. upload as CUSTOMER → 403
    @Test
    void upload_asCustomer_returns403() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal customer = new CatalogPrincipal(UUID.randomUUID(), "cust@test.com", bakeryId, "CUSTOMER");

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[1024]);

        mockMvc.perform(multipart("/api/catalog/admin/products/" + productId + "/media")
                        .file(file)
                        .with(authentication(authToken(customer))))
                .andExpect(status().isForbidden());
    }

    // 4. upload with unsupported content type → 400
    @Test
    void upload_unsupportedContentType_returns400() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.upload(any(), any(), any(), any(), anyBoolean()))
                .thenThrow(new IllegalArgumentException("Unsupported content type: application/pdf"));

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[1024]);

        mockMvc.perform(multipart("/api/catalog/admin/products/" + productId + "/media")
                        .file(file)
                        .with(authentication(authToken(admin))))
                .andExpect(status().isBadRequest());
    }

    // 5. upload file too large → 400
    @Test
    void upload_fileTooLarge_returns400() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.upload(any(), any(), any(), any(), anyBoolean()))
                .thenThrow(new IllegalArgumentException("File exceeds 10MB limit"));

        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[100]);

        mockMvc.perform(multipart("/api/catalog/admin/products/" + productId + "/media")
                        .file(file)
                        .with(authentication(authToken(admin))))
                .andExpect(status().isBadRequest());
    }

    // 6. list returns presigned URLs → 200
    @Test
    void list_returnsMediaList_returns200() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        Instant expiresAt = Instant.now().plusSeconds(900);
        when(service.list(eq(bakeryId), eq(productId))).thenReturn(List.of(
                new MediaResponse(UUID.randomUUID(), "http://minio/img1.jpg", "image/jpeg", 1024, 0, true, expiresAt),
                new MediaResponse(UUID.randomUUID(), "http://minio/img2.jpg", "image/jpeg", 2048, 1, false, expiresAt)
        ));

        mockMvc.perform(get("/api/catalog/admin/products/" + productId + "/media")
                        .with(authentication(authToken(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].primary").value(true))
                .andExpect(jsonPath("$[1].primary").value(false));
    }

    // 7. delete → 204
    @Test
    void delete_existingMedia_returns204() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        doNothing().when(service).delete(bakeryId, mediaId);

        mockMvc.perform(delete("/api/catalog/admin/media/" + mediaId)
                        .with(authentication(authToken(admin))))
                .andExpect(status().isNoContent());
    }

    // 8. delete from different bakery → 404
    @Test
    void delete_otherBakeryMedia_returns404() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        doThrow(new NotFoundException("Media not found")).when(service).delete(bakeryId, mediaId);

        mockMvc.perform(delete("/api/catalog/admin/media/" + mediaId)
                        .with(authentication(authToken(admin))))
                .andExpect(status().isNotFound());
    }

    // 9. unauthenticated upload → 403 (no AuthenticationEntryPoint configured, stateless)
    @Test
    void upload_unauthenticated_returns403() throws Exception {
        UUID productId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[100]);

        mockMvc.perform(multipart("/api/catalog/admin/products/" + productId + "/media")
                        .file(file))
                .andExpect(status().isForbidden());
    }
}
