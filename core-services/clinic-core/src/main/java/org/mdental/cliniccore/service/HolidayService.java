package org.mdental.cliniccore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.model.dto.HolidayRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.model.entity.Holiday;
import org.mdental.cliniccore.repository.HolidayRepository;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final ClinicService clinicService;

    @Transactional(readOnly = true)
    public List<Holiday> getHolidaysByClinicId(UUID clinicId) {
        log.debug("Fetching holidays for clinic ID: {}", clinicId);
        // Verify clinic exists
        clinicService.getClinicById(clinicId);
        return holidayRepository.findByClinicId(clinicId);
    }

    @Transactional(readOnly = true)
    public List<Holiday> getHolidaysByClinicIdAndDateRange(UUID clinicId, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching holidays for clinic ID: {} between {} and {}", clinicId, startDate, endDate);
        // Verify clinic exists
        clinicService.getClinicById(clinicId);
        return holidayRepository.findByClinicIdAndDateBetween(clinicId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Holiday getHolidayById(UUID id) {
        log.debug("Fetching holiday with ID: {}", id);
        return holidayRepository.findById(id)
                .orElseThrow(() -> new HolidayNotFoundException("Holiday not found with ID: " + id));
    }

    @Transactional
    public Holiday createHoliday(UUID clinicId, HolidayRequest request) {
        log.info("Creating holiday for clinic ID: {} on date: {}", clinicId, request.getDate());

        Clinic clinic = clinicService.getClinicById(clinicId);

        Holiday holiday = Holiday.builder()
                .clinic(clinic)
                .date(request.getDate())
                .description(request.getDescription())
                .recurring(request.getRecurring())
                .build();

        return holidayRepository.save(holiday);
    }

    @Transactional
    public Holiday updateHoliday(UUID id, HolidayRequest request) {
        log.info("Updating holiday with ID: {}", id);

        Holiday holiday = getHolidayById(id);

        holiday.setDate(request.getDate());
        holiday.setDescription(request.getDescription());
        holiday.setRecurring(request.getRecurring());

        return holidayRepository.save(holiday);
    }

    @Transactional
    public void deleteHoliday(UUID id) {
        log.info("Deleting holiday with ID: {}", id);
        Holiday holiday = getHolidayById(id);

        // Use soft delete instead of hard delete
        String username = getCurrentUsername();
        holiday.softDelete(username);
        holidayRepository.save(holiday);
    }

    /**
     * Gets the current authenticated username or "system" if not available
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "system";
    }

    // Custom exceptions
    public static class HolidayNotFoundException extends BaseException {
        public HolidayNotFoundException(String message) {
            super(message, ErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}