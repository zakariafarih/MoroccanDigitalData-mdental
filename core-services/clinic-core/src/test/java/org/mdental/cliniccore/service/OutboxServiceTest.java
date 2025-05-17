package org.mdental.cliniccore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mdental.cliniccore.model.entity.Address;
import org.mdental.cliniccore.model.entity.BusinessHours;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.model.entity.Outbox;
import org.mdental.cliniccore.repository.OutboxRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    private ObjectMapper objectMapper;
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        // Register Jackson modules needed for Java 8 date/time
        objectMapper.findAndRegisterModules();

        outboxService = new OutboxService(outboxRepository, objectMapper);
    }

    @ParameterizedTest
    @MethodSource("provideEntityPairs")
    void saveEvent_shouldPersistEntityChanges(String aggregateType, Object oldValue, Object newValue) {
        // Arrange
        UUID aggregateId = UUID.randomUUID();
        String eventType = "UPDATED";

        // Act
        outboxService.saveEvent(aggregateType, aggregateId, eventType, oldValue, newValue);

        // Assert
        ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());

        Outbox savedOutbox = outboxCaptor.getValue();
        assertThat(savedOutbox.getAggregateType()).isEqualTo(aggregateType);
        assertThat(savedOutbox.getAggregateId()).isEqualTo(aggregateId);
        assertThat(savedOutbox.getEventType()).isEqualTo(eventType);

        try {
            // Verify payload structure
            JsonNode payload = objectMapper.readTree(savedOutbox.getPayload());

            if (oldValue != null) {
                assertThat(payload.has("oldValue")).isTrue();
            }

            if (newValue != null) {
                assertThat(payload.has("newValue")).isTrue();
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to parse payload JSON", e);
        }
    }

    @Test
    void saveEvent_shouldHandleNullOldValue() {
        // Arrange
        UUID aggregateId = UUID.randomUUID();
        String aggregateType = "Clinic";
        String eventType = "CREATED";
        Clinic newClinic = new Clinic();
        newClinic.setName("Test Clinic");

        // Act
        outboxService.saveEvent(aggregateType, aggregateId, eventType, null, newClinic);

        // Assert
        ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());

        Outbox savedOutbox = outboxCaptor.getValue();

        try {
            JsonNode payload = objectMapper.readTree(savedOutbox.getPayload());
            assertThat(payload.has("oldValue")).isFalse();
            assertThat(payload.has("newValue")).isTrue();
        } catch (Exception e) {
            throw new AssertionError("Failed to parse payload JSON", e);
        }
    }

    // Helper method to provide test data for parameterized test
    private static Stream<Arguments> provideEntityPairs() {
        // Clinic entity pair
        Clinic oldClinic = new Clinic();
        oldClinic.setName("Old Clinic Name");
        oldClinic.setSlug("old-slug");

        Clinic newClinic = new Clinic();
        newClinic.setName("New Clinic Name");
        newClinic.setSlug("old-slug");

        // Address entity pair
        Address oldAddress = new Address();
        oldAddress.setStreet("123 Old St");
        oldAddress.setCity("Old City");

        Address newAddress = new Address();
        newAddress.setStreet("456 New St");
        newAddress.setCity("Old City");

        // BusinessHours entity pair
        BusinessHours oldHours = new BusinessHours();
        oldHours.setDayOfWeek(DayOfWeek.MONDAY);
        oldHours.setOpenTime(LocalTime.of(9, 0));
        oldHours.setCloseTime(LocalTime.of(17, 0));

        BusinessHours newHours = new BusinessHours();
        newHours.setDayOfWeek(DayOfWeek.MONDAY);
        newHours.setOpenTime(LocalTime.of(8, 0));
        newHours.setCloseTime(LocalTime.of(18, 0));

        return Stream.of(
                Arguments.of("Clinic", oldClinic, newClinic),
                Arguments.of("Address", oldAddress, newAddress),
                Arguments.of("BusinessHours", oldHours, newHours)
        );
    }
}