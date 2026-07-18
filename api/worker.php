<?php

declare(strict_types=1);

/**
 * Worker de cola asíncrona.
 * Uso: php api/worker.php [--once] [--max=20]
 */
require __DIR__ . '/config.php';

$once = in_array('--once', $argv ?? [], true);
$max = 50;
foreach ($argv ?? [] as $arg) {
    if (str_starts_with($arg, '--max=')) {
        $max = max(1, (int) substr($arg, 6));
    }
}

$processed = 0;
echo "SmartVest queue worker · " . queue_dir() . PHP_EOL;

while ($processed < $max) {
    $job = queue_claim_next();
    if ($job === null) {
        echo "Sin jobs pendientes. Stats: " . json_encode(queue_stats()) . PHP_EOL;
        break;
    }

    $type = (string) ($job['type'] ?? '');
    $payload = is_array($job['payload'] ?? null) ? $job['payload'] : [];
    echo "Procesando {$job['id']} type={$type}" . PHP_EOL;

    try {
        $message = process_smartvest_job($type, $payload);
        queue_complete($job, true, $message);
        echo "  OK: {$message}" . PHP_EOL;
    } catch (Throwable $e) {
        queue_complete($job, false, $e->getMessage());
        echo "  FAIL: " . $e->getMessage() . PHP_EOL;
    }

    $processed++;
    if ($once) {
        break;
    }
}

echo "Hechos: {$processed}. Stats: " . json_encode(queue_stats()) . PHP_EOL;

function process_smartvest_job(string $type, array $payload): string
{
    return match ($type) {
        'prune_iot_history' => job_prune_iot_history($payload),
        'notify_sos' => job_notify_sos($payload),
        default => throw new RuntimeException("Tipo de job desconocido: {$type}"),
    };
}

function job_prune_iot_history(array $payload): string
{
    $deviceId = (string) ($payload['deviceId'] ?? '');
    $maxRows = (int) ($payload['maxRows'] ?? 500);
    if ($deviceId === '') {
        throw new InvalidArgumentException('deviceId requerido');
    }

    $pdo = get_pdo();
    $countStatement = $pdo->prepare('SELECT COUNT(*) FROM iot_history WHERE device_id = :device_id');
    $countStatement->execute(['device_id' => $deviceId]);
    $count = (int) $countStatement->fetchColumn();

    if ($count <= $maxRows) {
        return "Sin prune ({$count} filas <= {$maxRows})";
    }

    $deleteStatement = $pdo->prepare(
        'DELETE FROM iot_history
        WHERE device_id = :device_id
        AND id NOT IN (
            SELECT id FROM (
                SELECT id FROM iot_history
                WHERE device_id = :device_id_inner
                ORDER BY recorded_at DESC
                LIMIT :keep_rows
            ) AS recent_rows
        )'
    );
    $deleteStatement->bindValue('device_id', $deviceId);
    $deleteStatement->bindValue('device_id_inner', $deviceId);
    $deleteStatement->bindValue('keep_rows', $maxRows, PDO::PARAM_INT);
    $deleteStatement->execute();

    return "Prune OK device={$deviceId} before={$count}";
}

function job_notify_sos(array $payload): string
{
    // Job simulado: en producción enviaría SMS/push. Aquí deja evidencia en log de cola.
    $deviceId = (string) ($payload['deviceId'] ?? '');
    $lat = $payload['latitude'] ?? null;
    $lng = $payload['longitude'] ?? null;
    $log = sys_get_temp_dir() . '/smartvest_sos_notifications.log';
    $line = gmdate('c') . " SOS device={$deviceId} lat={$lat} lng={$lng}" . PHP_EOL;
    file_put_contents($log, $line, FILE_APPEND | LOCK_EX);

    return 'SOS notificado (log async): ' . trim($line);
}
