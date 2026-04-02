/****************************************************
 * SmartVest - Version previa a ESP32-CAM (con GPS en SMS)
 * 
 * Funciones:
 *  - Ultrasonido HC-SR04 -> Buzzer + Motor vibrador`
 *  - GPS NEO-6M por UART1 (parseo básico $GPRMC)
 *  - SIM800L por UART2
 *  - Botón SOS en D27: envía SMS con ubicación GPS
 *
 *  Placa: ESP32 DevKit
 *  Monitor Serie: 115200 baud
 ****************************************************/

 #include <HardwareSerial.h>

 // ================== PINES ULTRASONIDO / ALERTAS ==================
 const int PIN_TRIG      = 5;    // TRIG HC-SR04 -> GPIO5
 const int PIN_ECHO      = 18;   // ECHO HC-SR04 -> GPIO18 (con divisor)
 const int PIN_BUZZER    = 13;   // BUZZER -> GPIO13
 const int PIN_VIBRADOR  = 32;   // VIBRADOR -> GPIO32
 
 // Parámetros del sensor ultra
 const unsigned long TIMEOUT_ULTRA = 30000UL; // 30 ms (≈5m)
 
 // Umbrales de distancia (cm)
 const float DIST_PELIGRO     = 40.0;
 const float DIST_ALERTA      = 100.0;
 const float DIST_PRECAUCION  = 200.0;
 
 // ================== PINES GPS (UART1) ==================
 HardwareSerial SerialGPS(1);    // UART1
 
 // Según tu esquema actual:
 const int GPS_RX = 4;           // GPIO4  <- TX del GPS NEO-6M
 const int GPS_TX = 14;          // GPIO14 -> RX del GPS NEO-6M
 
 // Variables GPS
 double gpsLat = 0.0;
 double gpsLon = 0.0;
 bool   gpsFixValido = false;   // true si hay fix válido
 String gpsUltimaTrama = "";    // opcional, para debug
 
 // ================== PINES SIM800L (UART2) ==================
 HardwareSerial SerialGSM(2);    // UART2
 
 const int GSM_RX = 16;          // GPIO16 <- TX del SIM800L
 const int GSM_TX = 17;          // GPIO17 -> RX del SIM800L
 
 // Número destino para SOS
 const char NUM_DESTINO_SOS[] = "+593963930791";
 
 // ================== BOTÓN SOS ==================
 const int PIN_SOS = 27;         // Botón SOS -> GPIO27 (a GND, con INPUT_PULLUP)
 
 // ================== PATRÓN DE ALERTA ==================
 struct PatronAlerta {
   unsigned long periodo;   // ciclo total (ms)
   unsigned long onTime;    // tiempo ON dentro del ciclo (ms)
   bool activo;
 };
 
 PatronAlerta patronActual = {0, 0, false};
 unsigned long tInicioPatron = 0;
 float distanciaCM = 0.0;
 bool sosPrevEstado = false;
 
 // ================== PROTOTIPOS ==================
 float medirDistanciaCM();
 void  actualizarPatronPorDistancia(float d);
 void  aplicarPatron();
 
 bool  sim800_enviarComando(const char* cmd, const char* respEsperada, unsigned long timeout);
 bool  sim800_enviarSMS(const String &mensaje);
 
 // GPS
 void   procesarGPS();                         // lee UART1 y parsea líneas
 void   procesarLineaNMEA(const String &linea);
 bool   parsearGPRMC(const String &linea);
 double nmeaToDecimal(const String &campo, char hemisferio);
 
 // =======================================================
 // ======================= SETUP =========================
 // =======================================================
 void setup() {
   Serial.begin(115200);
   delay(1000);
   Serial.println();
   Serial.println("===== SmartVest - Version con GPS en SMS SOS =====");
 
   // --- Pines ultrasonido / alertas ---
   pinMode(PIN_TRIG, OUTPUT);
   pinMode(PIN_ECHO, INPUT);
   pinMode(PIN_BUZZER, OUTPUT);
   pinMode(PIN_VIBRADOR, OUTPUT);
 
   digitalWrite(PIN_TRIG, LOW);
   digitalWrite(PIN_BUZZER, LOW);
   digitalWrite(PIN_VIBRADOR, LOW);
 
   // --- Botón SOS ---
   pinMode(PIN_SOS, INPUT_PULLUP);
 
   // --- UART GPS (NEO-6M) ---
   SerialGPS.begin(9600, SERIAL_8N1, GPS_RX, GPS_TX);
   Serial.println("GPS NEO-6M inicializado en UART1.");
   Serial.print("  RX1 = GPIO");
   Serial.print(GPS_RX);
   Serial.print("  (desde TX GPS), TX1 = GPIO");
   Serial.println(GPS_TX);
 
   // --- UART SIM800L ---
   SerialGSM.begin(9600, SERIAL_8N1, GSM_RX, GSM_TX);
   Serial.println("SIM800L inicializado en UART2.");
   Serial.print("  RX2 = GPIO");
   Serial.print(GSM_RX);
   Serial.print("  (desde TX SIM800L), TX2 = GPIO");
   Serial.println(GSM_TX);
 
   delay(2000); // tiempo para que el SIM800L arranque
 
   Serial.println("\nInicializando SIM800L...");
   sim800_enviarComando("AT",       "OK", 2000);
   sim800_enviarComando("ATE0",     "OK", 2000);  // sin eco
   sim800_enviarComando("AT+CMGF=1","OK", 2000);  // modo texto
   sim800_enviarComando("AT+CSQ",   "OK", 2000);  // señal
   sim800_enviarComando("AT+CREG?", "OK", 2000);  // registro en red
 
   tInicioPatron = millis();
 
   Serial.println("\nSistema listo. Midiendo distancia, leyendo GPS y esperando SOS...");
   Serial.println("----------------------------------------------------------------");
 }
 
 // =======================================================
 // ======================== LOOP =========================
 // =======================================================
 void loop() {
   // 1) Medir distancia y aplicar patrón de alerta
   distanciaCM = medirDistanciaCM();
   actualizarPatronPorDistancia(distanciaCM);
   aplicarPatron();
 
   // 2) Procesar datos de GPS continuamente
   procesarGPS();
 
   // 3) Botón SOS: flanco de NO presionado -> presionado
   bool sosPresionado = (digitalRead(PIN_SOS) == LOW);
   if (sosPresionado && !sosPrevEstado) {
     Serial.println("\n[EVENTO] Botón SOS PRESIONADO.");
 
     String mensaje;
     mensaje  = "ALERTA SOS SmartVest: usuario requiere ayuda.\n";
 
     if (gpsFixValido) {
       mensaje += "Ubicacion aproximada:\n";
       mensaje += "Lat: ";
       mensaje += String(gpsLat, 6);
       mensaje += ", Lon: ";
       mensaje += String(gpsLon, 6);
       mensaje += "\n";
       mensaje += "Mapa: https://maps.google.com/?q=";
       mensaje += String(gpsLat, 6);
       mensaje += ",";
       mensaje += String(gpsLon, 6);
     } else {
       mensaje += "Ubicacion GPS NO disponible (sin fix).\n";
       mensaje += "Verificar cobertura de satelites.";
     }
 
     if (sim800_enviarSMS(mensaje)) {
       Serial.println("[OK] SMS SOS ENVIADO correctamente (con info GPS).");
     } else {
       Serial.println("[ERROR] No se pudo enviar el SMS SOS.");
     }
   }
   sosPrevEstado = sosPresionado;
 
   // 4) Monitoreo por Serial (distancia y estado básico)
   Serial.print("Distancia: ");
   Serial.print(distanciaCM);
   Serial.print(" cm | GPS fix: ");
   Serial.print(gpsFixValido ? "OK" : "NO");
   if (gpsFixValido) {
     Serial.print(" | Lat: ");
     Serial.print(gpsLat, 6);
     Serial.print(", Lon: ");
     Serial.print(gpsLon, 6);
   }
   Serial.println();
 
   // 5) Mostrar tráfico del SIM800L (respuestas AT, SMS entrantes, etc.)
   while (SerialGSM.available()) {
     char c = SerialGSM.read();
     Serial.write(c);
   }
 
   // 6) (Opcional) Enviar comandos AT manuales al SIM800L desde el PC
   if (Serial.available()) {
     char c = Serial.read();
     SerialGSM.write(c);
   }
 
   delay(200); // pequeño respiro
 }
 
 // =======================================================
 // =========== LÓGICA DE ULTRASONIDO / ALERTAS ==========
 // =======================================================
 
 // Medir distancia con HC-SR04 (cm)
 float medirDistanciaCM() {
   digitalWrite(PIN_TRIG, LOW);
   delayMicroseconds(2);
   digitalWrite(PIN_TRIG, HIGH);
   delayMicroseconds(10);
   digitalWrite(PIN_TRIG, LOW);
 
   unsigned long duracion = pulseIn(PIN_ECHO, HIGH, TIMEOUT_ULTRA);
 
   if (duracion == 0) {
     // Sin eco: sin obstáculo o fuera de rango
     return 9999.0;
   }
 
   // Fórmula HC-SR04: distancia (cm) = tiempo(us) * 0.0343 / 2
   float distancia = (duracion * 0.0343f) / 2.0f;
   return distancia;
 }
 
 // Elegir patrón de vibración/sonido según distancia
 void actualizarPatronPorDistancia(float d) {
   PatronAlerta nuevoPatron;
 
   if (d > 500.0 || d > DIST_PRECAUCION) {
     // Muy lejos o lectura inválida: sin alerta
     nuevoPatron = {0, 0, false};
   }
   else if (d > DIST_ALERTA) {
     // Precaución (100~200 cm aprox)
     nuevoPatron.periodo = 1000;   // 1 s
     nuevoPatron.onTime  = 200;    // 0.2 s ON
     nuevoPatron.activo  = true;
   }
   else if (d > DIST_PELIGRO) {
     // Alerta (40~100 cm)
     nuevoPatron.periodo = 600;    // 0.6 s
     nuevoPatron.onTime  = 200;    // 0.2 s ON
     nuevoPatron.activo  = true;
   }
   else {
     // Peligro (< 40 cm)
     nuevoPatron.periodo = 400;    // 0.4 s
     nuevoPatron.onTime  = 300;    // 0.3 s ON
     nuevoPatron.activo  = true;
   }
 
   // Actualizar solo si cambió el patrón
   if (nuevoPatron.periodo != patronActual.periodo ||
       nuevoPatron.onTime  != patronActual.onTime  ||
       nuevoPatron.activo  != patronActual.activo) {
     patronActual = nuevoPatron;
     tInicioPatron = millis();
   }
 }
 
 // Aplicar patrón al buzzer y vibrador (no bloqueante)
 void aplicarPatron() {
   if (!patronActual.activo || patronActual.periodo == 0) {
     digitalWrite(PIN_BUZZER, LOW);
     digitalWrite(PIN_VIBRADOR, LOW);
     return;
   }
 
   unsigned long ahora   = millis();
   unsigned long tCiclo  = (ahora - tInicioPatron) % patronActual.periodo;
 
   if (tCiclo < patronActual.onTime) {
     digitalWrite(PIN_BUZZER, HIGH);
     digitalWrite(PIN_VIBRADOR, HIGH);
   } else {
     digitalWrite(PIN_BUZZER, LOW);
     digitalWrite(PIN_VIBRADOR, LOW);
   }
 }
 
 // =======================================================
 // ==================== FUNCIONES SIM800L =================
 // =======================================================
 
 // Envía comando AT y espera respEsperada en el buffer
 bool sim800_enviarComando(const char* cmd, const char* respEsperada, unsigned long timeout) {
   // Limpiar buffer previo
   while (SerialGSM.available()) SerialGSM.read();
 
   Serial.print("[CMD] ");
   Serial.println(cmd);
 
   SerialGSM.print(cmd);
   SerialGSM.print("\r");
 
   unsigned long t0 = millis();
   String buffer = "";
 
   while (millis() - t0 < timeout) {
     while (SerialGSM.available()) {
       char c = SerialGSM.read();
       buffer += c;
       Serial.write(c);
 
       if (buffer.indexOf(respEsperada) != -1) {
         Serial.println();
         Serial.print("[OK] Respuesta contiene: ");
         Serial.println(respEsperada);
         return true;
       }
       if (buffer.indexOf("ERROR") != -1) {
         Serial.println("\n[ERROR] El módulo respondió ERROR.");
         return false;
       }
     }
   }
 
   Serial.println();
   Serial.print("[TIMEOUT] No se encontró '");
   Serial.print(respEsperada);
   Serial.println("' en la respuesta.");
   return false;
 }
 
 // Envía un SMS de texto al número SOS
 bool sim800_enviarSMS(const String &mensaje) {
   // Asegurar modo texto
   if (!sim800_enviarComando("AT+CMGF=1", "OK", 2000)) {
     Serial.println("[ERROR] No se pudo configurar modo texto.");
     return false;
   }
 
   // Iniciar SMS
   {
     String cmd = String("AT+CMGS=\"") + NUM_DESTINO_SOS + "\"";
     if (!sim800_enviarComando(cmd.c_str(), ">", 5000)) {
       Serial.println("[ERROR] No se recibió '>' para escribir el SMS.");
       return false;
     }
   }
 
   // Cuerpo del SMS
   Serial.println("[INFO] Escribiendo cuerpo del SMS SOS...");
   SerialGSM.print(mensaje);
 
   // CTRL+Z para enviar
   SerialGSM.write(26); // ASCII 26 = CTRL+Z
   Serial.println("[INFO] CTRL+Z enviado, esperando confirmación...");
 
   // Esperar confirmación
   unsigned long t0 = millis();
   String buffer = "";
   while (millis() - t0 < 20000) { // hasta 20 s
     while (SerialGSM.available()) {
       char c = SerialGSM.read();
       buffer += c;
       Serial.write(c);
 
       if (buffer.indexOf("+CMGS:") != -1 || buffer.indexOf("OK") != -1) {
         Serial.println("\n[OK] SIM800L reporta SMS enviado.");
         return true;
       }
       if (buffer.indexOf("ERROR") != -1) {
         Serial.println("\n[ERROR] SIM800L reporta error al enviar SMS.");
         return false;
       }
     }
   }
 
   Serial.println("\n[TIMEOUT] No hubo confirmación clara de envío.");
   return false;
 }
 
 // =======================================================
 // ================== FUNCIONES GPS/NMEA =================
 // =======================================================
 
 // Leer datos de UART GPS y acumular líneas NMEA
 void procesarGPS() {
   static String linea = "";
 
   while (SerialGPS.available()) {
     char c = SerialGPS.read();
 
     // Opcional: ver todo el stream NMEA
     // Serial.write(c);
 
     if (c == '\r') {
       // ignoramos CR
       continue;
     } else if (c == '\n') {
       if (linea.length() > 0) {
         procesarLineaNMEA(linea);
         linea = "";
       }
     } else {
       linea += c;
       if (linea.length() > 120) {
         linea = ""; // evitar desbordes si algo viene corrupto
       }
     }
   }
 }
 
 void procesarLineaNMEA(const String &linea) {
   // Guardar última trama (debug)
   gpsUltimaTrama = linea;
 
   if (linea.startsWith("$GPRMC")) {
     if (parsearGPRMC(linea)) {
       gpsFixValido = true;
       // Serial.println("[GPS] Fix actualizado correctamente.");
     } else {
       // Estado no válido
       gpsFixValido = false;
     }
   }
 }
 
 // Formato típico GPRMC:
 // $GPRMC,hhmmss.sss,A,llll.ll,a,yyyyy.yy,a,x.x,x.x,ddmmyy,... 
 //  index: 0      1      2 3       4 5        6 ...
 //         $GPRMC,time,  A,lat,    N,lon,     W,...
 bool parsearGPRMC(const String &linea) {
   // Separar por comas de forma simple
   int idx = 0;
   int campoIndex = 0;
   String campos[12];  // nos basta con los primeros 7-8 campos
 
   while (idx < linea.length() && campoIndex < 12) {
     int coma = linea.indexOf(',', idx);
     if (coma == -1) {
       campos[campoIndex++] = linea.substring(idx);
       break;
     } else {
       campos[campoIndex++] = linea.substring(idx, coma);
       idx = coma + 1;
     }
   }
 
   if (campoIndex < 7) {
     return false;  // muy corta
   }
 
   String status = campos[2]; // 'A' = válido, 'V' = inválido
   if (status != "A") {
     // sin fix
     return false;
   }
 
   String latStr = campos[3];
   String latH   = campos[4];
   String lonStr = campos[5];
   String lonH   = campos[6];
 
   if (latStr.length() < 4 || lonStr.length() < 5) {
     return false;
   }
 
   double latDec = nmeaToDecimal(latStr, (latH.length() > 0 ? latH.charAt(0) : 'N'));
   double lonDec = nmeaToDecimal(lonStr, (lonH.length() > 0 ? lonH.charAt(0) : 'E'));
 
   gpsLat = latDec;
   gpsLon = lonDec;
 
   return true;
 }
 
 // Convierte campo NMEA ddmm.mmmm o dddmm.mmmm a decimal
 double nmeaToDecimal(const String &campo, char hemisferio) {
   // Buscar el punto decimal
   int punto = campo.indexOf('.');
   if (punto == -1) {
     return 0.0;
   }
 
   // Dos o tres dígitos de grados según sea lat/lon
   int gradosEnterosLen = (campo.length() - punto > 4) ? 3 : 2; 
   // Heurística: lon suele tener 3 grados, lat 2
 
   String gradosStr = campo.substring(0, gradosEnterosLen);
   String minutosStr = campo.substring(gradosEnterosLen);
 
   double grados  = gradosStr.toDouble();
   double minutos = minutosStr.toDouble();
 
   double decimal = grados + (minutos / 60.0);
 
   if (hemisferio == 'S' || hemisferio == 'W') {
     decimal = -decimal;
   }
 
   return decimal;
 }