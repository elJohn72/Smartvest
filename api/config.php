<?php

declare(strict_types=1);

const DB_HOST = '127.0.0.1';
const DB_PORT = '3306';
const DB_NAME = 'smartvest';
const DB_USER = 'root';
const DB_PASSWORD = '';

function json_response(array $payload, int $status = 200): never
{
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

function normalize_user_row(array $row): array
{
    return [
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
        'password' => $row['password'] ?: null,
        'deviceId' => $row['device_id'] ?: null,
    ];
}
