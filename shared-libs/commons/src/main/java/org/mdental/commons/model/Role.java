package org.mdental.commons.model;

public enum Role {
    SUPER_ADMIN, SUPPORT,
    CLINIC_ADMIN, DOCTOR, RECEPTIONIST, PATIENT;

    public static final String SUPER_ADMIN_AUTHORITY = "ROLE_SUPER_ADMIN";

    public String asSpringRole() {
        return "ROLE_" + name();
    }
}