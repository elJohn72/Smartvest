USE smartvest;

CREATE TABLE IF NOT EXISTS iot_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    distance_cm DECIMAL(10, 1) DEFAULT NULL,
    latitude DECIMAL(10, 7) NOT NULL DEFAULT 0,
    longitude DECIMAL(10, 7) NOT NULL DEFAULT 0,
    sos_active TINYINT(1) NOT NULL DEFAULT 0,
    battery_level INT DEFAULT NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_iot_history_device_time (device_id, recorded_at)
);
