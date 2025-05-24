package org.mdental.cliniccore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.mapper.AddressMapper;
import org.mdental.cliniccore.model.dto.AddressRequest;
import org.mdental.cliniccore.model.dto.AddressResponse;
import org.mdental.cliniccore.model.entity.Address;
import org.mdental.cliniccore.service.AddressService;
import org.mdental.commons.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clinics/{clinicId}/addresses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clinic Addresses", description = "APIs for managing clinic addresses")
public class AddressController {

    private final AddressService addressService;
    private final AddressMapper addressMapper;
    private final HttpServletRequest request;

    @GetMapping
    @Operation(summary = "Get all addresses for a clinic", description = "Returns all addresses for the specified clinic")
    public ApiResponse<List<AddressResponse>> getAllAddresses(@PathVariable UUID clinicId) {
        log.info("REST request to get all addresses for clinic: {}", clinicId);
        verifyClinicAccess(clinicId);
        List<Address> addresses = addressService.getAddressByClinicId(clinicId);
        List<AddressResponse> response = addresses.stream()
                .map(addressMapper::toDto)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by ID", description = "Returns the specified address")
    public ApiResponse<AddressResponse> getAddress(
            @PathVariable UUID clinicId,
            @PathVariable UUID id) {
        log.info("REST request to get address: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);
        Address address = addressService.getAddressById(id);

        // Double check that the address belongs to the specified clinic
        if (!address.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Address does not belong to the specified clinic");
        }

        return ApiResponse.success(addressMapper.toDto(address));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create address", description = "Creates a new address for the specified clinic")
    public ApiResponse<AddressResponse> createAddress(
            @PathVariable UUID clinicId,
            @Valid @RequestBody AddressRequest request) {
        log.info("REST request to create address for clinic: {}", clinicId);
        verifyClinicAccess(clinicId);
        Address address = addressService.createAddress(clinicId, request);
        return ApiResponse.success(addressMapper.toDto(address));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update address", description = "Updates an existing address")
    public ApiResponse<AddressResponse> updateAddress(
            @PathVariable UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        log.info("REST request to update address: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);

        // Verify the address belongs to the specified clinic before updating
        Address existing = addressService.getAddressById(id);
        if (!existing.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Address does not belong to the specified clinic");
        }

        Address address = addressService.updateAddress(id, request);
        return ApiResponse.success(addressMapper.toDto(address));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete address", description = "Deletes the specified address")
    public ApiResponse<Void> deleteAddress(
            @PathVariable UUID clinicId,
            @PathVariable UUID id) {
        log.info("REST request to delete address: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);

        // Verify the address belongs to the specified clinic before deleting
        Address existing = addressService.getAddressById(id);
        if (!existing.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Address does not belong to the specified clinic");
        }

        addressService.deleteAddress(id);
        return ApiResponse.success(null);
    }

    /**
     * Verifies the current user has access to the specified clinic
     */
    private void verifyClinicAccess(UUID clinicId) {
        // Super admin has access to all clinics
        if (request.isUserInRole("SUPER_ADMIN")) {
            return;
        }

        // Check if clinic ID in path matches the user's clinic ID from header
        String userClinicId = request.getHeader("X-User-ClinicId");
        if (userClinicId == null || !userClinicId.equals(clinicId.toString())) {
            throw new ClinicController.AccessDeniedException("You don't have access to this clinic");
        }
    }
}