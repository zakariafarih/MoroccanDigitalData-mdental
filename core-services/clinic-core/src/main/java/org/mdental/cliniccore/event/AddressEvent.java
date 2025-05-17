package org.mdental.cliniccore.event;

import lombok.Getter;
import org.mdental.cliniccore.model.entity.Address;
import org.springframework.context.ApplicationEvent;

@Getter
public class AddressEvent extends ApplicationEvent {

    private final Address address;
    private final EventType type;
    private final Address oldAddress;

    public AddressEvent(Object source, Address address, EventType type) {
        this(source, address, type, null);
    }

    public AddressEvent(Object source, Address address, EventType type, Address oldAddress) {
        super(source);
        this.address = address;
        this.type = type;
        this.oldAddress = oldAddress;
    }

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }
}