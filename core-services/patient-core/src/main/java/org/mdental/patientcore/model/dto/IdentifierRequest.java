package org.mdental.patientcore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.patientcore.model.entity.IdentifierType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentifierRequest {

    @NotNull(message = "Identifier type is required")
    private IdentifierType type;

    @NotBlank(message = "Value is required")
    @Size(max = 100, message = "Value cannot exceed 100 characters")
    private String value;

    @Size(max = 100, message = "Issuer cannot exceed 100 characters")
    private String issuer;
}