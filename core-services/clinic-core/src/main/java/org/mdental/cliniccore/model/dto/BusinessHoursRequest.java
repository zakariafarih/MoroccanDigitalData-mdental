package org.mdental.cliniccore.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessHoursRequest {

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Open time is required")
    private LocalTime openTime;

    @NotNull(message = "Close time is required")
    private LocalTime closeTime;

    @NotNull(message = "Active status is required")
    private Boolean active;

    // Custom validation to ensure open time is before close time
    public void validate() {
        if (openTime != null && closeTime != null && !openTime.isBefore(closeTime)) {
            throw new ValidationException("Open time must be before close time");
        }
    }

    public static class ValidationException extends BaseException {
        public ValidationException(String message) {
            super(message, ErrorCode.VALIDATION_ERROR);
        }
    }
}