package org.mdental.authcore.domain.event;

/**
 * Tenant-related events.
 */
public enum TenantEvent {
    /**
     * Tenant created.
     */
    CREATED,

    /**
     * Tenant updated.
     */
    UPDATED,

    /**
     * Tenant activated.
     */
    ACTIVATED,

    /**
     * Tenant deactivated.
     */
    DEACTIVATED
}