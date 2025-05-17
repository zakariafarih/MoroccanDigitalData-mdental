package org.mdental.cliniccore.event;

import lombok.Getter;
import org.mdental.cliniccore.model.entity.BusinessHours;
import org.springframework.context.ApplicationEvent;

@Getter
public class BusinessHoursEvent extends ApplicationEvent {

    private final BusinessHours businessHours;
    private final EventType type;
    private final BusinessHours oldBusinessHours;

    public BusinessHoursEvent(Object source, BusinessHours businessHours, EventType type) {
        this(source, businessHours, type, null);
    }

    public BusinessHoursEvent(Object source, BusinessHours businessHours, EventType type, BusinessHours oldBusinessHours) {
        super(source);
        this.businessHours = businessHours;
        this.type = type;
        this.oldBusinessHours = oldBusinessHours;
    }

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }
}