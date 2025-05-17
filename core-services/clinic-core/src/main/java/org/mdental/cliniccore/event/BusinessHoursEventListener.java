package org.mdental.cliniccore.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.model.entity.BusinessHours;
import org.mdental.cliniccore.service.OutboxService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for BusinessHours changes.
 * Foundation for Change Data Capture / Outbox pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessHoursEventListener {

    private final OutboxService outboxService;

    @EventListener
    public void handleBusinessHoursEvent(BusinessHoursEvent event) {
        BusinessHours businessHours = event.getBusinessHours();
        BusinessHoursEvent.EventType eventType = event.getType();

        outboxService.saveEvent(
                "BusinessHours",
                businessHours.getId(),
                eventType.name(),
                event.getOldBusinessHours(),
                businessHours
        );
    }
}