-- V1__initial_schema.sql
CREATE TABLE IF NOT EXISTS patients (
                                        id UUID PRIMARY KEY,
                                        clinic_id UUID NOT NULL,
                                        first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    marital_status VARCHAR(20),
    preferred_language VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    version BIGINT
    );

CREATE INDEX IF NOT EXISTS idx_patient_clinic_id ON patients(clinic_id);
