<?php

declare(strict_types=1);

// Front controller de la API v1 y recursos autorizados SmartVest
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('Cache-Control: no-store, private');

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../lib/session.php';
require_once __DIR__ . '/../lib/authorization.php';

use SmartVest\Lib\SessionManager;
use SmartVest\Lib\Authorization;

// Parse URI relativa
$uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?? '/';
$prefix = '/api/v1';
$pos = strpos($uri, $prefix);
$path = ($pos !== false) ? substr($uri, $pos + strlen($prefix)) : $uri;
$path = trim($path, '/');
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

// 1. Liveness check: NO depende de la base de datos (responde 200 inmediato si PHP está vivo)
if (($path === 'health/live' || $uri === '/health/live') && $method === 'GET') {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['status' => 'ok']);
    exit;
}

// 2. Readiness check: DEBE validar conectividad real con la BD y verificar tablas migradas
if (($path === 'health/ready' || $uri === '/health/ready') && $method === 'GET') {
    header('Content-Type: application/json; charset=utf-8');
    try {
        $pdo = get_pdo();
        // Verificar que la base de datos responde y que las tablas de la migración existen
        $stmt = $pdo->query('SELECT COUNT(*) FROM accounts');
        $stmt->fetchColumn();
        echo json_encode(['status' => 'ready', 'database' => 'connected', 'migrations' => 'applied']);
        exit;
    } catch (\Throwable $e) {
        http_response_code(503);
        echo json_encode(['status' => 'error', 'message' => 'Base de datos o migraciones no disponibles: ' . $e->getMessage()]);
        exit;
    }
}

// Inicialización de PDO y servicios para el resto de endpoints
$pdo = get_pdo();
$sessionManager = new SessionManager($pdo);
$auth = new Authorization($pdo);

// Helper para extraer token Bearer
function get_bearer_token(): string {
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (preg_match('/Bearer\s(\S+)/i', $header, $matches)) {
        return $matches[1];
    }
    return '';
}

