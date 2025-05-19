package org.mdental.patientcore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.patientcore.model.entity.ConsentType;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponse {
    private UUID id;
    private UUID patientId;
    private ConsentType type;
    private boolean granted;
    private Instant grantedAt;
    private Instant expiresAt;
    private String grantedBy;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long version;
}