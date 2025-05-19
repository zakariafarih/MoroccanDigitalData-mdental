package org.mdental.patientcore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContactResponse {
    private UUID id;
    private UUID patientId;
    private String name;
    private String relationship;
    private String phone;
    private String email;
    private boolean primary;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long version;
}