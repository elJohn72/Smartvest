# Runtime y Matriz de Compatibilidad SmartVest M1

Fecha de fijación: 2026-10-13

## 1. Android Nativo
- **Lenguaje:** Kotlin 2.0.21 / 2.1.0
- **UI Framework:** Jetpack Compose (BOM 2024.10.00 / Compose Compiler integrado en Kotlin)
- **Min SDK:** 26 (Android 8.0 Oreo) - garantiza soporte seguro de Keystore, TLS moderno y Java time APIs sin desugaring pesado
- **Compile SDK:** 35
- **Target SDK:** 35
- **Android Gradle Plugin (AGP):** 8.7.2
- **Gradle:** 8.10.2 / 8.11 (o 9.x compatible)
- **JDK requerido:** JDK 21 / 24+
- **Application ID:** `com.ajtecnology.smartvest`
- **Version Code:** 25
- **Version Name:** `1.1.0-m1`

## 2. Backend API v1 & Contenedores
- **Runtime:** PHP 8.2-fpm-alpine / 8.3-fpm-alpine con extensiones `pdo_mysql`, `json`, `sodium`, `opcache`
- **Webserver:** Nginx alpine como reverse-proxy / front controller hacia PHP-FPM
- **Base de datos:** MariaDB 10.11 LTS (`mariadb:10.11`)
- **Zona horaria por defecto:** UTC
- **Coolify Target:** Proyecto `SMARTVEST`, Environment `lab`

## 3. Firmware Embebido
- **Plataforma:** ESP32 Dev Module
- **Framework:** Arduino ESP32 (v2.0.17 / Espressif 7.1.3 PlatformIO)
- **Toolchain:** PlatformIO Core 6.1.16+
- **Seguridad:** WiFiClientSecure con validación de certificado TLS / CA Bundle (`esp_crt_bundle_attach`) y sincronización NTP antes de transferencias seguras.

## Vercel fuera del despliegue M1

John confirmó el 2026-10-13 que SmartVest M1 se desplegará en Coolify. `vercel.json` define `git.deploymentEnabled: false` para desactivar despliegues automáticos de la integración Git de Vercel en las ramas que contienen esta configuración. Los deployments anteriores permanecen como historial; no son validación de API PHP ni Android.

No se eliminan proyectos, dominios ni la GitHub App global. Los jobs propios de GitHub Actions se conservan. Al fusionar este cambio, main también deja de lanzar deploys automáticos Vercel. GitHub no tenía protección de rama ni reglas efectivas que exigieran Vercel al comprobarlo. Esta decisión no configura Coolify ni autoriza merge/deploy.

Referencia oficial: https://vercel.com/docs/project-configuration/git-configuration#gitdeploymentenabled
