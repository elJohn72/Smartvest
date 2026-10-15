<?php

declare(strict_types=1);

function get_db_config(): array
{
    // Soporte prioritario para variables de entorno (Compose / Coolify / PHP-FPM)
    $host = getenv('DB_HOST') ?: (defined('DB_HOST') ? DB_HOST : null);
    $port = getenv('DB_PORT') ?: (defined('DB_PORT') ? DB_PORT : '3306');
    $name = getenv('DB_NAME') ?: (defined('DB_NAME') ? DB_NAME : null);
    $user = getenv('DB_USER') ?: (defined('DB_USER') ? DB_USER : null);
    $pass = getenv('DB_PASSWORD') !== false ? getenv('DB_PASSWORD') : (defined('DB_PASSWORD') ? DB_PASSWORD : null);

    // Si falta alguna configuración obligatoria en producción/lab, no usar valores predeterminados inseguros
    if ($host === null || $name === null || $user === null || $pass === null) {
        // En entorno local estricto solo permitir si config.local.php las definió explícitamente
        if (defined('SMARTVEST_ALLOW_LOCAL_DEV') && SMARTVEST_ALLOW_LOCAL_DEV === true) {
            $host = $host ?? '127.0.0.1';
            $port = $port ?? '3306';
            $name = $name ?? 'smartvest';
            $user = $user ?? 'root';
            $pass = $pass ?? '';
        } else {
            json_response([
                'success' => false,
                'message' => 'Configuración de base de datos incompleta. Faltan variables de entorno DB_HOST, DB_NAME, DB_USER o DB_PASSWORD.',
            ], 500);
        }
    }

    return [
        'host' => (string)$host,
        'port' => (string)$port,
        'name' => (string)$name,
        'user' => (string)$user,
        'pass' => (string)$pass,
    ];
}

/** Clave por defecto solo para desarrollo local; sobrescribe en config.local.php */
const SMARTVEST_IOT_API_KEY_DEFAULT = 'smartvest-local-dev-key';

$localConfigPath = __DIR__ . '/config.local.php';
if (is_file($localConfigPath)) {
    require $localConfigPath;
}

require_once __DIR__ . '/lib/cache_aside.php';
require_once __DIR__ . '/lib/job_queue.php';
require_once __DIR__ . '/lib/auth_token.php';

/** TTL cache-aside (segundos) — lecturas frecuentes de listados / IoT */
const SMARTVEST_CACHE_TTL_USERS = 30;
const SMARTVEST_CACHE_TTL_IOT = 5;

function send_security_headers(): void
{
    header('X-Content-Type-Options: nosniff');
    header('X-Frame-Options: SAMEORIGIN');
    header('Referrer-Policy: strict-origin-when-cross-origin');
}

