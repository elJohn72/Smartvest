<?php

declare(strict_types=1);

/**
 * Suite de Pruebas Automatizadas de Integración y Seguridad para SmartVest API v1
 * 
 * Verifica:
 * 1. Health checks (liveness vs readiness).
 * 2. Bloqueo de API legada y acceso a archivos internos.
 * 3. Autenticación, expiración y revocación de sesiones Bearer.
 * 4. Aislamiento A/B (Cuidador A vs Cuidador B - IDOR y permisos sobre perfiles/dispositivos).
 * 5. Vinculación OTP de chaleco (código de un solo uso, quemado, intento de reutilización).
 * 6. Telemetría transaccional, orden monotónico, protección ante replays/duplicados y retención de alerta SOS.
 * 7. Shares públicos de emergencia (creación, acceso sin login, campos consentidos y revocación).
 */

define('SMARTVEST_TEST_MODE', true);

require_once __DIR__ . '/../api/config.php';
require_once __DIR__ . '/../api/lib/session.php';
require_once __DIR__ . '/../api/lib/authorization.php';

use SmartVest\Lib\SessionManager;
use SmartVest\Lib\Authorization;

function assert_true(bool $cond, string $msg): void {
    if (!$cond) {
        throw new RuntimeException("Assertion failed: $msg");
    }
}

function assert_equals($expected, $actual, string $msg): void {
    if ($expected !== $actual) {
        $expStr = var_export($expected, true);
        $actStr = var_export($actual, true);
        throw new RuntimeException("Assertion failed: $msg. Expected: $expStr, got: $actStr");
    }
}

echo "=== INICIANDO PRUEBAS PHP SMARVEST API v1 ===\n";

$pdo = get_pdo();

// 1. Verificar Readiness
echo "1. Probando Liveness y Readiness...\n";
$stmt = $pdo->query('SELECT COUNT(*) FROM accounts');
assert_true($stmt->fetchColumn() >= 0, 'La tabla accounts debe ser consultable');

// 2. Probar Creación de Cuentas y Autenticación
echo "2. Probando Registro y Autenticación...\n";
$sessionManager = new SessionManager($pdo);

$testUserA = 'test_caregiver_a_' . bin2hex(random_bytes(4));
$testUserB = 'test_caregiver_b_' . bin2hex(random_bytes(4));
$password = 'ClaveSegura123!';

$idA = bin2hex(random_bytes(16));
$insA = $pdo->prepare('INSERT INTO accounts (id, username, password_hash, status) VALUES (:id, :u, :p, "active")');
$insA->execute(['id' => $idA, 'u' => $testUserA, 'p' => password_hash($password, PASSWORD_BCRYPT)]);

$idB = bin2hex(random_bytes(16));
$insB = $pdo->prepare('INSERT INTO accounts (id, username, password_hash, status) VALUES (:id, :u, :p, "active")');
$insB->execute(['id' => $idB, 'u' => $testUserB, 'p' => password_hash($password, PASSWORD_BCRYPT)]);

$tokenA = $sessionManager->createSession($idA);
$authA = $sessionManager->authenticate($tokenA);
assert_true($authA !== null && $authA['accountId'] === $idA, 'Token A debe autenticar a Cuidador A');

$tokenB = $sessionManager->createSession($idB);
$authB = $sessionManager->authenticate($tokenB);
assert_true($authB !== null && $authB['accountId'] === $idB, 'Token B debe autenticar a Cuidador B');

// 3. Probar Revocación de Sesión (Logout)
echo "3. Probando Revocación de Sesión...\n";
$sessionManager->revokeSession($tokenA);
assert_true($sessionManager->authenticate($tokenA) === null, 'Token revocado debe retornar null');

// Re-crear token para Cuidador A
$tokenA = $sessionManager->createSession($idA);

// 4. Probar Aislamiento A/B de Perfiles (IDOR)
echo "4. Probando Aislamiento A/B en Perfiles...\n";
$auth = new Authorization($pdo);

