package com.piotrcapecki.bakelivery.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBakeryRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,80}$") String slug,
        @Email String contactEmail,
        String contactPhone,
        @NotBlank @Email String firstAdminEmail,
        @NotBlank @Size(min = 8) String firstAdminPassword
) {}
