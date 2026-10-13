<?php

declare(strict_types=1);

namespace SmartVest\Lib;

use PDO;

class SessionManager
{
    private PDO $pdo;

    public function __construct(PDO $pdo)
    {
        $this->pdo = $pdo;
    }

    public function createSession(string $accountId, int $ttlSeconds = 43200): string
    {
        $rawToken = bin2hex(random_bytes(32));
        $tokenHash = hash('sha256', $rawToken);
        $sessionId = bin2hex(random_bytes(16));
        $expiresAt = gmdate('Y-m-d H:i:s', time() + $ttlSeconds);

        $stmt = $this->pdo->prepare('
            INSERT INTO sessions (id, token_hash, account_id, issued_at, expires_at, last_seen_at)
            VALUES (:id, :hash, :acc, UTC_TIMESTAMP(), :exp, UTC_TIMESTAMP())
        ');
        $stmt->execute([
            'id' => $sessionId,
            'hash' => $tokenHash,
            'acc' => $accountId,
            'exp' => $expiresAt,
        ]);

        return $rawToken;
    }

    public function authenticate(string $rawToken): ?array
    {
        if ($rawToken === '') {
            return null;
        }

        $tokenHash = hash('sha256', $rawToken);
        $stmt = $this->pdo->prepare('
            SELECT s.id as session_id, s.account_id, s.expires_at, s.revoked_at, a.username, a.status
            FROM sessions s
            JOIN accounts a ON s.account_id = a.id
            WHERE s.token_hash = :hash
        ');
        $stmt->execute(['hash' => $tokenHash]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$row) {
            return null;
        }

        if ($row['revoked_at'] !== null) {
            return null;
        }

        if (strtotime($row['expires_at'] . ' UTC') < time()) {
            return null;
        }

        if ($row['status'] !== 'active') {
            return null;
        }

        // Actualizar last_seen_at
        $updateStmt = $this->pdo->prepare('UPDATE sessions SET last_seen_at = UTC_TIMESTAMP() WHERE id = :id');
        $updateStmt->execute(['id' => $row['session_id']]);

        return [
            'accountId' => $row['account_id'],
            'username' => $row['username'],
            'sessionId' => $row['session_id'],
        ];
    }

    public function revokeSession(string $rawToken): bool
    {
        $tokenHash = hash('sha256', $rawToken);
        $stmt = $this->pdo->prepare('
            UPDATE sessions
            SET revoked_at = UTC_TIMESTAMP()
            WHERE token_hash = :hash AND revoked_at IS NULL
        ');
        $stmt->execute(['hash' => $tokenHash]);
        return $stmt->rowCount() > 0;
    }
}