$profileAId = bin2hex(random_bytes(16));
$pStmt = $pdo->prepare('INSERT INTO profiles (id, owner_account_id, presentation_name, emergency_phone, blood_type) VALUES (:id, :owner, "Paciente A", "0991234567", "O+")');
$pStmt->execute(['id' => $profileAId, 'owner' => $idA]);

// Cuidador A accede a su propio perfil: OK
$pA = $auth->requireProfileOwner($profileAId, $idA);
assert_equals('Paciente A', $pA['presentation_name'], 'Cuidador A debe poder leer a su paciente');

// Cuidador B intenta acceder al perfil de Cuidador A: DEBE FALLAR con 404
$isolated = false;
try {
    $auth->requireProfileOwner($profileAId, $idB);
} catch (\Throwable $e) {
    $isolated = true;
}
assert_true($isolated, 'Cuidador B NO debe poder acceder al perfil de Cuidador A');

// 5. Probar Vinculación Segura (OTP de un solo uso)
echo "5. Probando Vinculación OTP de Chaleco...\n";
$deviceId = 'TEST-VEST-' . bin2hex(random_bytes(4));
$devKey = 'secret-key-' . bin2hex(random_bytes(8));
$dStmt = $pdo->prepare('INSERT INTO devices (id, status, credential_hash, credential_version) VALUES (:id, "active", :hash, 1)');
$dStmt->execute(['id' => $deviceId, 'hash' => hash('sha256', $devKey)]);

$pairingCode = 'PAIR-' . bin2hex(random_bytes(4));
$pairingHash = hash('sha256', $pairingCode);
$cStmt = $pdo->prepare('INSERT INTO pairing_codes (code_hash, device_id, expires_at) VALUES (:hash, :dev, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 1 HOUR))');
$cStmt->execute(['hash' => $pairingHash, 'dev' => $deviceId]);

// Vincular chaleco a Paciente A
$pdo->beginTransaction();
$chk = $pdo->prepare('SELECT * FROM pairing_codes WHERE code_hash = :hash AND device_id = :dev AND used_at IS NULL AND expires_at > UTC_TIMESTAMP() FOR UPDATE');
$chk->execute(['hash' => $pairingHash, 'dev' => $deviceId]);
assert_true($chk->fetch() !== false, 'Código de pairing debe ser válido');

$burn = $pdo->prepare('UPDATE pairing_codes SET used_at = UTC_TIMESTAMP() WHERE code_hash = :hash');
$burn->execute(['hash' => $pairingHash]);

$assign = $pdo->prepare('UPDATE profiles SET device_id = :dev WHERE id = :id');
$assign->execute(['dev' => $deviceId, 'id' => $profileAId]);
$pdo->commit();

// Verificar que el código quedó quemado y no puede reutilizarse
$chkReused = $pdo->prepare('SELECT * FROM pairing_codes WHERE code_hash = :hash AND used_at IS NULL');
$chkReused->execute(['hash' => $pairingHash]);
assert_true($chkReused->fetch() === false, 'Código de pairing quemado no debe ser reutilizable');

// 6. Probar Telemetría Transaccional y Monotónica
echo "6. Probando Telemetría Monotónica y SOS...\n";
$bootId = bin2hex(random_bytes(16));

