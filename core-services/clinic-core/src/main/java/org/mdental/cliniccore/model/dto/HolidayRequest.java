package org.mdental.cliniccore.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String description;

    @NotNull(message = "Recurring flag is required")
    private Boolean recurring;
}