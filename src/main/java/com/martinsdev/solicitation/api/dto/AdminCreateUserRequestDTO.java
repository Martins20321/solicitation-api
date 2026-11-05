package com.martinsdev.solicitation.api.dto;

import com.martinsdev.solicitation.api.model.enums.RoleUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AdminCreateUserRequestDTO(@NotBlank(message = "Name is required") @Size(max = 250) String name,
                                        @NotBlank(message = "Email is required") @Email(message = "The email format is invalid") @Size(max = 80) String email,
                                        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be as least 6 characters") String password,
                                        @NotNull RoleUser role,
                                        Set<String> states) {
    public AdminCreateUserRequestDTO {
        if (name != null) {
            name = name.strip();
        }
        if (email != null) {
            email = email.strip().toLowerCase();
        }
    }
}
