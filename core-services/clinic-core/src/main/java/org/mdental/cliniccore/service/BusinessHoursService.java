package org.mdental.cliniccore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.event.BusinessHoursEvent;
import org.mdental.cliniccore.mapper.BusinessHoursMapper;
import org.mdental.cliniccore.model.dto.BusinessHoursRequest;
import org.mdental.cliniccore.model.entity.BusinessHours;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.repository.BusinessHoursRepository;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessHoursService {

    private final BusinessHoursRepository businessHoursRepository;
    private final ClinicService clinicService;
    private final BusinessHoursMapper businessHoursMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<BusinessHours> getBusinessHoursByClinicId(UUID clinicId) {
        log.debug("Fetching business hours for clinic ID: {}", clinicId);
        // Verify clinic exists
        clinicService.getClinicById(clinicId);
        return businessHoursRepository.findByClinicId(clinicId);
    }

    @Transactional(readOnly = true)
    public BusinessHours getBusinessHoursById(UUID id) {
        log.debug("Fetching business hours with ID: {}", id);
        return businessHoursRepository.findById(id)
                .orElseThrow(() -> new BusinessHoursNotFoundException("Business hours not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<BusinessHours> getBusinessHoursByClinicIdAndDay(UUID clinicId, DayOfWeek dayOfWeek) {
        log.debug("Finding business hours for clinic ID: {} and day: {}", clinicId, dayOfWeek);
        // Verify clinic exists
        clinicService.getClinicById(clinicId);
        return businessHoursRepository.findByClinicIdAndDayOfWeek(clinicId, dayOfWeek);
    }

    @Transactional(readOnly = true)
    public List<BusinessHours> getActiveHoursByDayAndDate(UUID clinicId, DayOfWeek dayOfWeek, LocalDate date) {
        log.debug("Finding active business hours for clinic ID: {} day: {} and date: {}", clinicId, dayOfWeek, date);
        return businessHoursRepository.findActiveHoursByDayAndDate(clinicId, dayOfWeek, date);
    }

    @Transactional
    public BusinessHours createBusinessHours(UUID clinicId, BusinessHoursRequest request) {
        log.info("Creating business hours for clinic ID: {} and day: {}", clinicId, request.getDayOfWeek());
        request.validate(); // Validate request

        Clinic clinic = clinicService.getClinicById(clinicId);

        // Check for time overlaps
        if (businessHoursRepository.existsOverlappingBusinessHours(
                clinicId,
                request.getDayOfWeek(),
                request.getOpenTime(),
                request.getCloseTime())) {
            throw new BusinessHoursOverlapException(
                    "Overlapping business hours found for day: " + request.getDayOfWeek());
        }

        // Get next sequence number
        Integer maxSequence = businessHoursRepository.findMaxSequenceForDay(clinicId, request.getDayOfWeek());
        int nextSequence = (maxSequence != null) ? maxSequence + 1 : 1;

        BusinessHours businessHours = businessHoursMapper.toEntity(request);
        businessHours.setClinic(clinic);
        businessHours.setSequence(nextSequence);  // Set sequence immediately after mapper.toEntity()

        BusinessHours savedHours = businessHoursRepository.save(businessHours);

        // Publish event
        eventPublisher.publishEvent(new BusinessHoursEvent(
                this,
                savedHours,
                BusinessHoursEvent.EventType.CREATED));

        return savedHours;
    }

    @Transactional
    public BusinessHours updateBusinessHours(UUID id, BusinessHoursRequest request) {
        log.info("Updating business hours with ID: {}", id);
        request.validate(); // Validate request

        BusinessHours businessHours = getBusinessHoursById(id);

        // Detect changes
        boolean timeChanged = !request.getOpenTime().equals(businessHours.getOpenTime()) ||
                !request.getCloseTime().equals(businessHours.getCloseTime());
        boolean dayChanged  = !request.getDayOfWeek().equals(businessHours.getDayOfWeek());

        // If day or time changed, check for overlaps
        if (timeChanged || dayChanged) {
            if (businessHoursRepository.existsOverlappingBusinessHours(
                    businessHours.getClinic().getId(),
                    request.getDayOfWeek(),
                    request.getOpenTime(),
                    request.getCloseTime(),
                    id)) {
                throw new BusinessHoursOverlapException(
                        "Overlapping business hours found for day: " + request.getDayOfWeek());
            }
        }

        BusinessHours originalHours = copyHours(businessHours);

        // If the day changed, assign a new sequence on the target day
        if (dayChanged) {
            Integer maxSeqOnNewDay = businessHoursRepository.findMaxSequenceForDay(
                    businessHours.getClinic().getId(),
                    request.getDayOfWeek());
            int nextSequence = (maxSeqOnNewDay != null ? maxSeqOnNewDay + 1 : 1);
            businessHours.setSequence(nextSequence);
        }

        // Now apply the rest of the updates
        businessHours.setDayOfWeek(request.getDayOfWeek());
        businessHours.setOpenTime(request.getOpenTime());
        businessHours.setCloseTime(request.getCloseTime());
        businessHours.setActive(request.getActive());

        BusinessHours updatedHours = businessHoursRepository.save(businessHours);

        // Publish event
        eventPublisher.publishEvent(new BusinessHoursEvent(
                this,
                updatedHours,
                BusinessHoursEvent.EventType.UPDATED,
                originalHours));

        return updatedHours;
    }

    @Transactional
    public void deleteBusinessHours(UUID id) {
        log.info("Deleting business hours with ID: {}", id);
        BusinessHours businessHours = getBusinessHoursById(id);

        // Get current username for soft delete
        String username = getCurrentUsername();
        businessHours.softDelete(username);

        businessHoursRepository.save(businessHours);

        // Publish event
        eventPublisher.publishEvent(new BusinessHoursEvent(
                this,
                businessHours,
                BusinessHoursEvent.EventType.DELETED));
    }

    /**
     * Creates a copy of the business hours entity for event comparison
     */
    private BusinessHours copyHours(BusinessHours original) {
        BusinessHours copy = BusinessHours.builder()
                .clinic(original.getClinic())
                .dayOfWeek(original.getDayOfWeek())
                .sequence(original.getSequence())
                .openTime(original.getOpenTime())
                .closeTime(original.getCloseTime())
                .active(original.getActive())
                .validFrom(original.getValidFrom())
                .validTo(original.getValidTo())
                .label(original.getLabel())
                .build();
        copy.setId(original.getId());
        return copy;
    }

    /**
     * Gets the current username from request header or "system" if not available
     */
    private String getCurrentUsername() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String username = request.getHeader("X-User-Username");
                if (username != null && !username.isBlank()) {
                    return username;
                }
            }
        } catch (Exception e) {
            // Fall back to system user
        }

        return "system";
    }

    // Custom exceptions
    public static class BusinessHoursNotFoundException extends BaseException {
        public BusinessHoursNotFoundException(String message) {
            super(message, ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    public static class BusinessHoursOverlapException extends BaseException {
        public BusinessHoursOverlapException(String message) {
            super(message, ErrorCode.VALIDATION_ERROR);
        }
    }
}