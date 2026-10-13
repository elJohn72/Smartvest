<?php

declare(strict_types=1);

// Front controller de la API v1
header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('Cache-Control: no-store, private');

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../lib/session.php';
require_once __DIR__ . '/../lib/authorization.php';

use SmartVest\Lib\SessionManager;
use SmartVest\Lib\Authorization;

$pdo = get_pdo();
$sessionManager = new SessionManager($pdo);
$auth = new Authorization($pdo);

// Parse URI relativa a /api/v1
$uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$prefix = '/api/v1';
$pos = strpos($uri, $prefix);
$path = ($pos !== false) ? substr($uri, $pos + strlen($prefix)) : $uri;
$path = trim($path, '/');
$method = $_SERVER['REQUEST_METHOD'];

// Helper para extraer token Bearer
function get_bearer_token(): string {
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (preg_match('/Bearer\s(\S+)/i', $header, $matches)) {
        return $matches[1];
    }
    return '';
}

// Rutas de salud
if ($path === 'health/live' && $method === 'GET') {
    echo json_encode(['status' => 'ok']);
    exit;
}

if ($path === 'health/ready' && $method === 'GET') {
    try {
        $pdo->query('SELECT 1');
        echo json_encode(['status' => 'ready', 'database' => 'connected']);
    } catch (\Throwable $e) {
        http_response_code(503);
        echo json_encode(['status' => 'error', 'message' => 'Base de datos no disponible']);
    }
    exit;
}

// Autenticación: Register
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

// Autenticación: Login
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

// Autenticación: Logout
if ($path === 'auth/logout' && $method === 'POST') {
    $token = get_bearer_token();
    if ($token !== '') {
        $sessionManager->revokeSession($token);
    }
    http_response_code(204);
    exit;
}

// Todas las rutas siguientes requieren sesión activa (salvo ingestión y QR público)
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

// Profiles endpoints
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

// Perfil individual
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

    $codeHash = hash('sha256', $pairingCode);
    $pdo->beginTransaction();

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
}

// Ingestión de telemetría física (POST /devices/{deviceId}/telemetry)
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
    $bootId = (string)($data['bootId'] ?? '');
    $seq = (int)($data['sequence'] ?? 0);
    $distance = isset($data['distanceCm']) ? (float)$data['distanceCm'] : null;
    $gpsFix = !empty($data['gpsFix']);
    $lat = $gpsFix && isset($data['latitude']) ? (float)$data['latitude'] : null;
    $lng = $gpsFix && isset($data['longitude']) ? (float)$data['longitude'] : null;
    $battery = isset($data['batteryLevel']) ? (int)$data['batteryLevel'] : null;
    $sos = !empty($data['sosActive']);
    $sosEventId = $data['sosEventId'] ?? null;

    // Insertar en historial con deduplicación
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
        if ($e->getCode() == 23000) { // Duplicate key
            http_response_code(200);
            echo json_encode(['status' => 'duplicate', 'message' => 'Telemetría ya procesada']);
            exit;
        }
        throw $e;
    }

    // Actualizar estado monotónico si secuencia es más reciente
    $upsert = $pdo->prepare('
        INSERT INTO telemetry_state (device_id, boot_id, sequence, distance_cm, gps_fix, latitude, longitude, battery_level, sos_active, received_at)
        VALUES (:dev, :boot, :seq, :dist, :fix, :lat, :lng, :bat, :sos, UTC_TIMESTAMP())
        ON DUPLICATE KEY UPDATE
            distance_cm = VALUES(distance_cm),
            gps_fix = VALUES(gps_fix),
            latitude = VALUES(latitude),
            longitude = VALUES(longitude),
            battery_level = VALUES(battery_level),
            sos_active = VALUES(sos_active),
            boot_id = VALUES(boot_id),
            sequence = VALUES(sequence),
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

    // Registrar evento SOS si viene activo
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

    http_response_code(202);
    echo json_encode(['status' => 'accepted']);
    exit;
}

// Consultar estado de dispositivo (GET /devices/{deviceId}/state)
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

// Consultar historial de telemetría (GET /devices/{deviceId}/history)
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

// Endpoint no encontrado
http_response_code(404);
echo json_encode(['error' => ['code' => 'NOT_FOUND', 'message' => 'Ruta no encontrada en API v1.']]);
exit;
