#!/usr/bin/env php
<?php

declare(strict_types=1);

/**
 * Convierte contraseñas en texto plano a hash bcrypt.
 * Uso: php scripts/migrate-plain-passwords.php
 */

require __DIR__ . '/../api/config.php';

$pdo = get_pdo();
$statement = $pdo->query('SELECT id, username, password FROM users WHERE password IS NOT NULL AND password != ""');
$rows = $statement->fetchAll();

$migrated = 0;
$skipped = 0;

foreach ($rows as $row) {
    $stored = (string) $row['password'];

    if (str_starts_with($stored, '$2y$') || str_starts_with($stored, '$argon2')) {
        $skipped++;
        continue;
    }

    $update = $pdo->prepare('UPDATE users SET password = :password WHERE id = :id');
    $update->execute([
        'password' => password_hash($stored, PASSWORD_DEFAULT),
        'id' => $row['id'],
    ]);

    $migrated++;
    $username = $row['username'] ?: $row['id'];
    echo "Migrado: {$username}\n";
}

echo "Listo. Migrados: {$migrated}, ya hasheados: {$skipped}\n";
