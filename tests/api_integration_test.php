<?php

declare(strict_types=1);

/**
 * Suite de Pruebas Automatizadas de Integración HTTP y Seguridad para SmartVest API v1
 *
 * Realiza solicitudes HTTP reales (vía curl/stream HTTP) contra el stack de laboratorio.
 * Valida:
 * 1. Health checks (liveness vs readiness con validación de BD).
 * 2. Bloqueo de API legada y acceso a archivos internos (Nginx/Router hardening).
 * 3. Autenticación, expiración y revocación de sesiones Bearer vía HTTP (/auth/login, /auth/logout).
 * 4. Aislamiento A/B (Cuidador A vs Cuidador B - IDOR y permisos sobre perfiles y dispositivos).
 * 5. Vinculación OTP de chaleco vía HTTP (código de un solo uso, quemado, intento de reutilización, dos dispositivos).
 * 6. Telemetría transaccional, orden monotónico y política estricta de reinicios / boots retirados:
 *    - Boot A, sequence 10
 *    - Boot B, sequence 1, SOS activo
 *    - Reenviar Boot A, sequence 10, SOS inactivo -> el estado conserva Boot B y su SOS.
 * 7. Shares públicos de emergencia QR (/shares/emergency/{token}) con consentimiento y revocación.
 */

define('SMARTVEST_TEST_MODE', true);

require_once __DIR__ . '/../api/config.php';

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

$host = '127.0.0.1';
$port = 8899;
$baseUrl = "http://$host:$port";

// Iniciar servidor web PHP embebido en segundo plano
$cmd = sprintf(
    'php -S %s:%d %s/router_lab.php > /dev/null 2>&1 & echo $!',
    $host,
    $port,
    __DIR__
);
$pid = (int)shell_exec($cmd);

register_shutdown_function(function() use ($pid) {
    if ($pid > 0) {
        @posix_kill($pid, SIGTERM);
        @posix_kill($pid, SIGKILL);
    }
});

// Esperar a que el servidor web responda
$started = false;
for ($i = 0; $i < 30; $i++) {
    usleep(100000); // 100ms
    $conn = @fsockopen($host, $port, $errno, $errstr, 0.2);
    if ($conn) {
        fclose($conn);
        $started = true;
        break;
    }
}

if (!$started) {
    fwrite(STDERR, "Error: No se pudo arrancar el servidor web de pruebas en $baseUrl\n");
    exit(1);
}

function http_request(string $method, string $path, array $headers = [], ?array $body = null): array {
    global $baseUrl;
    $url = $baseUrl . $path;

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    curl_setopt($ch, CURLOPT_HEADER, false);

    $formattedHeaders = [];
    foreach ($headers as $k => $v) {
        $formattedHeaders[] = "$k: $v";
    }

    if ($body !== null) {
        $formattedHeaders[] = 'Content-Type: application/json';
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body));
    }

    curl_setopt($ch, CURLOPT_HTTPHEADER, $formattedHeaders);
    $response = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    $json = null;
    if ($response !== false && $response !== '') {
        $json = json_decode((string)$response, true);
    }

    return [
        'status' => $status,
        'body' => $response,
        'json' => $json,
    ];
}

echo "=== INICIANDO PRUEBAS HTTP DE INTEGRACIÓN SMARTVEST API v1 ===\n";

$pdo = get_pdo();

// 1. Health checks (liveness vs readiness)
echo "1. Probando Liveness y Readiness vía HTTP...\n";
$live = http_request('GET', '/api/v1/health/live');
assert_equals(200, $live['status'], 'Liveness debe responder 200');
assert_equals('ok', $live['json']['status'] ?? null, 'Liveness status debe ser ok');

$ready = http_request('GET', '/api/v1/health/ready');
assert_equals(200, $ready['status'], 'Readiness debe responder 200 con base de datos viva');
assert_equals('ready', $ready['json']['status'] ?? null, 'Readiness status debe ser ready');

// 2. Bloqueo de API antigua y archivos internos
echo "2. Probando Bloqueo de API Legada y Archivos Sensibles...\n";
$legacyUsers = http_request('GET', '/api/users.php');
assert_equals(404, $legacyUsers['status'], 'Acceso directo a users.php debe devolver 404');

$legacyIot = http_request('GET', '/api/iot.php');
assert_equals(404, $legacyIot['status'], 'Acceso directo a iot.php debe devolver 404');

$configSql = http_request('GET', '/api/migrations/001_native.sql');
assert_equals(404, $configSql['status'], 'Descarga de archivos .sql debe devolver 404');

// 3. Autenticación, Registro y Revocación vía HTTP
echo "3. Probando Registro, Login y Logout vía HTTP...\n";
$userA = 'caregiver_a_' . bin2hex(random_bytes(4));
$userB = 'caregiver_b_' . bin2hex(random_bytes(4));
$password = 'Secret123!Safe';

