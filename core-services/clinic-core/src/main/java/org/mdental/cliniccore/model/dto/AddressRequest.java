package org.mdental.cliniccore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.cliniccore.model.entity.Address;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotNull(message = "Address type is required")
    private Address.AddressType type;

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "ZIP code is required")
    private String zip;

    @NotBlank(message = "Country is required")
    private String country;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @NotNull(message = "Primary flag is required")
    private Boolean primary;
}