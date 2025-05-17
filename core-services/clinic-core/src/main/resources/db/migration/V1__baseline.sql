CREATE TABLE IF NOT EXISTS clinics (
                                       id UUID PRIMARY KEY,
                                       name VARCHAR(100) NOT NULL,
    slug VARCHAR(50) NOT NULL DEFAULT 'unknown' UNIQUE,
    realm VARCHAR(50) NOT NULL UNIQUE,
    legal_name VARCHAR(100),
    tax_id VARCHAR(50),
    description TEXT,
    logo_url VARCHAR(255),
    primary_color VARCHAR(50),
    secondary_color VARCHAR(50),
    license_number VARCHAR(100),
    license_expiry DATE,
    privacy_policy_url VARCHAR(255),
    default_time_zone VARCHAR(50),
    default_currency VARCHAR(10),
    locale VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
    );

CREATE INDEX IF NOT EXISTS idx_clinic_realm ON clinics(realm);
CREATE INDEX IF NOT EXISTS idx_clinic_slug ON clinics(slug);
CREATE INDEX IF NOT EXISTS idx_clinic_status ON clinics(status);
CREATE INDEX IF NOT EXISTS idx_clinic_deleted_at ON clinics(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS contact_info (
                                            id UUID PRIMARY KEY,
                                            clinic_id UUID NOT NULL REFERENCES clinics(id),
    type VARCHAR(20) NOT NULL,
    label VARCHAR(100),
    value VARCHAR(255) NOT NULL,
    is_primary BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
    );

CREATE INDEX IF NOT EXISTS idx_contact_clinic_id ON contact_info(clinic_id);
CREATE INDEX IF NOT EXISTS idx_contact_type ON contact_info(type);
CREATE INDEX IF NOT EXISTS idx_contact_primary ON contact_info(is_primary);
CREATE INDEX IF NOT EXISTS idx_contact_deleted_at ON contact_info(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS addresses (
                                         id UUID PRIMARY KEY,
                                         clinic_id UUID NOT NULL REFERENCES clinics(id),
    type VARCHAR(20) NOT NULL,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    zip VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    is_primary BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
    );

CREATE INDEX IF NOT EXISTS idx_address_clinic_id ON addresses(clinic_id);
CREATE INDEX IF NOT EXISTS idx_address_type ON addresses(type);
CREATE INDEX IF NOT EXISTS idx_address_primary ON addresses(is_primary);
CREATE INDEX IF NOT EXISTS idx_address_deleted_at ON addresses(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS business_hours (
                                              id UUID PRIMARY KEY,
                                              clinic_id UUID NOT NULL REFERENCES clinics(id),
    day_of_week VARCHAR(10) NOT NULL,
    sequence INTEGER NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    active BOOLEAN NOT NULL,
    valid_from DATE,
    valid_to DATE,
    label VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
    );

CREATE INDEX IF NOT EXISTS idx_hours_clinic_id ON business_hours(clinic_id);
CREATE INDEX IF NOT EXISTS idx_hours_day_active ON business_hours(day_of_week, active);
CREATE INDEX IF NOT EXISTS idx_hours_valid_dates ON business_hours(valid_from, valid_to);
CREATE INDEX IF NOT EXISTS idx_hours_deleted_at ON business_hours(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS holidays (
                                        id UUID PRIMARY KEY,
                                        clinic_id UUID NOT NULL REFERENCES clinics(id),
    date DATE NOT NULL,
    description VARCHAR(255),
    recurring BOOLEAN NOT NULL,
    rule_type VARCHAR(20),
    rule_pattern VARCHAR(100),
    is_half_day BOOLEAN NOT NULL,
    half_day_start TIME,
    half_day_end TIME,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
    );

CREATE INDEX IF NOT EXISTS idx_holiday_clinic_id ON holidays(clinic_id);
CREATE INDEX IF NOT EXISTS idx_holiday_date ON holidays(date);
CREATE INDEX IF NOT EXISTS idx_holiday_recurring ON holidays(recurring);
CREATE INDEX IF NOT EXISTS idx_holiday_deleted_at ON holidays(deleted_at) WHERE deleted_at IS NULL;