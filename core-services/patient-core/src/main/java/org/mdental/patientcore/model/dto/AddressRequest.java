package org.mdental.patientcore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.patientcore.model.entity.AddressType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotNull(message = "Address type is required")
    private AddressType type;

    @NotBlank(message = "Street is required")
    @Size(max = 255, message = "Street cannot exceed 255 characters")
    private String street;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @NotBlank(message = "ZIP code is required")
    @Size(max = 20, message = "ZIP code cannot exceed 20 characters")
    private String zip;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;

    private BigDecimal lat;
    private BigDecimal lng;
    private boolean primary;
}