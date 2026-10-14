<?php

declare(strict_types=1);

function smartvest_env_int(string $name, int $default): int
{
    $value = getenv($name);
    if ($value === false || $value === '') {
        return $default;
    }
    return (int)$value;
}

function smartvest_registration_enabled(): bool
{
    $value = getenv('SMARTVEST_LAB_REGISTRATION');
    if ($value === false || $value === '') {
        return true;
    }
    return !in_array(strtolower((string)$value), ['0', 'false', 'off', 'no'], true);
}

function smartvest_client_address(): string
{
    $ip = $_SERVER['REMOTE_ADDR'] ?? '';
    if (!is_string($ip) || $ip === '') {
        return 'unknown';
    }
    return $ip;
}

function smartvest_rate_subject_hash(string $scope, string $subject): string
{
    return hash('sha256', $scope . "\n" . $subject);
}

/**
 * null = permitido. Entero = segundos de Retry-After.
 * $consume incrementa el contador de la ventana. Una ventana vencida se reinicia;
 * el bloqueo no es indefinido.
 */
function smartvest_rate_gate(PDO $pdo, string $scope, string $subject, int $max, int $windowSeconds, bool $consume): ?int
{
    if ($max < 1) {
        $max = 1;
    }
    if ($windowSeconds < 1) {
        $windowSeconds = 1;
    }

    $hash = smartvest_rate_subject_hash($scope, $subject);
    $now = time();
    $pdo->beginTransaction();
    try {
        $select = $pdo->prepare(
            'SELECT window_started_at, attempt_count FROM auth_rate_limits WHERE scope = :scope AND subject_hash = :hash FOR UPDATE'
        );
        $select->execute(['scope' => $scope, 'hash' => $hash]);
        $row = $select->fetch(PDO::FETCH_ASSOC);

        $expired = !$row || ($now - (int)$row['window_started_at']) >= $windowSeconds;
        if ($expired) {
            if ($consume) {
                $reset = $pdo->prepare(
                    'INSERT INTO auth_rate_limits (scope, subject_hash, window_started_at, attempt_count)
                     VALUES (:scope, :hash, :started, 1)
                     ON DUPLICATE KEY UPDATE window_started_at = VALUES(window_started_at), attempt_count = 1'
                );
                $reset->execute(['scope' => $scope, 'hash' => $hash, 'started' => $now]);
            }
            $pdo->commit();
            return null;
        }

        $count = (int)$row['attempt_count'];
        $retryAfter = max(1, $windowSeconds - ($now - (int)$row['window_started_at']));
        if ($count >= $max) {
            $pdo->commit();
            return $retryAfter;
        }

        if ($consume) {
            $update = $pdo->prepare(
                'UPDATE auth_rate_limits SET attempt_count = attempt_count + 1 WHERE scope = :scope AND subject_hash = :hash'
            );
            $update->execute(['scope' => $scope, 'hash' => $hash]);
            $count++;
            if ($count >= $max) {
                $pdo->commit();
                return $retryAfter;
            }
        }

        $pdo->commit();
        return null;
    } catch (Throwable $exception) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        throw $exception;
    }
}

function smartvest_rate_clear(PDO $pdo, string $scope, string $subject): void
{
    $delete = $pdo->prepare('DELETE FROM auth_rate_limits WHERE scope = :scope AND subject_hash = :hash');
    $delete->execute([
        'scope' => $scope,
        'hash' => smartvest_rate_subject_hash($scope, $subject),
    ]);
}

function smartvest_rate_limit_response(?int $retryAfter): void
{
    if ($retryAfter === null) {
        return;
    }
    http_response_code(429);
    header('Retry-After: ' . $retryAfter);
    echo json_encode([
        'error' => [
            'code' => 'RATE_LIMITED',
            'message' => 'Demasiados intentos. Espera y vuelve a probar.',
            'retryAfterSeconds' => $retryAfter,
        ],
    ]);
    exit;
}
