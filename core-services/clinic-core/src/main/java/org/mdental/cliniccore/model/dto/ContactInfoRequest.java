package org.mdental.cliniccore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.cliniccore.model.entity.ContactInfo;

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
            throw new IllegalArgumentException("Invalid email format");
        }
        if (type == ContactInfo.ContactType.PHONE && !value.matches("^\\+?[0-9\\-\\s()]+$")) {
            throw new IllegalArgumentException("Invalid phone format");
        }
        if (type == ContactInfo.ContactType.WEBSITE && !value.matches("^(http|https)://.*")) {
            throw new IllegalArgumentException("Website must start with http:// or https://");
        }
    }
}