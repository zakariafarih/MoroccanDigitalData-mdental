package org.mdental.cliniccore.controller;

import lombok.RequiredArgsConstructor;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.service.ClinicService;
import org.mdental.commons.model.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Development-only controller for clinic management.
 * Do not use in production.
 */
@RestController
@RequestMapping("/internal/dev/clinics")
@RequiredArgsConstructor
@Profile({"dev", "local"})
public class DevClinicController {

    private final ClinicService clinicService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Clinic> createClinic(@RequestBody CreateClinicRequest request) {
        // Create clinic directly using the ClinicService with the request DTO
        return ApiResponse.success(clinicService.createClinic(request));
    }

    @GetMapping
    public ApiResponse<List<Clinic>> getAllClinics() {
        return ApiResponse.success(clinicService.getAllClinics());
    }
}