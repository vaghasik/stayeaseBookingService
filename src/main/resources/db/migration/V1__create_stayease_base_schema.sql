-- V1__init.sql
-- Initial schema creation for StayEase Hotel Booking Service
-- Author: Dinesh Sahu (dinesh.sahu@apexon.com) - ID: 206755

-- Create users table first (referenced by hotels and bookings)
CREATE TABLE IF NOT EXISTS users (
    user_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email_address VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    user_role VARCHAR(20) NOT NULL CHECK (user_role IN ('GUEST', 'MANAGER', 'ADMIN')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Create hotels table (manager_id references users)
CREATE TABLE IF NOT EXISTS hotels (
    hotel_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    hotel_name VARCHAR(150) NOT NULL,
    description TEXT,
    street_address VARCHAR(255),
    city_name VARCHAR(100) NOT NULL,
    state_name VARCHAR(100),
    country_name VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(150),
    manager_id UUID NOT NULL,
    star_rating INTEGER CHECK (star_rating >= 1 AND star_rating <= 5),
    cover_image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_hotel_manager FOREIGN KEY (manager_id) REFERENCES users(user_id) ON DELETE RESTRICT
);

-- Create rooms table (hotel_id references hotels)
CREATE TABLE IF NOT EXISTS rooms (
    room_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    hotel_id UUID NOT NULL,
    room_number VARCHAR(20) NOT NULL,
    room_type VARCHAR(30) NOT NULL CHECK (room_type IN ('SINGLE', 'DOUBLE', 'SUITE', 'DELUXE', 'FAMILY')),
    room_description TEXT,
    price_per_night NUMERIC(10, 2) NOT NULL CHECK (price_per_night > 0),
    max_occupancy INTEGER NOT NULL CHECK (max_occupancy > 0),
    room_image_url VARCHAR(500),
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_hotel_room UNIQUE (hotel_id, room_number),
    CONSTRAINT fk_room_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE
);

-- Create bookings table (references users and rooms)
CREATE TABLE IF NOT EXISTS bookings (
    booking_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    booking_reference VARCHAR(30) NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    room_id UUID NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    number_of_guests INTEGER NOT NULL CHECK (number_of_guests > 0),
    total_amount NUMERIC(10, 2) NOT NULL CHECK (total_amount >= 0),
    booking_status VARCHAR(20) NOT NULL CHECK (booking_status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    special_requests TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_checkout_after_checkin CHECK (check_out_date > check_in_date),
    CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE RESTRICT
);

-- Create indexes for better query performance
CREATE INDEX idx_users_email ON users(email_address);
CREATE INDEX idx_users_role ON users(user_role);
CREATE INDEX idx_hotels_city ON hotels(city_name);
CREATE INDEX idx_hotels_manager ON hotels(manager_id);
CREATE INDEX idx_rooms_hotel ON rooms(hotel_id);
CREATE INDEX idx_rooms_type ON rooms(room_type);
CREATE INDEX idx_bookings_customer ON bookings(customer_id);
CREATE INDEX idx_bookings_room ON bookings(room_id);
CREATE INDEX idx_bookings_dates ON bookings(check_in_date, check_out_date);
CREATE INDEX idx_bookings_status ON bookings(booking_status);
