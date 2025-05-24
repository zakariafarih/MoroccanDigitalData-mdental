package org.mdental.cliniccore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.cliniccore.model.entity.ContactInfo;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfoRequest {

    @NotNull(message = "Contact type is required")
    private ContactInfo.ContactType type;

    private String label;

    @NotBlank(message = "Value is required")
    private String value;

    @NotNull(message = "Primary flag is required")
    private Boolean primary;

    // Custom validation logic based on type
    public void validate() {
        if (type == ContactInfo.ContactType.EMAIL && !value.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new ValidationException("Invalid email format");
        }
        if (type == ContactInfo.ContactType.PHONE && !value.matches("^\\+?[0-9\\-\\s()]+$")) {
            throw new ValidationException("Invalid phone format");
        }
        if (type == ContactInfo.ContactType.WEBSITE && !value.matches("^(http|https)://.*")) {
            throw new ValidationException("Website must start with http:// or https://");
        }
    }

    public static class ValidationException extends BaseException {
        public ValidationException(String message) {
            super(message, ErrorCode.VALIDATION_ERROR);
        }
    }
}