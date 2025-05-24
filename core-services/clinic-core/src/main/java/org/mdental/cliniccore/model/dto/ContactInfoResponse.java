package org.mdental.cliniccore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.cliniccore.model.entity.ContactInfo;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfoResponse {

    private UUID id;
    private ContactInfo.ContactType type;
    private String label;
    private String value;
    private Boolean primary;

    // Audit fields
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
}