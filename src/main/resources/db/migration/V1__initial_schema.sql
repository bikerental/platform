-- V1: Initial schema for BikeRental platform
-- Migrated from JPA entities (ddl-auto=update)

-- Hotels table (multi-tenant root)
CREATE TABLE hotels (
    hotel_id BIGINT NOT NULL AUTO_INCREMENT,
    hotel_code VARCHAR(50) NOT NULL,
    hotel_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (hotel_id),
    CONSTRAINT uk_hotel_code UNIQUE (hotel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bikes table
CREATE TABLE bikes (
    bike_id BIGINT NOT NULL AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    bike_number VARCHAR(50) NOT NULL,
    bike_type VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    ooo_note TEXT,
    ooo_since DATETIME(6),
    PRIMARY KEY (bike_id),
    CONSTRAINT uk_hotel_bike_number UNIQUE (hotel_id, bike_number),
    CONSTRAINT fk_bike_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Signatures table (stores guest signatures as blobs)
CREATE TABLE signatures (
    signature_id BIGINT NOT NULL AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    signature_data MEDIUMBLOB NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (signature_id),
    INDEX idx_signature_hotel (hotel_id),
    CONSTRAINT fk_signature_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rentals table
CREATE TABLE rentals (
    rental_id BIGINT NOT NULL AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_at DATETIME(6) NOT NULL,
    due_at DATETIME(6) NOT NULL,
    return_at DATETIME(6),
    room_number VARCHAR(50) NOT NULL,
    bed_number VARCHAR(50),
    tnc_version VARCHAR(50) NOT NULL,
    signature_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (rental_id),
    INDEX idx_rental_hotel_status (hotel_id, status),
    INDEX idx_rental_due_at (hotel_id, due_at),
    CONSTRAINT fk_rental_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (hotel_id),
    CONSTRAINT fk_rental_signature FOREIGN KEY (signature_id) REFERENCES signatures (signature_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rental items table (individual bikes in a rental)
CREATE TABLE rental_items (
    rental_item_id BIGINT NOT NULL AUTO_INCREMENT,
    rental_id BIGINT NOT NULL,
    bike_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RENTED',
    returned_at DATETIME(6),
    lost_reason TEXT,
    -- Generated column for I1 invariant: ensures a bike can only be in one active rental
    rented_bike_id_if_rented BIGINT AS (CASE WHEN status = 'RENTED' THEN bike_id ELSE NULL END) STORED,
    PRIMARY KEY (rental_item_id),
    INDEX idx_rental_item_bike (bike_id),
    CONSTRAINT fk_rental_item_rental FOREIGN KEY (rental_id) REFERENCES rentals (rental_id),
    CONSTRAINT fk_rental_item_bike FOREIGN KEY (bike_id) REFERENCES bikes (bike_id),
    -- Unique constraint on generated column enforces I1: one bike can only be RENTED once at a time
    CONSTRAINT uk_rented_bike UNIQUE (rented_bike_id_if_rented)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Hotel settings table
CREATE TABLE hotel_settings (
    settings_id BIGINT NOT NULL AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    rental_duration_options TEXT,
    grace_minutes INT NOT NULL DEFAULT 0,
    tnc_text TEXT,
    tnc_version VARCHAR(50),
    PRIMARY KEY (settings_id),
    CONSTRAINT uk_hotel_settings UNIQUE (hotel_id),
    CONSTRAINT fk_settings_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