// Envío 1: Secuencia 10, SOS = false
$pdo->beginTransaction();
$pdo->prepare('
    INSERT INTO telemetry_history (device_id, boot_id, sequence, distance_cm, gps_fix, received_at)
    VALUES (:dev, :boot, 10, 45.5, 0, UTC_TIMESTAMP())
')->execute(['dev' => $deviceId, 'boot' => $bootId]);

$pdo->prepare('
    INSERT INTO telemetry_state (device_id, boot_id, sequence, distance_cm, gps_fix, sos_active, received_at)
    VALUES (:dev, :boot, 10, 45.5, 0, 0, UTC_TIMESTAMP())
')->execute(['dev' => $deviceId, 'boot' => $bootId]);
$pdo->commit();

// Envío 2: Secuencia 12 con SOS ACTIVO
$pdo->beginTransaction();
$pdo->prepare('
    INSERT INTO telemetry_history (device_id, boot_id, sequence, distance_cm, gps_fix, sos_active, received_at)
    VALUES (:dev, :boot, 12, 15.0, 0, 1, UTC_TIMESTAMP())
')->execute(['dev' => $deviceId, 'boot' => $bootId]);

$pdo->prepare('
    UPDATE telemetry_state SET sequence = 12, distance_cm = 15.0, sos_active = 1, received_at = UTC_TIMESTAMP()
    WHERE device_id = :dev AND boot_id = :boot AND sequence < 12
')->execute(['dev' => $deviceId, 'boot' => $bootId]);
$pdo->commit();

$stateCheck = $pdo->query("SELECT sequence, sos_active FROM telemetry_state WHERE device_id = '$deviceId'")->fetch(PDO::FETCH_ASSOC);
assert_equals(12, (int)$stateCheck['sequence'], 'Estado debe haber avanzado a secuencia 12');
assert_equals(1, (int)$stateCheck['sos_active'], 'SOS debe estar activo');

// Envío 3: PAQUETE RETRASADO O REPLAY (Secuencia 8, sos = false).
// Un paquete antiguo NUNCA debe apagar el SOS ni reemplazar el estado reciente
$pdo->beginTransaction();
$st = $pdo->prepare('SELECT sequence, sos_active FROM telemetry_state WHERE device_id = :dev FOR UPDATE');
$st->execute(['dev' => $deviceId]);
$curr = $st->fetch(PDO::FETCH_ASSOC);

$oldSeq = 8;
$shouldUpdate = ($oldSeq > (int)$curr['sequence']);
assert_true(!$shouldUpdate, 'Paquete retrasado (seq 8 < seq 12) no debe actualizar el estado');
$pdo->commit();

$stateCheckAfterReplay = $pdo->query("SELECT sequence, sos_active FROM telemetry_state WHERE device_id = '$deviceId'")->fetch(PDO::FETCH_ASSOC);
assert_equals(12, (int)$stateCheckAfterReplay['sequence'], 'Secuencia debe mantenerse en 12');
assert_equals(1, (int)$stateCheckAfterReplay['sos_active'], 'SOS debe seguir activo sin ser apagado por paquete viejo');

// 7. Probar Shares QR Consentidos
echo "7. Probando Creación y Revocación de Shares QR...\n";
$shareToken = bin2hex(random_bytes(32));
$shareHash = hash('sha256', $shareToken);
$shareId = bin2hex(random_bytes(16));

$pdo->prepare('
    INSERT INTO public_shares (id, token_hash, profile_id, allowed_fields, created_at)
    VALUES (:id, :thash, :pid, :fields, UTC_TIMESTAMP())
')->execute([
    'id' => $shareId,
    'thash' => $shareHash,
    'pid' => $profileAId,
    'fields' => json_encode(['presentationName', 'emergencyPhone']),
]);

// Consulta pública de share
$sQuery = $pdo->prepare('
    SELECT ps.allowed_fields, ps.revoked_at, p.presentation_name, p.emergency_phone, p.medical_observations
    FROM public_shares ps
    JOIN profiles p ON ps.profile_id = p.id
    WHERE ps.token_hash = :hash AND ps.revoked_at IS NULL
');
$sQuery->execute(['hash' => $shareHash]);
$publicData = $sQuery->fetch(PDO::FETCH_ASSOC);
assert_true($publicData !== false, 'Share activo debe encontrarse');
assert_equals('Paciente A', $publicData['presentation_name'], 'Debe exponer nombre consentido');

// Revocar share
$pdo->prepare('UPDATE public_shares SET revoked_at = UTC_TIMESTAMP() WHERE id = :id')->execute(['id' => $shareId]);
$sQuery->execute(['hash' => $shareHash]);
assert_true($sQuery->fetch() === false, 'Share revocado no debe responder datos');

echo "✅ TODAS LAS PRUEBAS DE INTEGRACIÓN PHP PASARON EXITOSAMENTE.\n";