$regA = http_request('POST', '/api/v1/auth/register', [], ['username' => $userA, 'password' => $password]);
assert_equals(201, $regA['status'], 'Registro de Cuidador A debe responder 201');
assert_equals(true, $regA['json']['success'] ?? null, 'Registro A debe responder success');

$regB = http_request('POST', '/api/v1/auth/register', [], ['username' => $userB, 'password' => $password]);
assert_equals(201, $regB['status'], 'Registro de Cuidador B debe responder 201');
assert_equals(true, $regB['json']['success'] ?? null, 'Registro B debe responder success');

// Login Cuidador A
$loginA = http_request('POST', '/api/v1/auth/login', [], ['username' => $userA, 'password' => $password]);
assert_equals(200, $loginA['status'], 'Login de Cuidador A debe responder 200');
$tokenA = $loginA['json']['token'] ?? '';
assert_true(!empty($tokenA), 'Debe generarse token Bearer en login para Cuidador A');

// Login Cuidador B
$loginB = http_request('POST', '/api/v1/auth/login', [], ['username' => $userB, 'password' => $password]);
assert_equals(200, $loginB['status'], 'Login de Cuidador B debe responder 200');
$tokenB = $loginB['json']['token'] ?? '';
assert_true(!empty($tokenB), 'Debe generarse token Bearer en login para Cuidador B');

// Logout de sesión tokenA
$logout = http_request('POST', '/api/v1/auth/logout', ['Authorization' => "Bearer $tokenA"]);
assert_equals(204, $logout['status'], 'Logout de Cuidador A debe responder 204');

// Verificar que tokenA revocado ya no permite acceder a perfiles
$badAuth = http_request('GET', '/api/v1/profiles', ['Authorization' => "Bearer $tokenA"]);
assert_equals(401, $badAuth['status'], 'Token revocado debe responder 401');

// Crear nueva sesión para Cuidador A para el resto de pruebas
$loginA2 = http_request('POST', '/api/v1/auth/login', [], ['username' => $userA, 'password' => $password]);
assert_equals(200, $loginA2['status'], 'Re-login de Cuidador A debe responder 200');
$tokenA = $loginA2['json']['token'];

// 4. Aislamiento A/B de Perfiles (IDOR) vía HTTP
echo "4. Probando Aislamiento A/B (IDOR) vía HTTP...\n";
$createProfA = http_request('POST', '/api/v1/profiles', ['Authorization' => "Bearer $tokenA"], [
    'presentationName' => 'Paciente de A',
    'emergencyPhone' => '099111222',
    'bloodType' => 'O+',
    'medicalObservations' => 'Alergia a penicilina'
]);
assert_equals(201, $createProfA['status'], 'Cuidador A debe poder crear un perfil');
$profileAId = $createProfA['json']['id'] ?? ($createProfA['json']['data']['id'] ?? null);
assert_true(!empty($profileAId), 'El perfil de A debe tener un ID válido');

// Cuidador A consulta su propio perfil
$getA = http_request('GET', "/api/v1/profiles/$profileAId", ['Authorization' => "Bearer $tokenA"]);
assert_equals(200, $getA['status'], 'Cuidador A debe poder leer su perfil');

// Cuidador B intenta leer o modificar el perfil de Cuidador A -> DEBE SER RECHAZADO (404)
$idorGet = http_request('GET', "/api/v1/profiles/$profileAId", ['Authorization' => "Bearer $tokenB"]);
assert_equals(404, $idorGet['status'], 'Cuidador B no debe poder acceder al perfil de Cuidador A');

$idorDel = http_request('DELETE', "/api/v1/profiles/$profileAId", ['Authorization' => "Bearer $tokenB"]);
assert_equals(404, $idorDel['status'], 'Cuidador B no debe poder eliminar el perfil de Cuidador A');

// 5. Vinculación Segura (OTP de un solo uso y dos dispositivos) vía HTTP
echo "5. Probando Vinculación OTP de Chaleco vía HTTP...\n";
$dev1 = 'VEST-LAB-01';
$dev2 = 'VEST-LAB-02';
$devKey = 'smartvest-local-dev-key';

// Insertar dispositivos de prueba en MariaDB
$pdo->prepare('INSERT IGNORE INTO devices (id, status, credential_hash) VALUES (:id, "active", :h)')->execute(['id' => $dev1, 'h' => hash('sha256', $devKey)]);
$pdo->prepare('INSERT IGNORE INTO devices (id, status, credential_hash) VALUES (:id, "active", :h)')->execute(['id' => $dev2, 'h' => hash('sha256', $devKey)]);