// ==============================================================================
// 3. PÁGINA PÚBLICA DE EMERGENCIA /emergency/{token} (HTML accesible)
// ==============================================================================
if (preg_match('#^(?:public/)?emergency/([^/]+)$#', $path, $m) || preg_match('#^emergency/([^/]+)$#', trim($uri, '/'), $m)) {
    $shareToken = $m[1];
    $tokenHash = hash('sha256', $shareToken);

    $stmt = $pdo->prepare('
        SELECT ps.allowed_fields, ps.revoked_at, p.presentation_name, p.emergency_phone, p.blood_type, p.medical_observations
        FROM public_shares ps
        JOIN profiles p ON ps.profile_id = p.id
        WHERE ps.token_hash = :hash
    ');
    $stmt->execute(['hash' => $tokenHash]);
    $share = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$share || $share['revoked_at'] !== null) {
        if (str_contains($_SERVER['HTTP_ACCEPT'] ?? '', 'text/html') || !str_contains($path, 'public/emergency')) {
            http_response_code(404);
            header('Content-Type: text/html; charset=utf-8');
            echo '<!DOCTYPE html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>QR No Válido - SmartVest</title><style>body{font-family:sans-serif;padding:24px;text-align:center;background:#f8f9fa;color:#333}h1{color:#c53030}</style></head><body><h1>Enlace de emergencia expirado o revocado</h1><p>Este código QR ya no es válido o fue revocado por el cuidador.</p></body></html>';
            exit;
        } else {
            http_response_code(404);
            header('Content-Type: application/json; charset=utf-8');
            echo json_encode(['error' => ['code' => 'NOT_FOUND', 'message' => 'Token QR no válido o revocado.']]);
            exit;
        }
    }

    $allowed = json_decode($share['allowed_fields'] ?? '[]', true) ?: [];
    $data = [];
    if (in_array('presentationName', $allowed)) $data['presentationName'] = $share['presentation_name'];
    if (in_array('emergencyPhone', $allowed)) $data['emergencyPhone'] = $share['emergency_phone'];
    if (in_array('bloodType', $allowed)) $data['bloodType'] = $share['blood_type'];

    // Si la petición es JSON (/api/v1/public/emergency/{token})
    if (str_contains($path, 'public/emergency') || (isset($_SERVER['HTTP_ACCEPT']) && str_contains($_SERVER['HTTP_ACCEPT'], 'application/json'))) {
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode($data);
        exit;
    }

    // Si es acceso web directo (/emergency/{token})
    header('Content-Type: text/html; charset=utf-8');
    $safeName = htmlspecialchars($data['presentationName'] ?? 'No especificado', ENT_QUOTES, 'UTF-8');
    $safePhone = htmlspecialchars($data['emergencyPhone'] ?? 'No especificado', ENT_QUOTES, 'UTF-8');
    $safeBlood = htmlspecialchars($data['bloodType'] ?? 'No especificado', ENT_QUOTES, 'UTF-8');
    $phoneLink = isset($data['emergencyPhone']) ? '<a href="tel:' . rawurlencode($data['emergencyPhone']) . '" style="display:inline-block;background:#0d6efd;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:bold;margin-top:8px">Llamar a Emergencias: ' . $safePhone . '</a>' : '<p>Sin teléfono registrado</p>';

    echo "<!DOCTYPE html>
<html lang=\"es\">
<head>
  <meta charset=\"utf-8\">
  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
  <title>Emergencia SmartVest</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin:0; padding:20px; background:#f4f6f8; color:#1a202c; line-height:1.5; }
    .card { max-width:480px; margin:20px auto; background:#fff; border-radius:12px; box-shadow:0 4px 12px rgba(0,0,0,0.08); padding:24px; border-top:6px solid #dc3545; }
    h1 { color:#dc3545; font-size:24px; margin-top:0; }
    .field { margin-bottom:16px; }
    .label { font-size:12px; text-transform:uppercase; color:#6c757d; font-weight:bold; }
    .value { font-size:18px; font-weight:600; color:#111; }
  </style>
</head>
<body>
  <div class=\"card\">
    <h1>Ficha de Emergencia Médica</h1>
    <div class=\"field\"><div class=\"label\">Nombre Asistido</div><div class=\"value\">{$safeName}</div></div>
    <div class=\"field\"><div class=\"label\">Grupo Sanguíneo</div><div class=\"value\">{$safeBlood}</div></div>
    <div class=\"field\"><div class=\"label\">Contacto de Emergencia</div><div>{$phoneLink}</div></div>
    <p style=\"font-size:11px;color:#888;margin-top:24px\">SmartVest · Sistema de Asistencia Vital</p>
  </div>
</body>
</html>";
    exit;
}

// Para el resto de endpoints se responde JSON
header('Content-Type: application/json; charset=utf-8');

// ==============================================================================
// 4. AUTENTICACIÓN: REGISTER / LOGIN / LOGOUT / ME
// ==============================================================================
if ($path === 'auth/register' && $method === 'POST') {
    $data = read_json_body();
    $username = trim((string)($data['username'] ?? ''));
    $password = (string)($data['password'] ?? '');

    if (strlen($username) < 3 || strlen($password) < 8) {
        http_response_code(400);
        echo json_encode(['error' => ['code' => 'INVALID_PAYLOAD', 'message' => 'Usuario (min 3) o contraseña (min 8) no válidos.']]);
        exit;
    }

    $stmt = $pdo->prepare('SELECT id FROM accounts WHERE username = :u');
    $stmt->execute(['u' => $username]);
    if ($stmt->fetch()) {
        http_response_code(409);
        echo json_encode(['error' => ['code' => 'USER_EXISTS', 'message' => 'El nombre de usuario ya está registrado.']]);
        exit;
    }

    $id = bin2hex(random_bytes(16));
    $hash = password_hash($password, PASSWORD_BCRYPT);
    $ins = $pdo->prepare('INSERT INTO accounts (id, username, password_hash, status) VALUES (:id, :u, :p, "active")');
    $ins->execute(['id' => $id, 'u' => $username, 'p' => $hash]);

    http_response_code(201);
    echo json_encode(['success' => true, 'id' => $id, 'username' => $username]);
    exit;
}

if ($path === 'auth/login' && $method === 'POST') {
    $data = read_json_body();
    $username = trim((string)($data['username'] ?? ''));
    $password = (string)($data['password'] ?? '');

    $stmt = $pdo->prepare('SELECT id, password_hash, status FROM accounts WHERE username = :u');
    $stmt->execute(['u' => $username]);
    $account = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$account || !password_verify($password, $account['password_hash']) || $account['status'] !== 'active') {
        http_response_code(401);
        echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Credenciales inválidas.']]);
        exit;
    }

    $token = $sessionManager->createSession($account['id']);
    echo json_encode([
        'token' => $token,
        'expiresAt' => gmdate('Y-m-d\TH:i:s\Z', time() + 43200),
    ]);
    exit;
}

if ($path === 'auth/logout' && $method === 'POST') {
    $token = get_bearer_token();
    if ($token !== '') {
        $sessionManager->revokeSession($token);
    }
    http_response_code(204);
    exit;
}

if ($path === 'me' && $method === 'GET') {
    $user = $sessionManager->authenticate(get_bearer_token());
    if (!$user) {
        http_response_code(401);
        echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Sesión inválida o expirada.']]);
        exit;
    }
    echo json_encode(['id' => $user['accountId'], 'username' => $user['username']]);
    exit;
}

// ==============================================================================
// 5. PERFILES: LISTAR / CREAR / OBTENER / ACTUALIZAR / BORRAR
// ==============================================================================
if ($path === 'profiles' && $method === 'GET') {
    $user = $sessionManager->authenticate(get_bearer_token());
    if (!$user) {
        http_response_code(401);
        echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Sesión requerida.']]);
        exit;
    }

    $stmt = $pdo->prepare('SELECT id, presentation_name, emergency_phone, blood_type, medical_observations, device_id FROM profiles WHERE owner_account_id = :owner ORDER BY created_at DESC');
    $stmt->execute(['owner' => $user['accountId']]);
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $items = array_map(function($r) {
        return [
            'id' => $r['id'],
            'presentationName' => $r['presentation_name'],
            'emergencyPhone' => $r['emergency_phone'],
            'bloodType' => $r['blood_type'],
            'medicalObservations' => $r['medical_observations'],
            'deviceId' => $r['device_id'],
        ];
    }, $rows);

    echo json_encode(['data' => $items]);
    exit;
}

if ($path === 'profiles' && $method === 'POST') {
    $user = $sessionManager->authenticate(get_bearer_token());
    if (!$user) {
        http_response_code(401);
        echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Sesión requerida.']]);
        exit;
    }

    $data = read_json_body();
    $name = trim((string)($data['presentationName'] ?? ''));
    if ($name === '') {
        http_response_code(400);
        echo json_encode(['error' => ['code' => 'VALIDATION_ERROR', 'message' => 'presentationName es obligatorio.']]);
        exit;
    }

    $id = bin2hex(random_bytes(16));
    $stmt = $pdo->prepare('
        INSERT INTO profiles (id, owner_account_id, presentation_name, emergency_phone, blood_type, medical_observations)
        VALUES (:id, :owner, :name, :phone, :blood, :obs)
    ');
    $stmt->execute([
        'id' => $id,
        'owner' => $user['accountId'],
        'name' => $name,
        'phone' => $data['emergencyPhone'] ?? null,
        'blood' => $data['bloodType'] ?? null,
        'obs' => $data['medicalObservations'] ?? null,
    ]);

    http_response_code(201);
    echo json_encode(['id' => $id, 'presentationName' => $name]);
    exit;
}

// Rutas individuales /profiles/{id}
if (preg_match('#^profiles/([^/]+)$#', $path, $m)) {
    $user = $sessionManager->authenticate(get_bearer_token());
    if (!$user) {
        http_response_code(401);
        echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Sesión requerida.']]);
        exit;
    }
    $profileId = $m[1];
    $profile = $auth->requireProfileOwner($profileId, $user['accountId']);

    if ($method === 'GET') {
        echo json_encode([
            'id' => $profile['id'],
            'presentationName' => $profile['presentation_name'],
            'emergencyPhone' => $profile['emergency_phone'],
            'bloodType' => $profile['blood_type'],
            'medicalObservations' => $profile['medical_observations'],
            'deviceId' => $profile['device_id'],
        ]);
        exit;
    }

    if ($method === 'PATCH') {
        $data = read_json_body();
        $fields = [];
        $params = ['id' => $profileId, 'owner' => $user['accountId']];

        if (isset($data['presentationName'])) {
            $fields[] = 'presentation_name = :pname';
            $params['pname'] = trim((string)$data['presentationName']);
        }
        if (isset($data['emergencyPhone'])) {
            $fields[] = 'emergency_phone = :phone';
            $params['phone'] = trim((string)$data['emergencyPhone']);
        }
        if (isset($data['bloodType'])) {
            $fields[] = 'blood_type = :blood';
            $params['blood'] = trim((string)$data['bloodType']);
        }
        if (isset($data['medicalObservations'])) {
            $fields[] = 'medical_observations = :obs';
            $params['obs'] = (string)$data['medicalObservations'];
        }

        if (!empty($fields)) {
            $sql = 'UPDATE profiles SET ' . implode(', ', $fields) . ' WHERE id = :id AND owner_account_id = :owner';
            $stmt = $pdo->prepare($sql);
            $stmt->execute($params);
        }

        echo json_encode(['success' => true]);
        exit;
    }

    if ($method === 'DELETE') {
        $stmt = $pdo->prepare('DELETE FROM profiles WHERE id = :id AND owner_account_id = :owner');
        $stmt->execute(['id' => $profileId, 'owner' => $user['accountId']]);
        http_response_code(204);
        exit;
    }
}

// Vinculación de chaleco (POST /profiles/{id}/device)
if (preg_match('#^profiles/([^/]+)/device$#', $path, $m) && $method === 'POST') {
    $user = $sessionManager->authenticate(get_bearer_token());
    if (!$user) {
        http_response_code(401);
        echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Sesión requerida.']]);
        exit;
    }
    $profileId = $m[1];
    $auth->requireProfileOwner($profileId, $user['accountId']);

    $data = read_json_body();
    $deviceId = trim((string)($data['deviceId'] ?? ''));
    $pairingCode = trim((string)($data['pairingCode'] ?? ''));

    if ($deviceId === '' || $pairingCode === '') {
        http_response_code(400);
        echo json_encode(['error' => ['code' => 'VALIDATION_ERROR', 'message' => 'deviceId y pairingCode son requeridos.']]);
        exit;
    }

    $codeHash = hash('sha256', $pairingCode);
    $pdo->beginTransaction();

    try {
        $stmt = $pdo->prepare('
            SELECT * FROM pairing_codes
            WHERE code_hash = :hash AND device_id = :dev AND used_at IS NULL AND expires_at > UTC_TIMESTAMP()
            FOR UPDATE
        ');
        $stmt->execute(['hash' => $codeHash, 'dev' => $deviceId]);
        $validCode = $stmt->fetch();

        if (!$validCode) {
            $pdo->rollBack();
            http_response_code(400);
            echo json_encode(['error' => ['code' => 'INVALID_CODE', 'message' => 'Código de vinculación inválido o expirado.']]);
            exit;
        }

        // Verificar si el dispositivo ya está asignado
        $chk = $pdo->prepare('SELECT id FROM profiles WHERE device_id = :dev');
        $chk->execute(['dev' => $deviceId]);
        if ($chk->fetch()) {
            $pdo->rollBack();
            http_response_code(409);
            echo json_encode(['error' => ['code' => 'ALREADY_ASSIGNED', 'message' => 'El dispositivo ya está vinculado a un perfil.']]);
            exit;
        }

        // Quemar pairing code y asignar
        $burn = $pdo->prepare('UPDATE pairing_codes SET used_at = UTC_TIMESTAMP() WHERE code_hash = :hash');
        $burn->execute(['hash' => $codeHash]);

        $assign = $pdo->prepare('UPDATE profiles SET device_id = :dev WHERE id = :id');
        $assign->execute(['dev' => $deviceId, 'id' => $profileId]);

        $pdo->commit();
        echo json_encode(['success' => true, 'deviceId' => $deviceId]);
        exit;
    } catch (\Throwable $e) {
        $pdo->rollBack();
        http_response_code(500);
        echo json_encode(['error' => ['code' => 'SERVER_ERROR', 'message' => 'Error durante la vinculación']]);
        exit;
    }
}

// ==============================================================================
// 6. SHARES QR CONSENTIDO: CREAR Y REVOCAR
// ==============================================================================
if (preg_match('#^profiles/([^/]+)/shares$#', $path, $m) && $method === 'POST') {
    $user = $sessionManager->authenticate(get_bearer_token());
    if (!$user) {
        http_response_code(401);
        echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Sesión requerida.']]);
        exit;
    }
    $profileId = $m[1];
    $auth->requireProfileOwner($profileId, $user['accountId']);

    $data = read_json_body();
    $allowedFields = $data['allowedFields'] ?? ['presentationName', 'emergencyPhone', 'bloodType'];
    if (!is_array($allowedFields)) {
        http_response_code(400);
        echo json_encode(['error' => ['code' => 'INVALID_PAYLOAD', 'message' => 'allowedFields debe ser una lista.']]);
        exit;
    }

    $shareId = bin2hex(random_bytes(16));
    $token = bin2hex(random_bytes(32)); // Token opaco de 64 hex
    $tokenHash = hash('sha256', $token);

    $stmt = $pdo->prepare('
        INSERT INTO public_shares (id, token_hash, profile_id, allowed_fields, created_at)
        VALUES (:id, :thash, :pid, :fields, UTC_TIMESTAMP())
    ');
    $stmt->execute([
        'id' => $shareId,
        'thash' => $tokenHash,
        'pid' => $profileId,
        'fields' => json_encode(array_values($allowedFields)),
    ]);

    $host = $_SERVER['HTTP_HOST'] ?? 'smartvest-lab.ajenzasas.com';
    $scheme = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on') ? 'https' : 'https';
    $publicUrl = "{$scheme}://{$host}/emergency/{$token}";

    http_response_code(201);
    echo json_encode([
        'shareId' => $shareId,
        'token' => $token,
        'publicUrl' => $publicUrl,
    ]);
    exit;
}

if (preg_match('#^profiles/([^/]+)/shares/([^/]+)$#', $path, $m) && $method === 'DELETE') {
    $user = $sessionManager->authenticate(get_bearer_token());
    if (!$user) {
        http_response_code(401);
        echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Sesión requerida.']]);
        exit;
    }
    $profileId = $m[1];
    $shareId = $m[2];
    $auth->requireProfileOwner($profileId, $user['accountId']);

    $stmt = $pdo->prepare('UPDATE public_shares SET revoked_at = UTC_TIMESTAMP() WHERE id = :sid AND profile_id = :pid');
    $stmt->execute(['sid' => $shareId, 'pid' => $profileId]);

    http_response_code(204);
    exit;
}

// ==============================================================================
// 7. INGESTIÓN DE TELEMETRÍA FÍSICA TRANSACCIONAL Y MONOTÓNICA
// ==============================================================================
if (preg_match('#^devices/([^/]+)/telemetry$#', $path, $m) && $method === 'POST') {
    $deviceId = $m[1];
    $apiKey = $_SERVER['HTTP_X_SMARTVEST_API_KEY'] ?? '';

    // Validar credencial del dispositivo
    $stmt = $pdo->prepare('SELECT credential_hash FROM devices WHERE id = :id AND status = "active"');
    $stmt->execute(['id' => $deviceId]);
    $dev = $stmt->fetch();

    if (!$dev || hash('sha256', $apiKey) !== $dev['credential_hash']) {
        http_response_code(401);
        echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Clave de dispositivo inválida.']]);
        exit;
    }

    $data = read_json_body();
    $bootId = trim((string)($data['bootId'] ?? ''));
    $seq = isset($data['sequence']) ? (int)$data['sequence'] : -1;
    $sos = !empty($data['sosActive']);
    $sosEventId = isset($data['sosEventId']) ? trim((string)$data['sosEventId']) : null;

    if ($bootId === '' || $seq < 0) {
        http_response_code(400);
        echo json_encode(['error' => ['code' => 'INVALID_PAYLOAD', 'message' => 'bootId y sequence (>= 0) son obligatorios.']]);
        exit;
    }

    $distance = isset($data['distanceCm']) ? (float)$data['distanceCm'] : null;
    $gpsFix = !empty($data['gpsFix']);
    $lat = $gpsFix && isset($data['latitude']) ? (float)$data['latitude'] : null;
    $lng = $gpsFix && isset($data['longitude']) ? (float)$data['longitude'] : null;
    $battery = isset($data['batteryLevel']) ? (int)$data['batteryLevel'] : null;

    $pdo->beginTransaction();

    try {
        // 1. Guardar en telemetry_history (idempotente por device_id, boot_id, sequence)
        $isDuplicate = false;
        try {
            $hist = $pdo->prepare('
                INSERT INTO telemetry_history (device_id, boot_id, sequence, distance_cm, gps_fix, latitude, longitude, battery_level, sos_active, received_at)
                VALUES (:dev, :boot, :seq, :dist, :fix, :lat, :lng, :bat, :sos, UTC_TIMESTAMP())
            ');
            $hist->execute([
                'dev' => $deviceId,
                'boot' => $bootId,
                'seq' => $seq,
                'dist' => $distance,
                'fix' => $gpsFix ? 1 : 0,
                'lat' => $lat,
                'lng' => $lng,
                'bat' => $battery,
                'sos' => $sos ? 1 : 0,
            ]);
        } catch (\PDOException $e) {
            if ($e->getCode() == 23000) {
                // Duplicado exacto de paquete ya procesado
                $isDuplicate = true;
            } else {
                throw $e;
            }
        }

        // 2. Transacción de estado monotónico con SELECT ... FOR UPDATE
        $stateStmt = $pdo->prepare('SELECT boot_id, sequence, sos_active FROM telemetry_state WHERE device_id = :dev FOR UPDATE');
        $stateStmt->execute(['dev' => $deviceId]);
        $currentState = $stateStmt->fetch(PDO::FETCH_ASSOC);

        $shouldUpdateState = false;
        if (!$currentState) {
            // Primer reporte del dispositivo
            $shouldUpdateState = true;
        } else {
            $currBoot = (string)$currentState['boot_id'];
            $currSeq = (int)$currentState['sequence'];
            $currSos = (bool)$currentState['sos_active'];

            // Política de actualización monotónica:
            // - Mismo boot: solo actualizar si la secuencia es estrictamente mayor
            // - Diferente boot: reinicio físico del ESP32 -> aceptar nuevo boot si sequence >= 0
            if ($bootId === $currBoot) {
                if ($seq > $currSeq) {
                    $shouldUpdateState = true;
                } elseif ($sos && !$currSos) {
                    // Si el paquete trae una alerta SOS y el estado previo no la tenía, nunca apagar/perder el SOS
                    $shouldUpdateState = true;
                }
            } else {
                // Nuevo ciclo de boot
                $shouldUpdateState = true;
            }
        }

        if ($shouldUpdateState) {
            $upsert = $pdo->prepare('
                INSERT INTO telemetry_state (device_id, boot_id, sequence, distance_cm, gps_fix, latitude, longitude, battery_level, sos_active, received_at)
                VALUES (:dev, :boot, :seq, :dist, :fix, :lat, :lng, :bat, :sos, UTC_TIMESTAMP())
                ON DUPLICATE KEY UPDATE
                    boot_id = VALUES(boot_id),
                    sequence = VALUES(sequence),
                    distance_cm = VALUES(distance_cm),
                    gps_fix = VALUES(gps_fix),
                    latitude = VALUES(latitude),
                    longitude = VALUES(longitude),
                    battery_level = VALUES(battery_level),
                    sos_active = VALUES(sos_active),
                    received_at = UTC_TIMESTAMP()
            ');
            $upsert->execute([
                'dev' => $deviceId,
                'boot' => $bootId,
                'seq' => $seq,
                'dist' => $distance,
                'fix' => $gpsFix ? 1 : 0,
                'lat' => $lat,
                'lng' => $lng,
                'bat' => $battery,
                'sos' => $sos ? 1 : 0,
            ]);
        }

        // 3. Registrar evento SOS si viene activo
        if ($sos && $sosEventId) {
            $sosStmt = $pdo->prepare('
                INSERT IGNORE INTO sos_events (id, device_id, event_id, status, received_at)
                VALUES (:id, :dev, :evt, "active", UTC_TIMESTAMP())
            ');
            $sosStmt->execute([
                'id' => bin2hex(random_bytes(16)),
                'dev' => $deviceId,
                'evt' => $sosEventId,
            ]);
        }

        $pdo->commit();

        if ($isDuplicate) {
            http_response_code(200);
            echo json_encode(['status' => 'duplicate', 'message' => 'Telemetría duplicada ya procesada']);
            exit;
        }

        http_response_code(202);
        echo json_encode(['status' => 'accepted']);
        exit;
    } catch (\Throwable $e) {
        $pdo->rollBack();
        http_response_code(500);
        echo json_encode(['error' => ['code' => 'TRANSACTION_ERROR', 'message' => 'Error al persistir telemetría: ' . $e->getMessage()]]);
        exit;
    }
}

// ==============================================================================
// 8. CONSULTA DE ESTADO E HISTORIAL
// ==============================================================================
if (preg_match('#^devices/([^/]+)/state$#', $path, $m) && $method === 'GET') {
    $user = $sessionManager->authenticate(get_bearer_token());
    if (!$user) {
        http_response_code(401);
        echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Sesión requerida.']]);
        exit;
    }
    $deviceId = $m[1];
    $auth->requireDeviceOwner($deviceId, $user['accountId']);

    $stmt = $pdo->prepare('SELECT * FROM telemetry_state WHERE device_id = :dev');
    $stmt->execute(['dev' => $deviceId]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$row) {
        echo json_encode(['data' => null]);
        exit;
    }

    $lastUpdateMs = (int)(strtotime($row['received_at'] . ' UTC') * 1000);
    echo json_encode([
        'data' => [
            'deviceId' => $row['device_id'],
            'distanceCm' => $row['distance_cm'] !== null ? (float)$row['distance_cm'] : null,
            'gpsFix' => (bool)$row['gps_fix'],
            'latitude' => $row['latitude'] !== null ? (float)$row['latitude'] : null,
            'longitude' => $row['longitude'] !== null ? (float)$row['longitude'] : null,
            'batteryLevel' => $row['battery_level'] !== null ? (int)$row['battery_level'] : null,
            'sosActive' => (bool)$row['sos_active'],
            'lastUpdate' => $lastUpdateMs,
            'bootId' => $row['boot_id'],
            'sequence' => (int)$row['sequence'],
        ]
    ]);
    exit;
}

if (preg_match('#^devices/([^/]+)/history$#', $path, $m) && $method === 'GET') {
    $user = $sessionManager->authenticate(get_bearer_token());
    if (!$user) {
        http_response_code(401);
        echo json_encode(['error' => ['code' => 'UNAUTHORIZED', 'message' => 'Sesión requerida.']]);
        exit;
    }
    $deviceId = $m[1];
    $auth->requireDeviceOwner($deviceId, $user['accountId']);

    $limit = min(120, max(1, (int)($_GET['limit'] ?? 20)));
    $stmt = $pdo->prepare('
        SELECT * FROM telemetry_history
        WHERE device_id = :dev
        ORDER BY received_at DESC, sequence DESC
        LIMIT :lim
    ');
    $stmt->bindValue(':dev', $deviceId, PDO::PARAM_STR);
    $stmt->bindValue(':lim', $limit, PDO::PARAM_INT);
    $stmt->execute();
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $items = array_map(function($r) {
        return [
            'id' => (int)$r['id'],
            'deviceId' => $r['device_id'],
            'distanceCm' => $r['distance_cm'] !== null ? (float)$r['distance_cm'] : null,
            'gpsFix' => (bool)$r['gps_fix'],
            'latitude' => $r['latitude'] !== null ? (float)$r['latitude'] : null,
            'longitude' => $r['longitude'] !== null ? (float)$r['longitude'] : null,
            'batteryLevel' => $r['battery_level'] !== null ? (int)$r['battery_level'] : null,
            'sosActive' => (bool)$r['sos_active'],
            'recordedAt' => gmdate('Y-m-d\TH:i:s\Z', strtotime($r['received_at'] . ' UTC')),
        ];
    }, $rows);

    echo json_encode(['data' => $items]);
    exit;
}

// Ruta no encontrada
http_response_code(404);
echo json_encode(['error' => ['code' => 'NOT_FOUND', 'message' => 'Ruta no encontrada en API v1.']]);
exit;
