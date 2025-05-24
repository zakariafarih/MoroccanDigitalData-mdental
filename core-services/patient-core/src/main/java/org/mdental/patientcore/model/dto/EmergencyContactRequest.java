package org.mdental.patientcore.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContactRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    private String name;

    @NotBlank(message = "Relationship is required")
    @Size(max = 100, message = "Relationship cannot exceed 100 characters")
    private String relationship;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+\\-\\s()]{8,20}$", message = "Invalid phone number format")
    private String phone;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    private boolean primary;
}