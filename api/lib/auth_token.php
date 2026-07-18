<?php

declare(strict_types=1);

/**
 * Token de sesión HMAC: valida identidad sin reconsultar users en cada request.
 */
function auth_token_secret(): string
{
    if (defined('SMARTVEST_AUTH_SECRET') && SMARTVEST_AUTH_SECRET !== '') {
        return SMARTVEST_AUTH_SECRET;
    }

    $fromEnv = getenv('SMARTVEST_AUTH_SECRET');
    if (is_string($fromEnv) && $fromEnv !== '') {
        return $fromEnv;
    }

    return get_iot_api_key() . '|smartvest-auth-local';
}

function issue_auth_token(string $userId, string $username, int $ttlSeconds = 3600): string
{
    $exp = time() + max(60, $ttlSeconds);
    $payload = $userId . '|' . $username . '|' . $exp;
    $sig = hash_hmac('sha256', $payload, auth_token_secret());

    return rtrim(strtr(base64_encode($payload . '|' . $sig), '+/', '-_'), '=');
}

function verify_auth_token(string $token): ?array
{
    $pad = strlen($token) % 4;
    if ($pad > 0) {
        $token .= str_repeat('=', 4 - $pad);
    }

    $decoded = base64_decode(strtr($token, '-_', '+/'), true);
    if ($decoded === false) {
        return null;
    }

    $parts = explode('|', $decoded);
    if (count($parts) !== 4) {
        return null;
    }

    [$userId, $username, $expRaw, $sig] = $parts;
    $exp = (int) $expRaw;
    if ($userId === '' || $exp < time()) {
        return null;
    }

    $payload = $userId . '|' . $username . '|' . $exp;
    $expected = hash_hmac('sha256', $payload, auth_token_secret());
    if (!hash_equals($expected, $sig)) {
        return null;
    }

    return [
        'userId' => $userId,
        'username' => $username,
        'expiresAt' => $exp,
    ];
}

function read_bearer_token(): string
{
    $header = (string) ($_SERVER['HTTP_AUTHORIZATION'] ?? '');
    if (preg_match('/^\s*Bearer\s+(\S+)\s*$/i', $header, $m)) {
        return $m[1];
    }

    return (string) ($_SERVER['HTTP_X_SMARTVEST_TOKEN'] ?? '');
}

/**
 * Auth sin query a BD: solo verifica firma HMAC del token.
 */
function require_auth_token(): array
{
    $token = read_bearer_token();
    if ($token === '') {
        json_response(['success' => false, 'message' => 'Token requerido.'], 401);
    }

    $session = verify_auth_token($token);
    if ($session === null) {
        json_response(['success' => false, 'message' => 'Token inválido o expirado.'], 401);
    }

    return $session;
}
