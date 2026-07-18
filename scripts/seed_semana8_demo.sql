USE smartvest;

-- Usuario PRUEBA / LOCAL para taller Semana 8 (no es cuenta real)
INSERT INTO users (
  id, full_name, national_id, age, blood_type, address, emergency_phone,
  emergency_contact, medical_observations, created_at, photo, username, password, device_id
) VALUES (
  'demo-s8-001',
  'Demo Semana 8 SmartVest',
  '0000000000',
  25,
  'O+',
  'Quininde - PRUEBA',
  '0999999999',
  '{"name":"Contacto Demo","relationship":"Familiar","phone":"0988888888"}',
  'Usuario PRUEBA para taller Semana 8',
  '2026-07-17T00:00:00+00:00',
  NULL,
  'demo.s8@smartvest.local',
  '$2y$10$pxDhNqaRBxuJyLiwZSEhR.xET6tkGBCNXjuhBv0wxRb2zBXqlRhj6',
  'VEST-DEMO'
) ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  password = VALUES(password),
  device_id = VALUES(device_id),
  full_name = VALUES(full_name);

INSERT INTO users (
  id, full_name, national_id, age, blood_type, address, emergency_phone,
  emergency_contact, medical_observations, created_at, photo, username, password, device_id
) VALUES (
  'demo-s8-002',
  'Paciente Demo N+1',
  '0000000001',
  40,
  'A+',
  'Esmeraldas - PRUEBA',
  '0977777777',
  '{"name":"Cuidador Demo","relationship":"Hijo","phone":"0966666666"}',
  'Segundo usuario para demostrar N+1',
  '2026-07-17T00:00:01+00:00',
  NULL,
  NULL,
  NULL,
  'VEST-002'
) ON DUPLICATE KEY UPDATE
  device_id = VALUES(device_id),
  full_name = VALUES(full_name);

INSERT INTO iot_states (device_id, distance_cm, latitude, longitude, sos_active, battery_level)
VALUES
  ('VEST-DEMO', 85.0, -0.1806530, -78.4678340, 0, 85),
  ('VEST-002', 40.0, -0.1900000, -78.4800000, 0, 70)
ON DUPLICATE KEY UPDATE
  distance_cm = VALUES(distance_cm),
  battery_level = VALUES(battery_level);
