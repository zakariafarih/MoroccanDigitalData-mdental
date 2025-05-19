package org.mdental.patientcore.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalPreferencesRequest {

    @NotEmpty(message = "Preferences cannot be empty")
    private Map<String, String> preferences;
}