CREATE DATABASE IF NOT EXISTS smartvest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smartvest;

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    national_id VARCHAR(64) DEFAULT NULL,
    age INT NOT NULL DEFAULT 0,
    blood_type VARCHAR(10) NOT NULL,
    address TEXT DEFAULT NULL,
    emergency_phone VARCHAR(50) NOT NULL,
    emergency_contact LONGTEXT NOT NULL,
    medical_observations TEXT DEFAULT NULL,
    created_at VARCHAR(40) NOT NULL,
    photo LONGTEXT DEFAULT NULL,
    username VARCHAR(255) DEFAULT NULL,
    password VARCHAR(255) DEFAULT NULL,
    device_id VARCHAR(100) DEFAULT NULL,
    KEY idx_users_device_id (device_id)
);

CREATE TABLE IF NOT EXISTS iot_states (
    device_id VARCHAR(100) NOT NULL PRIMARY KEY,
    distance_cm DECIMAL(10, 1) DEFAULT NULL,
    latitude DECIMAL(10, 7) NOT NULL DEFAULT 0,
    longitude DECIMAL(10, 7) NOT NULL DEFAULT 0,
    sos_active TINYINT(1) NOT NULL DEFAULT 0,
    battery_level INT DEFAULT NULL,
    last_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO iot_states (device_id, distance_cm, latitude, longitude, sos_active, battery_level)
VALUES ('VEST-DEMO', 85.0, -0.1806530, -78.4678340, 0, 85)
ON DUPLICATE KEY UPDATE
    distance_cm = VALUES(distance_cm),
    latitude = VALUES(latitude),
    longitude = VALUES(longitude),
    sos_active = VALUES(sos_active),
    battery_level = VALUES(battery_level);
