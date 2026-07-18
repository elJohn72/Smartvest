<?php

declare(strict_types=1);

/**
 * Cola de trabajos asíncronos basada en archivos JSON.
 * Productor: queue_push(). Consumidor: php api/worker.php
 */
function queue_dir(): string
{
    $dir = sys_get_temp_dir() . '/smartvest_queue';
    if (!is_dir($dir)) {
        mkdir($dir, 0775, true);
    }
    foreach (['pending', 'processing', 'done', 'failed'] as $sub) {
        $path = $dir . '/' . $sub;
        if (!is_dir($path)) {
            mkdir($path, 0775, true);
        }
    }

    return $dir;
}

function queue_push(string $type, array $payload = []): string
{
    $id = bin2hex(random_bytes(8));
    $job = [
        'id' => $id,
        'type' => $type,
        'payload' => $payload,
        'created_at' => gmdate('c'),
        'attempts' => 0,
    ];

    $path = queue_dir() . '/pending/' . $id . '.json';
    file_put_contents(
        $path,
        json_encode($job, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT),
        LOCK_EX
    );

    return $id;
}

function queue_claim_next(): ?array
{
    $pending = queue_dir() . '/pending';
    $files = glob($pending . '/*.json') ?: [];
    sort($files);
    if ($files === []) {
        return null;
    }

    $source = $files[0];
    $id = basename($source, '.json');
    $dest = queue_dir() . '/processing/' . $id . '.json';

    if (!@rename($source, $dest)) {
        return null;
    }

    $raw = file_get_contents($dest);
    if ($raw === false) {
        return null;
    }

    $job = json_decode($raw, true);
    if (!is_array($job)) {
        return null;
    }

    $job['attempts'] = ((int) ($job['attempts'] ?? 0)) + 1;
    $job['claimed_at'] = gmdate('c');
    file_put_contents($dest, json_encode($job, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT), LOCK_EX);

    return $job;
}

function queue_complete(array $job, bool $ok, string $message = ''): void
{
    $id = (string) ($job['id'] ?? '');
    if ($id === '') {
        return;
    }

    $processing = queue_dir() . '/processing/' . $id . '.json';
    $targetDir = $ok ? 'done' : 'failed';
    $target = queue_dir() . '/' . $targetDir . '/' . $id . '.json';

    $job['finished_at'] = gmdate('c');
    $job['ok'] = $ok;
    $job['message'] = $message;

    file_put_contents($target, json_encode($job, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT), LOCK_EX);
    if (is_file($processing)) {
        @unlink($processing);
    }
}

function queue_stats(): array
{
    $base = queue_dir();
    $count = static function (string $sub) use ($base): int {
        return count(glob($base . '/' . $sub . '/*.json') ?: []);
    };

    return [
        'pending' => $count('pending'),
        'processing' => $count('processing'),
        'done' => $count('done'),
        'failed' => $count('failed'),
        'dir' => $base,
    ];
}
