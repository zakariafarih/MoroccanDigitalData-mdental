package org.mdental.patientcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.PageResponse;
import org.mdental.patientcore.model.dto.PatientRequest;
import org.mdental.patientcore.model.dto.PatientResponse;
import org.mdental.patientcore.model.dto.PatientSummaryResponse;
import org.mdental.patientcore.model.entity.PatientStatus;
import org.mdental.patientcore.service.PatientService;
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
@RequestMapping("/api/clinics/{clinicId}/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Management", description = "APIs for managing patient data")
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    public ResponseEntity<ApiResponse<PageResponse<PatientResponse>>> getAllPatients(
            @PathVariable UUID clinicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        PageResponse<PatientResponse> patients = patientService.listAllPatients(clinicId, pageable);
        return ResponseEntity.ok(ApiResponse.success(patients));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "List patients by status", description = "Returns patients with the specified status")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getPatientsByStatus(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient status", required = true) @PathVariable PatientStatus status) {
        List<PatientResponse> patients = patientService.listPatientsByStatus(clinicId, status);
        return ResponseEntity.ok(ApiResponse.success(patients));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    public ResponseEntity<ApiResponse<PageResponse<PatientSummaryResponse>>> getPatientSummaries(
            @PathVariable UUID clinicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        PageResponse<PatientSummaryResponse> summaries = patientService.getPatientSummaries(clinicId, pageable);
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "Search patients", description = "Search patients by name")
    public ResponseEntity<ApiResponse<PageResponse<PatientResponse>>> searchPatients(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Search term") @RequestParam String term,
            Pageable pageable) {
        PageResponse<PatientResponse> patients = patientService.searchPatients(clinicId, term, pageable);
        return ResponseEntity.ok(ApiResponse.success(patients));
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "Get patient by ID", description = "Returns the patient with the specified ID")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId) {
        PatientResponse patient = patientService.getPatientById(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Create a new patient", description = "Creates a new patient record")
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Valid @RequestBody PatientRequest request) {
        PatientResponse patient = patientService.createPatient(clinicId, request);
        return new ResponseEntity<>(ApiResponse.success(patient), HttpStatus.CREATED);
    }

    @PutMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Update a patient", description = "Updates an existing patient record")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Valid @RequestBody PatientRequest request) {
        PatientResponse patient = patientService.updatePatient(clinicId, patientId, request);
        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    @DeleteMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    @Operation(summary = "Delete a patient", description = "Soft deletes a patient record")
    public ResponseEntity<ApiResponse<Void>> deletePatient(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId) {
        patientService.deletePatient(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{patientId}/purge")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Purge a patient", description = "Permanently deletes a patient record (for GDPR)")
    public ResponseEntity<ApiResponse<Void>> purgePatient(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId) {
        patientService.purgePatient(patientId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}