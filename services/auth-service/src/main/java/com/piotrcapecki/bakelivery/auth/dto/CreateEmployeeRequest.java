package com.piotrcapecki.bakelivery.auth.dto;

import com.piotrcapecki.bakelivery.auth.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String temporaryPassword,
        @NotNull Role role
) {
    public CreateEmployeeRequest {
        if (role != null && (role == Role.SUPER_ADMIN || role == Role.CUSTOMER)) {
            throw new IllegalArgumentException("Cannot create employee with role " + role);
        }
    }
}
