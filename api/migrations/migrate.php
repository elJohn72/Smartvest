<?php

declare(strict_types=1);

/**
 * Script de ejecución de migraciones y fixtures para SmartVest API
 */

require_once __DIR__ . '/../config.php';

$pdo = get_pdo();

$files = [
    __DIR__ . '/001_native.sql',
];

if (isset($argv[1]) && file_exists($argv[1])) {
    $files[] = $argv[1];
}

foreach ($files as $file) {
    echo "Ejecutando SQL desde $file...\n";
    $sql = file_get_contents($file);
    if ($sql === false) {
        fwrite(STDERR, "Error al leer $file\n");
        exit(1);
    }

    // MariaDB PDO exec con soporte para múltiples queries
    try {
        $pdo->exec($sql);
        echo "OK: $file ejecutado con éxito.\n";
    } catch (PDOException $e) {
        fwrite(STDERR, "Error ejecutando $file: " . $e->getMessage() . "\n");
        exit(1);
    }
}

echo "Migraciones completadas exitosamente.\n";
exit(0);
