package org.mdental.patientcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.PageResponse;
import org.mdental.patientcore.model.dto.AddressRequest;
import org.mdental.patientcore.model.dto.AddressResponse;
import org.mdental.patientcore.service.AddressService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinics/{clinicId}/patients/{patientId}/addresses")
@RequiredArgsConstructor
@Tag(name = "Patient Addresses", description = "APIs for managing patient addresses")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "List all addresses", description = "Returns all addresses for the specified patient")
    public ResponseEntity<ApiResponse<PageResponse<AddressResponse>>> getAllAddresses(
            @PathVariable UUID clinicId,
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        PageResponse<AddressResponse> addresses = addressService.getPatientAddresses(clinicId, patientId, pageable);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @GetMapping("/{addressId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "Get address by ID", description = "Returns the address with the specified ID")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Address ID", required = true) @PathVariable UUID addressId) {
        AddressResponse address = addressService.getPatientAddressById(clinicId, patientId, addressId);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Create a new address", description = "Creates a new address for the patient")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = addressService.createPatientAddress(clinicId, patientId, request);
        return new ResponseEntity<>(ApiResponse.success(address), HttpStatus.CREATED);
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Update an address", description = "Updates an existing address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Address ID", required = true) @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = addressService.updatePatientAddress(clinicId, patientId, addressId, request);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    @Operation(summary = "Delete an address", description = "Deletes an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Address ID", required = true) @PathVariable UUID addressId) {
        addressService.deletePatientAddress(clinicId, patientId, addressId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}