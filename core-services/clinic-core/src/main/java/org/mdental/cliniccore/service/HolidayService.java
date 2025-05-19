package org.mdental.cliniccore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.model.dto.HolidayRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.model.entity.Holiday;
import org.mdental.cliniccore.repository.HolidayRepository;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        // Get fixed-date holidays
        List<Holiday> fixedHolidays = holidayRepository.findByClinicId(clinicId);

        // Get recurring holidays
        List<Holiday> recurringHolidays = holidayRepository.findRecurringHolidaysByClinicId(clinicId);

        // Combine both lists
        Set<Holiday> allHolidays = new HashSet<>(fixedHolidays);
        allHolidays.addAll(recurringHolidays);

        return List.copyOf(allHolidays);
    }

    @Transactional(readOnly = true)
    public List<Holiday> getHolidaysByClinicIdAndDateRange(UUID clinicId, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching holidays for clinic ID: {} between {} and {}", clinicId, startDate, endDate);
        // Verify clinic exists
        clinicService.getClinicById(clinicId);

        // Get fixed-date holidays in the range
        List<Holiday> fixedHolidays = holidayRepository.findByClinicIdAndDateBetween(clinicId, startDate, endDate);

        // Get recurring holidays
        List<Holiday> recurringHolidays = holidayRepository.findRecurringHolidaysByClinicId(clinicId);

        // Combine both lists
        Set<Holiday> allHolidays = new HashSet<>(fixedHolidays);
        allHolidays.addAll(recurringHolidays);

        return List.copyOf(allHolidays);
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
    public static class HolidayNotFoundException extends BaseException {
        public HolidayNotFoundException(String message) {
            super(message, ErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}