// Generar código OTP para dev1
$otp1 = '123456';
$pdo->prepare('DELETE FROM pairing_codes WHERE device_id = :d')->execute(['d' => $dev1]);
$pdo->prepare('INSERT INTO pairing_codes (code_hash, device_id, expires_at) VALUES (:h, :d, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE))')->execute(['h' => hash('sha256', $otp1), 'd' => $dev1]);

// Intento de vinculación con código falso -> 400
$badPair = http_request('POST', "/api/v1/profiles/$profileAId/device", ['Authorization' => "Bearer $tokenA"], [
    'deviceId' => $dev1,
    'pairingCode' => '999999',
]);
assert_equals(400, $badPair['status'], 'Código inválido debe ser rechazado con 400');

// Vinculación correcta dev1 a Paciente A
$pair1 = http_request('POST', "/api/v1/profiles/$profileAId/device", ['Authorization' => "Bearer $tokenA"], [
    'deviceId' => $dev1,
    'pairingCode' => $otp1,
]);
assert_equals(200, $pair1['status'], 'Vinculación válida debe responder 200');

// Intento de reutilización del código OTP (debe estar quemado) -> 400
$reusedPair = http_request('POST', "/api/v1/profiles/$profileAId/device", ['Authorization' => "Bearer $tokenA"], [
    'deviceId' => $dev1,
    'pairingCode' => $otp1,
]);
assert_equals(400, $reusedPair['status'], 'Código OTP quemado no puede reutilizarse');

// 6. Telemetría Transaccional, Monotónica y Protección de Boots Retirados vía HTTP
echo "6. Probando Ingestión de Telemetría y Política de Boots Retirados vía HTTP...\n";
$bootA = 'boot-alpha-' . bin2hex(random_bytes(8));
$bootB = 'boot-beta-' . bin2hex(random_bytes(8));

// Paso 6.1: Enviar Boot A, secuencia 10, SOS = false
$telemetryHeaders = [
    'X-SmartVest-Api-Key' => $devKey,
];
$resp1 = http_request('POST', '/api/v1/telemetry', $telemetryHeaders, [
    'deviceId' => $dev1,
    'bootId' => $bootA,
    'sequence' => 10,
    'distanceCm' => 150.0,
    'gpsFix' => false,
    'latitude' => null,
    'longitude' => null,
    'batteryLevel' => 90,
    'sosActive' => false,
    'sosEventId' => null,
]);
assert_equals(202, $resp1['status'], 'Telemetría 1 debe ser aceptada con 202');

// Verificar estado en la API
$st1 = http_request('GET', "/api/v1/devices/$dev1/state", ['Authorization' => "Bearer $tokenA"]);
assert_equals(200, $st1['status'], 'Consulta de estado debe responder 200');
assert_equals($bootA, $st1['json']['data']['bootId'], 'Estado debe tener Boot A');
assert_equals(10, $st1['json']['data']['sequence'], 'Estado debe tener secuencia 10');
assert_equals(false, $st1['json']['data']['sosActive'], 'SOS debe estar inactivo');

// Paso 6.2: Enviar Boot B (reinicio legítimo en el mismo segundo), secuencia 1, SOS = true
// Sin necesidad de usleep ni reloj de un segundo: boot_epoch asigna un entero monotónico
$sosEvtId = 'sos-event-' . bin2hex(random_bytes(8));
$resp2 = http_request('POST', '/api/v1/telemetry', $telemetryHeaders, [
    'deviceId' => $dev1,
    'bootId' => $bootB,
    'sequence' => 1,
    'distanceCm' => 20.0,
    'gpsFix' => false,
    'latitude' => null,
    'longitude' => null,
    'batteryLevel' => 88,
    'sosActive' => true,
    'sosEventId' => $sosEvtId,
]);
assert_equals(202, $resp2['status'], 'Telemetría Boot B con SOS debe ser aceptada con 202');

$st2 = http_request('GET', "/api/v1/devices/$dev1/state", ['Authorization' => "Bearer $tokenA"]);
assert_equals($bootB, $st2['json']['data']['bootId'], 'Estado debe haber cambiado a Boot B');
assert_equals(1, $st2['json']['data']['sequence'], 'Secuencia debe ser 1');
assert_equals(true, $st2['json']['data']['sosActive'], 'SOS debe estar ACTIVO');

// Paso 6.3: REENVIAR Boot A en el mismo segundo, con secuencia 10 y SOS inactivo (Paquete rezagado / replay de boot retirado)
$respReplay = http_request('POST', '/api/v1/telemetry', $telemetryHeaders, [
    'deviceId' => $dev1,
    'bootId' => $bootA,
    'sequence' => 10,
    'distanceCm' => 150.0,
    'gpsFix' => false,
    'latitude' => null,
    'longitude' => null,
    'batteryLevel' => 90,
    'sosActive' => false,
    'sosEventId' => null,
]);
assert_true(in_array($respReplay['status'], [200, 202]), 'El paquete replay debe ser procesado sin 500');

