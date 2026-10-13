<?php

declare(strict_types=1);

namespace SmartVest\Lib;

use PDO;

class Authorization
{
    private PDO $pdo;

    public function __construct(PDO $pdo)
    {
        $this->pdo = $pdo;
    }

    public function requireProfileOwner(string $profileId, string $accountId): array
    {
        $stmt = $this->pdo->prepare('
            SELECT * FROM profiles WHERE id = :id AND owner_account_id = :acc
        ');
        $stmt->execute(['id' => $profileId, 'acc' => $accountId]);
        $profile = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$profile) {
            http_response_code(404);
            echo json_encode(['error' => ['code' => 'NOT_FOUND', 'message' => 'Perfil no encontrado o no pertenece a esta cuenta.']]);
            exit;
        }

        return $profile;
    }

    public function requireDeviceOwner(string $deviceId, string $accountId): array
    {
        $stmt = $this->pdo->prepare('
            SELECT p.* FROM profiles p
            WHERE p.device_id = :device_id AND p.owner_account_id = :acc
        ');
        $stmt->execute(['device_id' => $deviceId, 'acc' => $accountId]);
        $profile = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$profile) {
            http_response_code(404);
            echo json_encode(['error' => ['code' => 'NOT_FOUND', 'message' => 'Dispositivo no vinculado a un perfil propio.']]);
            exit;
        }

        return $profile;
    }
}
