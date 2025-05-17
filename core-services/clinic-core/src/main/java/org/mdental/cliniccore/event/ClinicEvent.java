package org.mdental.cliniccore.event;

import lombok.Getter;
import org.mdental.cliniccore.model.entity.Clinic;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClinicEvent extends ApplicationEvent {

    private final Clinic clinic;
    private final EventType type;
    private final Clinic oldClinic;

    public ClinicEvent(Object source, Clinic clinic, EventType type) {
        this(source, clinic, type, null);
    }

    public ClinicEvent(Object source, Clinic clinic, EventType type, Clinic oldClinic) {
        super(source);
        this.clinic = clinic;
        this.type = type;
        this.oldClinic = oldClinic;
    }

    public enum EventType {
        CREATED,
        UPDATED,
        STATUS_CHANGED,
        DELETED
    }
}