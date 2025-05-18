package org.mdental.cliniccore.service.util;

import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.dto.UpdateClinicRequest;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

public class ClinicValidationUtils {

    /**
     * Validates clinic slug format
     */
    public static void validateSlug(String slug) {
        if (slug == null || !slug.matches("^[a-z0-9-]+$")) {
            throw new ValidationException("Clinic slug must contain only lowercase letters, numbers, and hyphens");
        }

        if (slug.length() < 2 || slug.length() > 50) {
            throw new ValidationException("Clinic slug must be between 2 and 50 characters");
        }
    }

    /**
     * Validates realm name format
     */
    public static void validateRealm(String realm) {
        if (realm == null || !realm.matches("^[a-z0-9-]+$")) {
            throw new ValidationException("Realm must contain only lowercase letters, numbers, and hyphens");
        }

        if (realm.length() < 2 || realm.length() > 50) {
            throw new ValidationException("Realm must be between 2 and 50 characters");
        }
    }

    /**
     * Validates color format (hex code)
     */
    public static void validateColor(String color) {
        if (color != null && !color.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new ValidationException("Color must be a valid hex code (e.g. #FF5500)");
        }
    }

    /**
     * Full validation for create request
     */
    public static void validateCreateRequest(CreateClinicRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException("Clinic name is required");
        }

        if (request.getName().length() < 2 || request.getName().length() > 100) {
            throw new ValidationException("Name must be between 2 and 100 characters");
        }

        validateSlug(request.getSlug());
        validateRealm(request.getRealm());
        validateColor(request.getPrimaryColor());
        validateColor(request.getSecondaryColor());
    }

    /**
     * Full validation for update request
     */
    public static void validateUpdateRequest(UpdateClinicRequest request) {
        if (request.getName() != null && (request.getName().length() < 2 || request.getName().length() > 100)) {
            throw new ValidationException("Name must be between 2 and 100 characters");
        }

        if (request.getSlug() != null) {
            validateSlug(request.getSlug());
        }

        validateColor(request.getPrimaryColor());
        validateColor(request.getSecondaryColor());
    }

    public static class ValidationException extends BaseException {
        public ValidationException(String message) {
            super(message, ErrorCode.VALIDATION_ERROR);
        }
    }
}