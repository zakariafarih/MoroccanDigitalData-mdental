package org.mdental.patientcore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.patientcore.model.entity.ContactType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfoRequest {

    @NotNull(message = "Contact type is required")
    private ContactType type;

    @Size(max = 50, message = "Label cannot exceed 50 characters")
    private String label;

    @NotBlank(message = "Value is required")
    @Size(max = 100, message = "Value cannot exceed 100 characters")
    private String value;

    private boolean primary;
}