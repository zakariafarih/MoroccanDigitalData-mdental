package org.mdental.patientcore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.patientcore.model.entity.IdentifierType;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentifierResponse {
    private UUID id;
    private UUID patientId;
    private IdentifierType type;
    private String value;
    private String issuer;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long version;
}