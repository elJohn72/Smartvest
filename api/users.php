<?php

declare(strict_types=1);

require __DIR__ . '/config.php';

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
$pdo = get_pdo();

if ($method === 'GET') {
    $id = $_GET['id'] ?? null;

    if ($id) {
        $statement = $pdo->prepare('SELECT * FROM users WHERE id = :id LIMIT 1');
        $statement->execute(['id' => $id]);
        $user = $statement->fetch();

        json_response([
            'success' => true,
            'user' => $user ? public_user_row($user) : null,
        ]);
    }

    $statement = $pdo->query('SELECT * FROM users ORDER BY created_at DESC');
    $users = array_map('public_user_row', $statement->fetchAll());

    json_response([
        'success' => true,
        'users' => $users,
    ]);
}

if ($method !== 'POST') {
    json_response(['success' => false, 'message' => 'Método no permitido.'], 405);
}

$payload = read_json_body();
$action = $payload['action'] ?? 'upsert';

if ($action === 'login') {
    $username = trim((string) ($payload['username'] ?? ''));
    $password = (string) ($payload['password'] ?? '');

    if ($username === '' || $password === '') {
        json_response(['success' => false, 'message' => 'Usuario y contraseña son obligatorios.'], 400);
    }

    $statement = $pdo->prepare('SELECT * FROM users WHERE username = :username LIMIT 1');
    $statement->execute(['username' => $username]);
    $user = $statement->fetch();

    if (!$user || !verify_password((string) ($user['password'] ?? ''), $password)) {
        json_response(['success' => false, 'message' => 'Usuario o contraseña incorrectos.'], 401);
    }

    if (!str_starts_with((string) $user['password'], '$2y$') && !str_starts_with((string) $user['password'], '$argon2')) {
        $hashed = hash_password($password);
        $update = $pdo->prepare('UPDATE users SET password = :password WHERE id = :id');
        $update->execute(['password' => $hashed, 'id' => $user['id']]);
    }

    json_response([
        'success' => true,
        'user' => public_user_row($user),
    ]);
}

if ($action === 'import') {
    $users = $payload['users'] ?? null;
    if (!is_array($users)) {
        json_response(['success' => false, 'message' => 'La importación debe incluir una lista de usuarios.'], 400);
    }

    $count = 0;
    foreach ($users as $user) {
        if (!is_array($user) || empty($user['id']) || empty($user['fullName'])) {
            continue;
        }

        upsert_user($pdo, $user);
        $count++;
    }

    json_response([
        'success' => true,
        'count' => $count,
        'message' => "Importación exitosa. {$count} usuarios procesados.",
    ]);
}

upsert_user($pdo, $payload);
json_response(['success' => true]);

function upsert_user(PDO $pdo, array $user): void
{
    $userId = (string) ($user['id'] ?? '');
    $existingHash = null;

    if ($userId !== '') {
        $lookup = $pdo->prepare('SELECT password FROM users WHERE id = :id LIMIT 1');
        $lookup->execute(['id' => $userId]);
        $existing = $lookup->fetch();
        $existingHash = $existing['password'] ?? null;
    }

    $passwordToStore = prepare_password_for_storage(
        array_key_exists('password', $user) ? (string) ($user['password'] ?? '') : null,
        $existingHash !== null ? (string) $existingHash : null
    );

    if ($passwordToStore === null && $existingHash !== null) {
        $passwordToStore = $existingHash;
    }

    $statement = $pdo->prepare(
        'INSERT INTO users (
            id, full_name, national_id, age, blood_type, address, emergency_phone,
            emergency_contact, medical_observations, created_at, photo, username, password, device_id
        ) VALUES (
            :id, :full_name, :national_id, :age, :blood_type, :address, :emergency_phone,
            :emergency_contact, :medical_observations, :created_at, :photo, :username, :password, :device_id
        )
        ON DUPLICATE KEY UPDATE
            full_name = VALUES(full_name),
            national_id = VALUES(national_id),
            age = VALUES(age),
            blood_type = VALUES(blood_type),
            address = VALUES(address),
            emergency_phone = VALUES(emergency_phone),
            emergency_contact = VALUES(emergency_contact),
            medical_observations = VALUES(medical_observations),
            created_at = VALUES(created_at),
            photo = VALUES(photo),
            username = VALUES(username),
            password = VALUES(password),
            device_id = VALUES(device_id)'
    );

    $statement->execute([
        'id' => $userId,
        'full_name' => (string) ($user['fullName'] ?? ''),
        'national_id' => (string) ($user['nationalId'] ?? ''),
        'age' => (int) ($user['age'] ?? 0),
        'blood_type' => (string) ($user['bloodType'] ?? ''),
        'address' => (string) ($user['address'] ?? ''),
        'emergency_phone' => (string) ($user['emergencyPhone'] ?? ''),
        'emergency_contact' => json_encode($user['emergencyContact'] ?? [], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
        'medical_observations' => (string) ($user['medicalObservations'] ?? ''),
        'created_at' => (string) ($user['createdAt'] ?? date(DATE_ATOM)),
        'photo' => $user['photo'] ?? null,
        'username' => $user['username'] ?? null,
        'password' => $passwordToStore,
        'device_id' => $user['deviceId'] ?? null,
    ]);
}
