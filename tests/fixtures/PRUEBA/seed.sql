-- Fixture de datos PRUEBA para verificación de aislamiento y multi-cuenta
-- Cuentas de laboratorio PRUEBA A y B (Password: 'Prueba1234!')
-- Hash de 'Prueba1234!': $2y$10$e8w.b3713f0Uv8.0R9N3nO5e8R2v7m1U1I8o7l0h4O2p5K3v4j7mG (simulado/bcrypt real)

INSERT INTO accounts (id, username, password_hash, status) VALUES
('acc-prueba-001', 'cuidador.prueba.a', '$2y$10$e0D9.Jb8mEwK5Z5P4p7K3.rE2d9V6x1Y3h8M0n4Q2r5T7u9v1w2x3', 'active'),
('acc-prueba-002', 'cuidador.prueba.b', '$2y$10$e0D9.Jb8mEwK5Z5P4p7K3.rE2d9V6x1Y3h8M0n4Q2r5T7u9v1w2x3', 'active')
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- Chalecos de prueba provisionados con clave SHA-256 (Clave: 'key-vest-001-secret' -> hash)
-- sha256('key-vest-001-secret') = 2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae
INSERT INTO devices (id, status, credential_hash, credential_version) VALUES
('VEST-001', 'active', '2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae', 1),
('VEST-002', 'active', 'fc613b4f3e6302302f45b221b03a3ff5f578b5db88792bc29a145101d076162e', 1)
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- Código de emparejamiento activo para VEST-001 (Código: 'PAIR-9988' -> sha256)
-- sha256('PAIR-9988') = eb7e44a954497a7833a8bc19ce161c9e81b67f1b13df1869f44db1d966e3ef54
INSERT INTO pairing_codes (code_hash, device_id, expires_at) VALUES
('eb7e44a954497a7833a8bc19ce161c9e81b67f1b13df1869f44db1d966e3ef54', 'VEST-001', DATE_ADD(NOW(), INTERVAL 2 HOUR))
ON DUPLICATE KEY UPDATE expires_at=VALUES(expires_at);
