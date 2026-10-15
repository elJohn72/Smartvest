#!/usr/bin/env bash
set -e

# Iniciar PHP-FPM en segundo plano
php-fpm -D

# Esperar a MariaDB si está configurada
if [ -n "$DB_HOST" ]; then
    echo "Esperando a MariaDB en $DB_HOST:$DB_PORT..."
    for i in {1..30}; do
        if mariadb-admin ping -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" --silent; then
            echo "MariaDB disponible."
            break
        fi
        sleep 1
    done
fi

# Iniciar Nginx en primer plano
echo "Iniciando Nginx..."
exec nginx -g "daemon off;"
