<?php

declare(strict_types=1);

const DB_HOST = '127.0.0.1';
const DB_PORT = '3306';
const DB_NAME = 'smartvest';
const DB_USER = 'root';
const DB_PASSWORD = '';

/** Clave por defecto solo para desarrollo local; sobrescribe en config.local.php */
const SMARTVEST_IOT_API_KEY_DEFAULT = 'smartvest-local-dev-key';

$localConfigPath = __DIR__ . '/config.local.php';
if (is_file($localConfigPath)) {
    require $localConfigPath;
}

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

    try {
        $pdo = new PDO(
            'mysql:host=' . DB_HOST . ';port=' . DB_PORT . ';dbname=' . DB_NAME . ';charset=utf8mb4',
            DB_USER,
            DB_PASSWORD,
            [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            ]
        );
    } catch (PDOException $exception) {
        json_response([
            'success' => false,
            'message' => 'No se pudo conectar a MariaDB. Revisa api/config.php y crea la base smartvest.',
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
