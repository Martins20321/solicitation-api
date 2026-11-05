package com.martinsdev.solicitation.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(@NotBlank(message = "Name is required") @Size(max = 250) String name,
                                 @NotBlank(message = "Email is required") @Email(message = "The email format is invalid") @Size(max = 80) String email,
                                 @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be as least 6 characters") String password) {
    public RegisterRequestDTO {
        if (name != null) {
            name = name.strip();
        }
        if (email != null) {
            email = email.strip().toLowerCase();
        }
    }
}
