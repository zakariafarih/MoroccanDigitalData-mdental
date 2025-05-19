package org.mdental.patientcore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.patientcore.model.entity.Gender;
import org.mdental.patientcore.model.entity.MaritalStatus;
import org.mdental.patientcore.model.entity.PatientStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {
    private UUID id;
    private UUID clinicId;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private MaritalStatus maritalStatus;
    private String preferredLanguage;
    private PatientStatus status;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long version;
}