function json_response(array $payload, int $status = 200): never
{
    send_security_headers();
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function read_json_body(): array
{
    $rawBody = file_get_contents('php://input');
    if ($rawBody === false || $rawBody === '') {
        return [];
    }

    $data = json_decode($rawBody, true);
    if (!is_array($data)) {
        json_response(['success' => false, 'message' => 'JSON inválido.'], 400);
    }

    return $data;
}

function get_pdo(): PDO
{
    static $pdo = null;

    if ($pdo instanceof PDO) {
        return $pdo;
    }

    $cfg = get_db_config();

    try {
        $pdo = new PDO(
            'mysql:host=' . $cfg['host'] . ';port=' . $cfg['port'] . ';dbname=' . $cfg['name'] . ';charset=utf8mb4',
            $cfg['user'],
            $cfg['pass'],
            [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            ]
        );
    } catch (PDOException $exception) {
        json_response([
            'success' => false,
            'message' => 'No se pudo conectar a MariaDB. Verifique las credenciales y conectividad con la base de datos.',
            'error' => $exception->getMessage(),
        ], 500);
    }

    return $pdo;
}

function get_iot_api_key(): string
{
    if (defined('SMARTVEST_IOT_API_KEY') && SMARTVEST_IOT_API_KEY !== '') {
        return SMARTVEST_IOT_API_KEY;
    }

    $fromEnv = getenv('SMARTVEST_IOT_API_KEY');
    if (is_string($fromEnv) && $fromEnv !== '') {
        return $fromEnv;
    }

    return SMARTVEST_IOT_API_KEY_DEFAULT;
}

function get_gemini_api_key(): string
{
    if (defined('SMARTVEST_GEMINI_API_KEY') && SMARTVEST_GEMINI_API_KEY !== '') {
        return SMARTVEST_GEMINI_API_KEY;
    }

    $fromEnv = getenv('GEMINI_API_KEY') ?: getenv('SMARTVEST_GEMINI_API_KEY');
    if (is_string($fromEnv) && $fromEnv !== '') {
        return $fromEnv;
    }

    return '';
}

function get_groq_api_key(): string
{
    if (defined('SMARTVEST_GROQ_API_KEY') && SMARTVEST_GROQ_API_KEY !== '') {
        return SMARTVEST_GROQ_API_KEY;
    }

    $fromEnv = getenv('GROQ_API_KEY') ?: getenv('SMARTVEST_GROQ_API_KEY');
    if (is_string($fromEnv) && $fromEnv !== '') {
        return $fromEnv;
    }

    return '';
}

function get_groq_model(): string
{
    if (defined('SMARTVEST_GROQ_MODEL') && SMARTVEST_GROQ_MODEL !== '') {
        return SMARTVEST_GROQ_MODEL;
    }

    $fromEnv = getenv('SMARTVEST_GROQ_MODEL');
    if (is_string($fromEnv) && $fromEnv !== '') {
        return $fromEnv;
    }

    return 'llama-3.3-70b-versatile';
}

function require_iot_api_key(): void
{
    $expected = get_iot_api_key();
    $provided = (string) ($_SERVER['HTTP_X_SMARTVEST_API_KEY'] ?? '');

    if ($provided === '' || !hash_equals($expected, $provided)) {
        json_response(['success' => false, 'message' => 'No autorizado.'], 401);
    }
}

function hash_password(string $password): string
{
    return password_hash($password, PASSWORD_DEFAULT);
}

function verify_password(string $storedPassword, string $plainPassword): bool
{
    if ($storedPassword === '') {
        return false;
    }

    if (str_starts_with($storedPassword, '$2y$') || str_starts_with($storedPassword, '$argon2')) {
        return password_verify($plainPassword, $storedPassword);
    }

    return hash_equals($storedPassword, $plainPassword);
}

function prepare_password_for_storage(?string $password, ?string $existingHash = null): ?string
{
    if ($password === null || $password === '') {
        return $existingHash;
    }

    if (
        $existingHash !== null
        && $existingHash !== ''
        && (str_starts_with($existingHash, '$2y$') || str_starts_with($existingHash, '$argon2'))
        && password_verify($password, $existingHash)
    ) {
        return $existingHash;
    }

    return hash_password($password);
}

function normalize_user_row(array $row, bool $includePassword = false): array
{
    $user = [
        'id' => $row['id'],
        'fullName' => $row['full_name'],
        'nationalId' => $row['national_id'] ?? '',
        'age' => (int) ($row['age'] ?? 0),
        'bloodType' => $row['blood_type'] ?? '',
        'address' => $row['address'] ?? '',
        'emergencyPhone' => $row['emergency_phone'] ?? '',
        'emergencyContact' => json_decode($row['emergency_contact'] ?? '{}', true) ?: [
            'name' => '',
            'relationship' => '',
            'phone' => '',
        ],
        'medicalObservations' => $row['medical_observations'] ?? '',
        'createdAt' => $row['created_at'] ?? '',
        'photo' => $row['photo'] ?: null,
        'username' => $row['username'] ?: null,
        'deviceId' => $row['device_id'] ?: null,
    ];

    if ($includePassword) {
        $user['password'] = $row['password'] ?: null;
    }

    return $user;
}

function public_user_row(array $row): array
{
    return normalize_user_row($row, false);
}
