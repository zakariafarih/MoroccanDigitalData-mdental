package org.mdental.patientcore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.patientcore.model.entity.AddressType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private UUID id;
    private UUID patientId;
    private AddressType type;
    private String street;
    private String city;
    private String state;
    private String zip;
    private String country;
    private BigDecimal lat;
    private BigDecimal lng;
    private boolean primary;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long version;
}