FROM php:8.2-fpm-alpine

# Instalar dependencias del sistema y extensiones de PHP necesarias
RUN apk add --no-cache \
    nginx \
    mariadb-client \
    bash \
    tzdata \
    && docker-php-ext-install pdo pdo_mysql opcache

ENV TZ=UTC

# Configuración Nginx
COPY infra/smartvest/nginx.conf /etc/nginx/nginx.conf

# Directorio de trabajo
WORKDIR /var/www/html

# Copiar aplicación
COPY api /var/www/html/api
COPY infra/smartvest/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh \
    && chown -R www-data:www-data /var/www/html

EXPOSE 80

ENTRYPOINT ["/entrypoint.sh"]
