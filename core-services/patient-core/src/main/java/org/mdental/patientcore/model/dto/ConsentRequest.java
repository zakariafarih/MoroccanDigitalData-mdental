package org.mdental.patientcore.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.patientcore.model.entity.ConsentType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequest {

    @NotNull(message = "Consent type is required")
    private ConsentType type;

    @NotNull(message = "Granted status is required")
    private Boolean granted;

    private Instant grantedAt;
    private Instant expiresAt;
    private String grantedBy;
}