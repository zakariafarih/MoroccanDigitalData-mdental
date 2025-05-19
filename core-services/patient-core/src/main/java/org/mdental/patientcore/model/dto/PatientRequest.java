package org.mdental.patientcore.model.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.patientcore.model.entity.Gender;
import org.mdental.patientcore.model.entity.MaritalStatus;
import org.mdental.patientcore.model.entity.PatientStatus;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Size(max = 100, message = "Middle name cannot exceed 100 characters")
    private String middleName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private MaritalStatus maritalStatus;

    @Size(max = 50, message = "Preferred language cannot exceed 50 characters")
    private String preferredLanguage;

    @NotNull(message = "Status is required")
    private PatientStatus status;
}