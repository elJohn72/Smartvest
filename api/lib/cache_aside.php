<?php

declare(strict_types=1);

/**
 * Cache-aside en filesystem (sin Redis) para XAMPP local.
 * TTL + invalidación por clave/prefijo.
 */
function cache_dir(): string
{
    $dir = sys_get_temp_dir() . '/smartvest_cache';
    if (!is_dir($dir)) {
        mkdir($dir, 0775, true);
    }

    return $dir;
}

function cache_path(string $key): string
{
    return cache_dir() . '/' . hash('sha256', $key) . '.json';
}

function cache_get(string $key): mixed
{
    $path = cache_path($key);
    if (!is_file($path)) {
        return null;
    }

    $raw = file_get_contents($path);
    if ($raw === false) {
        return null;
    }

    $payload = json_decode($raw, true);
    if (!is_array($payload) || !array_key_exists('expires_at', $payload) || !array_key_exists('value', $payload)) {
        @unlink($path);
        return null;
    }

    if ((int) $payload['expires_at'] < time()) {
        @unlink($path);
        return null;
    }

    return $payload['value'];
}

function cache_set(string $key, mixed $value, int $ttlSeconds): void
{
    $ttlSeconds = max(1, $ttlSeconds);
    $payload = [
        'expires_at' => time() + $ttlSeconds,
        'value' => $value,
        'key' => $key,
    ];

    file_put_contents(
        cache_path($key),
        json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
        LOCK_EX
    );
}

function cache_forget(string $key): void
{
    $path = cache_path($key);
    if (is_file($path)) {
        @unlink($path);
    }
}

/**
 * Invalida claves cuyo nombre empieza con el prefijo (escanea metadatos).
 */
function cache_forget_prefix(string $prefix): void
{
    $dir = cache_dir();
    foreach (glob($dir . '/*.json') ?: [] as $file) {
        $raw = @file_get_contents($file);
        if ($raw === false) {
            continue;
        }
        $payload = json_decode($raw, true);
        $key = is_array($payload) ? (string) ($payload['key'] ?? '') : '';
        if ($key !== '' && str_starts_with($key, $prefix)) {
            @unlink($file);
        }
    }
}

function cache_remember(string $key, int $ttlSeconds, callable $producer): mixed
{
    $cached = cache_get($key);
    if ($cached !== null) {
        return $cached;
    }

    $value = $producer();
    cache_set($key, $value, $ttlSeconds);

    return $value;
}
