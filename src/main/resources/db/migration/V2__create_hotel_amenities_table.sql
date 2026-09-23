-- V2__add_hotel_amenities.sql
-- Add hotel amenities feature
-- Author: Dinesh Sahu (dinesh.sahu@apexon.com) - ID: 206755

-- Amenities a hotel offers (e.g., WiFi, Pool, Gym, Parking, etc.)
-- Modelled as a separate table to allow multiple amenities per hotel
CREATE TABLE IF NOT EXISTS hotel_amenities (
    hotel_id UUID NOT NULL,
    amenity_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (hotel_id, amenity_name),
    CONSTRAINT fk_amenity_hotel FOREIGN KEY (hotel_id) 
        REFERENCES hotels(hotel_id) ON DELETE CASCADE
);

-- Create index for efficient querying
CREATE INDEX idx_hotel_amenities_hotel ON hotel_amenities(hotel_id);

-- Add comments for documentation
COMMENT ON TABLE hotel_amenities IS 'Stores amenities offered by each hotel (many-to-many relationship)';
COMMENT ON COLUMN hotel_amenities.amenity_name IS 'Examples: WiFi, Pool, Gym, Parking, Restaurant, Spa, etc.';