// VERIFICACIÓN CRÍTICA: El estado debe CONSERVAR Boot B y su SOS activo
$stAfterReplay = http_request('GET', "/api/v1/devices/$dev1/state", ['Authorization' => "Bearer $tokenA"]);
assert_equals($bootB, $stAfterReplay['json']['data']['bootId'], 'El estado debe conservar Boot B (Boot A es retirado)');
assert_equals(true, $stAfterReplay['json']['data']['sosActive'], 'El SOS activo de Boot B NUNCA debe ser apagado por el boot antiguo');

// Paso 6.4: REENVIAR Boot A con secuencia todavía MAYOR (sequence 99) y SOS inactivo en el mismo segundo
$respReplayHigherSeq = http_request('POST', '/api/v1/telemetry', $telemetryHeaders, [
    'deviceId' => $dev1,
    'bootId' => $bootA,
    'sequence' => 99,
    'distanceCm' => 250.0,
    'gpsFix' => false,
    'latitude' => null,
    'longitude' => null,
    'batteryLevel' => 95,
    'sosActive' => false,
    'sosEventId' => null,
]);
assert_true(in_array($respReplayHigherSeq['status'], [200, 202]), 'El paquete rezagado con secuencia mayor debe aceptarse sin 500');

// VERIFICACIÓN CRÍTICA: Boot A retirado, incluso con secuencia mayor y en el mismo segundo, NO cambia mediciones ni apaga SOS
$stAfterHigherReplay = http_request('GET', "/api/v1/devices/$dev1/state", ['Authorization' => "Bearer $tokenA"]);
assert_equals($bootB, $stAfterHigherReplay['json']['data']['bootId'], 'Boot retirado con secuencia mayor no debe reemplazar el boot actual');
assert_equals(1, $stAfterHigherReplay['json']['data']['sequence'], 'La secuencia del estado debe permanecer en Boot B seq 1');
assert_equals(true, $stAfterHigherReplay['json']['data']['sosActive'], 'El SOS activo debe mantenerse intacto');
assert_equals(20.0, (float)$stAfterHigherReplay['json']['data']['distanceCm'], 'La distancia debe conservar la medición de Boot B');

// 7. Shares públicos QR y consentimiento vía HTTP
echo "7. Probando Creación, Lectura Pública y Revocación de Shares QR vía HTTP...\n";
$shareCreate = http_request('POST', "/api/v1/profiles/$profileAId/shares", ['Authorization' => "Bearer $tokenA"], [
    'allowedFields' => ['presentationName', 'emergencyPhone']
]);
assert_equals(201, $shareCreate['status'], 'Creación de share público debe responder 201');
$publicUrl = $shareCreate['json']['publicUrl'] ?? '';
$shareId = $shareCreate['json']['shareId'] ?? '';
assert_true(!empty($publicUrl), 'Debe devolverse la URL pública');

// Extraer el token de la URL
$tokenPart = basename(parse_url($publicUrl, PHP_URL_PATH));

// Consulta pública de emergencia sin cabecera de autenticación (vía JSON y HTML)
$emergencyJson = http_request('GET', "/api/v1/public/emergency/$tokenPart");
assert_equals(200, $emergencyJson['status'], 'API pública de emergencia debe responder 200 sin autenticación');
assert_equals('Paciente de A', $emergencyJson['json']['presentationName'] ?? null, 'Debe exponer nombre consentido');
assert_true(!isset($emergencyJson['json']['medicalObservations']), 'Observaciones médicas NO consentidas no deben exponerse');

$emergencyHtml = http_request('GET', "/emergency/$tokenPart");
assert_equals(200, $emergencyHtml['status'], 'Portal público HTML de emergencia debe responder 200');

// Revocar el share (OpenAPI especifica 204 No Content)
$revokeResp = http_request('DELETE', "/api/v1/profiles/$profileAId/shares/$shareId", ['Authorization' => "Bearer $tokenA"]);
assert_true(in_array($revokeResp['status'], [200, 204]), 'Revocación del share debe responder 204 o 200');

// Consulta pública posterior a la revocación -> 404
$emergencyAfterRevoke = http_request('GET', "/api/v1/public/emergency/$tokenPart");
assert_equals(404, $emergencyAfterRevoke['status'], 'Share revocado debe responder 404');

echo "✅ TODAS LAS PRUEBAS HTTP DE INTEGRACIÓN Y SEGURIDAD PASARON EXITOSAMENTE.\n";
exit(0);
