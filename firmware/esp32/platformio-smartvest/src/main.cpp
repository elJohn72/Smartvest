#include <Arduino.h>
#include "telemetry_outbox.h"
#include "wifi_provision.h"
#include <HTTPClient.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>

#if __has_include("smartvest_config.h")
#include "smartvest_config.h"
#endif

#ifndef SMARTVEST_DEVICE_ID
#define SMARTVEST_DEVICE_ID "VEST-001"
#endif

#ifndef SMARTVEST_WIFI_ENABLED
#define SMARTVEST_WIFI_ENABLED false
#endif

#ifndef SMARTVEST_WIFI_SSID
#define SMARTVEST_WIFI_SSID ""
#endif

#ifndef SMARTVEST_WIFI_PASSWORD
#define SMARTVEST_WIFI_PASSWORD ""
#endif

#ifndef SMARTVEST_API_URL
#define SMARTVEST_API_URL ""
#endif

#ifndef SMARTVEST_HTTP_INTERVAL_MS
#define SMARTVEST_HTTP_INTERVAL_MS 5000UL
#endif

#ifndef SMARTVEST_ENABLE_SIM800
#define SMARTVEST_ENABLE_SIM800 false
#endif

#ifndef SMARTVEST_BATTERY_ADC_PIN
#define SMARTVEST_BATTERY_ADC_PIN -1
#endif

#ifndef SMARTVEST_SOS_PHONE
#define SMARTVEST_SOS_PHONE "+593000000000"
#endif

namespace Pins {
constexpr uint8_t trig = 5;
constexpr uint8_t echo = 18;
constexpr uint8_t buzzer = 13;
constexpr uint8_t vibrator = 32;
constexpr uint8_t sos = 27;

constexpr uint8_t gpsRx = 4;
constexpr uint8_t gpsTx = 14;
constexpr uint8_t gsmRx = 16;
constexpr uint8_t gsmTx = 17;

constexpr uint8_t camRx = 23;
constexpr uint8_t camTx = 22;
constexpr int8_t batteryAdc = SMARTVEST_BATTERY_ADC_PIN;
}

HardwareSerial gpsSerial(1);
HardwareSerial gsmSerial(2);

struct GpsState {
  double latitude = 0.0;
  double longitude = 0.0;
  bool fix = false;
  unsigned long lastSentenceMs = 0;
};

enum class ObstacleLevel {
  clear,
  caution,
  alert,
  danger,
};

struct AlertPattern {
  bool enabled = false;
  unsigned long periodMs = 0;
  unsigned long onMs = 0;

  AlertPattern() = default;
  AlertPattern(bool enabledValue, unsigned long periodValue, unsigned long onValue)
      : enabled(enabledValue), periodMs(periodValue), onMs(onValue) {}
};

GpsState gpsState;
String gpsLineBuffer;

float currentDistanceCm = 9999.0f;
bool sosPressed = false;
bool sosEdgeLatched = false;
int batteryLevel = -1;

// Identificadores de secuencia monotónica y arranque (Contrato v1)
char bootId[37] = {0};
unsigned long telemetrySequence = 0;
char currentSosEventId[37] = {0};

AlertPattern currentPattern;
unsigned long patternStartedAt = 0;
unsigned long lastDistanceReadMs = 0;
unsigned long lastTelemetryMs = 0;
unsigned long lastHttpPublishMs = 0;
unsigned long lastWifiAttemptMs = 0;

constexpr unsigned long kUltrasonicTimeoutUs = 30000UL;
constexpr unsigned long kDistanceReadIntervalMs = 150UL;
constexpr unsigned long kTelemetryIntervalMs = 1000UL;
constexpr unsigned long kWifiRetryIntervalMs = 10000UL;
constexpr unsigned long kHttpIntervalSosMs = 2000UL;
constexpr float kDangerDistanceCm = 40.0f;
constexpr float kAlertDistanceCm = 100.0f;
constexpr float kCautionDistanceCm = 200.0f;

struct TelemetryMessage {
  unsigned long sequence;
  float distanceCm;
  bool gpsFix;
  double latitude;
  double longitude;
  int batteryLevel;
  bool sosActive;
  char sosEventId[37];
};

struct SmsMessage {
  char text[160];
};

static QueueHandle_t telemetryQueue = nullptr;
static QueueHandle_t smsQueue = nullptr;
static TaskHandle_t networkTaskHandle = nullptr;
static portMUX_TYPE sosMux = portMUX_INITIALIZER_UNLOCKED;
static SosPending sosPolicy{};
static TelemetryMessage sosHeld{};
static bool sosHeldValid = false;
static int sosBackoffMs = 2000;
static unsigned long sosNextAttemptMs = 0;
static constexpr int kSosPermanentLimit = 3;

float measureDistanceCm();
float measureDistanceCmFiltered();
void processGpsStream();
bool parseGprmc(const String& line);
double nmeaToDecimal(const String& raw, char hemisphere);
ObstacleLevel getObstacleLevel(float distanceCm);
const char* obstacleLevelToText(ObstacleLevel level);
AlertPattern getPatternForDistance(float distanceCm);
void applyAlertPattern();
void applySosAlertPattern();
void updateBatteryLevel();
void ensureWifiConnection();
void enqueueTelemetryMessage();
void networkTask(void* pvParameters);
TelemetryHttpClass sendTelemetryHttp(const TelemetryMessage& msg);
void makeEventId(char* out, size_t outLen);
void fillTelemetryMessage(TelemetryMessage& msg, unsigned long sequence, bool sosActive, const char* eventId);
void captureSosEdge();
void printTelemetry();
bool sendSms(const String& message);
bool sendAtCommand(const char* command, const char* expected, unsigned long timeoutMs);
String buildTelemetryJson();

void setup() {
  Serial.begin(115200);
  delay(500);

  pinMode(Pins::trig, OUTPUT);
  pinMode(Pins::echo, INPUT);
  pinMode(Pins::buzzer, OUTPUT);
  pinMode(Pins::vibrator, OUTPUT);
  pinMode(Pins::sos, INPUT_PULLUP);

  digitalWrite(Pins::trig, LOW);
  digitalWrite(Pins::buzzer, LOW);
  digitalWrite(Pins::vibrator, LOW);

  gpsSerial.begin(9600, SERIAL_8N1, Pins::gpsRx, Pins::gpsTx);
  gsmSerial.begin(9600, SERIAL_8N1, Pins::gsmRx, Pins::gsmTx);

  if (Pins::batteryAdc >= 0) {
    analogReadResolution(12);
    analogSetAttenuation(ADC_11db);
  }

  WiFi.mode(WIFI_STA);
  startSmartVestWifiProvision();

  if (SMARTVEST_ENABLE_SIM800) {
    delay(1500);
    sendAtCommand("AT", "OK", 2000);
    sendAtCommand("ATE0", "OK", 2000);
    sendAtCommand("AT+CMGF=1", "OK", 2000);
  }

  // Generar bootId criptográfico único para este ciclo de encendido (UUID v4 format)
  uint8_t randBytes[16];
  for (int i = 0; i < 16; i += 4) {
    uint32_t r = esp_random();
    memcpy(&randBytes[i], &r, 4);
  }
  randBytes[6] = (randBytes[6] & 0x0F) | 0x40; // Versión 4
  randBytes[8] = (randBytes[8] & 0x3F) | 0x80; // Variante RFC 4122
  snprintf(bootId, sizeof(bootId),
           "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
           randBytes[0], randBytes[1], randBytes[2], randBytes[3],
           randBytes[4], randBytes[5], randBytes[6], randBytes[7],
           randBytes[8], randBytes[9], randBytes[10], randBytes[11],
           randBytes[12], randBytes[13], randBytes[14], randBytes[15]);

  // Crear colas acotadas para telemetría y SMS para no agotar RAM si la red o el módem caen
  telemetryQueue = xQueueCreate(10, sizeof(TelemetryMessage));
  smsQueue = xQueueCreate(3, sizeof(SmsMessage));
  if (telemetryQueue != nullptr) {
    xTaskCreatePinnedToCore(
        networkTask,
        "net_task",
        8192,
        nullptr,
        1,
        &networkTaskHandle,
        0 // Ejecutar en Core 0, dejando Core 1 para el bucle crítico de sensores y alarmas
    );
  }

  Serial.println();
  Serial.println("SmartVest PlatformIO iniciado (Contrato v1 + TLS FreeRTOS Task)");
  Serial.print("deviceId=");
  Serial.println(SMARTVEST_DEVICE_ID);
  Serial.print("bootId=");
  Serial.println(bootId);
}

void loop() {
  const unsigned long now = millis();

  processGpsStream();

  if (now - lastDistanceReadMs >= kDistanceReadIntervalMs) {
    lastDistanceReadMs = now;
    currentDistanceCm = measureDistanceCmFiltered();
    const AlertPattern nextPattern = getPatternForDistance(currentDistanceCm);
    if (nextPattern.enabled != currentPattern.enabled ||
        nextPattern.periodMs != currentPattern.periodMs ||
        nextPattern.onMs != currentPattern.onMs) {
      currentPattern = nextPattern;
      patternStartedAt = now;
    } else {
      currentPattern = nextPattern;
    }
    updateBatteryLevel();
  }

  sosPressed = digitalRead(Pins::sos) == LOW;
  if (sosPressed && !sosEdgeLatched) {
    sosEdgeLatched = true;
    captureSosEdge();

    if (SMARTVEST_ENABLE_SIM800 && smsQueue != nullptr) {
      SmsMessage smsMsg;
      String message = String("ALERTA SOS SmartVest\n") +
                       "deviceId: " + SMARTVEST_DEVICE_ID + "\n" +
                       "Distancia: " + String(currentDistanceCm, 0) + " cm\n";

      if (gpsState.fix) {
        message += "Mapa: https://maps.google.com/?q=";
        message += String(gpsState.latitude, 6);
        message += ",";
        message += String(gpsState.longitude, 6);
      } else {
        message += "GPS sin fix. Abre el perfil web del cuidador.";
      }

      strncpy(smsMsg.text, message.c_str(), sizeof(smsMsg.text) - 1);
      smsMsg.text[sizeof(smsMsg.text) - 1] = '\0';

      if (xQueueSend(smsQueue, &smsMsg, 0) != pdTRUE) {
        Serial.println("SMS: cola llena, descartando SMS duplicado");
      }
    }
  }

  if (!sosPressed) {
    sosEdgeLatched = false;
    sos_note_release(&sosPolicy);
  }

  if (sosPressed) {
    applySosAlertPattern();
  } else {
    applyAlertPattern();
  }

  ensureWifiConnection();

  if (now - lastTelemetryMs >= kTelemetryIntervalMs) {
    lastTelemetryMs = now;
    printTelemetry();
  }

  const unsigned long httpInterval =
      sosPressed ? kHttpIntervalSosMs : SMARTVEST_HTTP_INTERVAL_MS;

  const bool labWifiReady = SMARTVEST_WIFI_ENABLED || WiFi.status() == WL_CONNECTED;
  if (labWifiReady && now - lastHttpPublishMs >= httpInterval) {
    lastHttpPublishMs = now;
    enqueueTelemetryMessage();
  }
}

float measureDistanceCm() {
  digitalWrite(Pins::trig, LOW);
  delayMicroseconds(2);
  digitalWrite(Pins::trig, HIGH);
  delayMicroseconds(10);
  digitalWrite(Pins::trig, LOW);

  const unsigned long duration = pulseIn(Pins::echo, HIGH, kUltrasonicTimeoutUs);
  if (duration == 0) {
    return 9999.0f;
  }

  return (duration * 0.0343f) / 2.0f;
}

float measureDistanceCmFiltered() {
  float samples[3] = {9999.0f, 9999.0f, 9999.0f};

  for (int i = 0; i < 3; ++i) {
    samples[i] = measureDistanceCm();
    delayMicroseconds(120);
  }

  for (int i = 0; i < 2; ++i) {
    for (int j = i + 1; j < 3; ++j) {
      if (samples[j] < samples[i]) {
        const float swap = samples[i];
        samples[i] = samples[j];
        samples[j] = swap;
      }
    }
  }

  return samples[1];
}

void processGpsStream() {
  while (gpsSerial.available()) {
    const char c = static_cast<char>(gpsSerial.read());

    if (c == '\r') {
      continue;
    }

    if (c == '\n') {
      if (!gpsLineBuffer.isEmpty()) {
        parseGprmc(gpsLineBuffer);
        gpsLineBuffer = "";
      }
      continue;
    }

    gpsLineBuffer += c;
  }
}

bool parseGprmc(const String& line) {
  if (!line.startsWith("$GPRMC") && !line.startsWith("$GNRMC")) {
    return false;
  }

  String fields[16];
  int fieldCount = 0;
  int start = 0;

  for (int i = 0; i <= line.length() && fieldCount < 16; ++i) {
    if (i == line.length() || line.charAt(i) == ',') {
      fields[fieldCount++] = line.substring(start, i);
      start = i + 1;
    }
  }

  if (fieldCount < 7) {
    return false;
  }

  const bool validFix = fields[2] == "A";
  gpsState.fix = validFix;
  gpsState.lastSentenceMs = millis();

  if (!validFix) {
    return true;
  }

  if (fields[3].isEmpty() || fields[4].isEmpty() || fields[5].isEmpty() || fields[6].isEmpty()) {
    gpsState.fix = false;
    return false;
  }

  gpsState.latitude = nmeaToDecimal(fields[3], fields[4].charAt(0));
  gpsState.longitude = nmeaToDecimal(fields[5], fields[6].charAt(0));
  return true;
}

double nmeaToDecimal(const String& raw, char hemisphere) {
  const double value = raw.toDouble();
  const int degrees = static_cast<int>(value / 100.0);
  const double minutes = value - (degrees * 100.0);
  double decimal = degrees + (minutes / 60.0);

  if (hemisphere == 'S' || hemisphere == 'W') {
    decimal *= -1.0;
  }

  return decimal;
}

ObstacleLevel getObstacleLevel(float distanceCm) {
  if (distanceCm <= kDangerDistanceCm) {
    return ObstacleLevel::danger;
  }

  if (distanceCm <= kAlertDistanceCm) {
    return ObstacleLevel::alert;
  }

  if (distanceCm <= kCautionDistanceCm) {
    return ObstacleLevel::caution;
  }

  return ObstacleLevel::clear;
}

const char* obstacleLevelToText(ObstacleLevel level) {
  switch (level) {
    case ObstacleLevel::danger:
      return "danger";
    case ObstacleLevel::alert:
      return "alert";
    case ObstacleLevel::caution:
      return "caution";
    case ObstacleLevel::clear:
    default:
      return "clear";
  }
}

AlertPattern getPatternForDistance(float distanceCm) {
  switch (getObstacleLevel(distanceCm)) {
    case ObstacleLevel::danger:
      return {true, 400UL, 300UL};
    case ObstacleLevel::alert:
      return {true, 600UL, 200UL};
    case ObstacleLevel::caution:
      return {true, 1000UL, 200UL};
    case ObstacleLevel::clear:
    default:
      return {false, 0UL, 0UL};
  }
}

void applyAlertPattern() {
  if (!currentPattern.enabled || currentPattern.periodMs == 0) {
    digitalWrite(Pins::buzzer, LOW);
    digitalWrite(Pins::vibrator, LOW);
    return;
  }

  const unsigned long elapsed = (millis() - patternStartedAt) % currentPattern.periodMs;
  const bool outputOn = elapsed < currentPattern.onMs;

  digitalWrite(Pins::buzzer, outputOn ? HIGH : LOW);
  digitalWrite(Pins::vibrator, outputOn ? HIGH : LOW);
}

void applySosAlertPattern() {
  constexpr unsigned long kSosPeriodMs = 280UL;
  constexpr unsigned long kSosOnMs = 200UL;
  const unsigned long elapsed = millis() % kSosPeriodMs;
  const bool outputOn = elapsed < kSosOnMs;

  digitalWrite(Pins::buzzer, outputOn ? HIGH : LOW);
  digitalWrite(Pins::vibrator, outputOn ? HIGH : LOW);
}

void updateBatteryLevel() {
  if (Pins::batteryAdc < 0) {
    batteryLevel = -1;
    return;
  }

  const int raw = analogRead(Pins::batteryAdc);
  batteryLevel = map(raw, 1800, 3200, 0, 100);
  batteryLevel = constrain(batteryLevel, 0, 100);
}

void ensureWifiConnection() {
  if (smartVestPhoneProvidesWifi() || !SMARTVEST_WIFI_ENABLED || strlen(SMARTVEST_WIFI_SSID) == 0) {
    return;
  }

  if (WiFi.status() == WL_CONNECTED) {
    return;
  }

  const unsigned long now = millis();
  if (now - lastWifiAttemptMs < kWifiRetryIntervalMs) {
    return;
  }

  lastWifiAttemptMs = now;
  WiFi.begin(SMARTVEST_WIFI_SSID, SMARTVEST_WIFI_PASSWORD);
}

void makeEventId(char* out, size_t outLen) {
  uint8_t randBytes[16];
  for (int i = 0; i < 16; i += 4) {
    uint32_t r = esp_random();
    memcpy(&randBytes[i], &r, 4);
  }
  randBytes[6] = (randBytes[6] & 0x0F) | 0x40;
  randBytes[8] = (randBytes[8] & 0x3F) | 0x80;
  snprintf(out, outLen,
           "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
           randBytes[0], randBytes[1], randBytes[2], randBytes[3],
           randBytes[4], randBytes[5], randBytes[6], randBytes[7],
           randBytes[8], randBytes[9], randBytes[10], randBytes[11],
           randBytes[12], randBytes[13], randBytes[14], randBytes[15]);
}

void fillTelemetryMessage(TelemetryMessage& msg, unsigned long sequence, bool activeSos, const char* eventId) {
  msg.sequence = sequence;
  msg.distanceCm = currentDistanceCm;
  msg.gpsFix = gpsState.fix;
  msg.latitude = gpsState.latitude;
  msg.longitude = gpsState.longitude;
  msg.batteryLevel = batteryLevel;
  msg.sosActive = activeSos;
  if (eventId != nullptr) {
    strncpy(msg.sosEventId, eventId, sizeof(msg.sosEventId) - 1);
  } else {
    msg.sosEventId[0] = '\0';
  }
  msg.sosEventId[sizeof(msg.sosEventId) - 1] = '\0';
}

void captureSosEdge() {
  char eventId[40] = {0};
  makeEventId(eventId, sizeof(eventId));
  uint32_t sequence = static_cast<uint32_t>(telemetrySequence);
  portENTER_CRITICAL(&sosMux);
  const bool immediate = sos_capture_press(&sosPolicy, &sequence, eventId);
  if (immediate) {
    telemetrySequence = sequence;
    fillTelemetryMessage(sosHeld, sosPolicy.sequence, true, sosPolicy.eventId);
    sosHeldValid = true;
    sosBackoffMs = 2000;
    sosNextAttemptMs = 0;
    strncpy(currentSosEventId, sosPolicy.eventId, sizeof(currentSosEventId) - 1);
    currentSosEventId[sizeof(currentSosEventId) - 1] = '\0';
  }
  portEXIT_CRITICAL(&sosMux);
}

void enqueueTelemetryMessage() {
  if (telemetryQueue == nullptr) return;

  telemetrySequence++;

  portENTER_CRITICAL(&sosMux);
  const bool includeSos = sosPolicy.pending && !sosPolicy.abandoned;
  char eventCopy[40] = {0};
  if (includeSos) {
    strncpy(eventCopy, sosPolicy.eventId, sizeof(eventCopy) - 1);
  }
  portEXIT_CRITICAL(&sosMux);

  TelemetryMessage msg;
  fillTelemetryMessage(msg, telemetrySequence, includeSos, includeSos ? eventCopy : "");

  if (xQueueSend(telemetryQueue, &msg, 0) == pdTRUE) {
    return;
  }

  TelemetryMessage dropped;
  if (xQueueReceive(telemetryQueue, &dropped, 0) != pdTRUE) {
    Serial.println("Telemetria descartada: cola llena");
    return;
  }
  if (queue_full_decision(msg.sosActive, dropped.sosActive) == QUEUE_KEEP_OLDEST) {
    if (xQueueSend(telemetryQueue, &dropped, 0) != pdTRUE) {
      Serial.println("No se pudo reponer el SOS retenido");
    }
    return;
  }
  if (xQueueSend(telemetryQueue, &msg, 0) != pdTRUE) {
    if (xQueueSend(telemetryQueue, &dropped, 0) != pdTRUE) {
      Serial.println("Telemetria perdida al reponer la cola");
    }
  }
}

void noteSosHttp(const TelemetryMessage& msg, TelemetryHttpClass outcome) {
  if (!msg.sosActive || msg.sosEventId[0] == '\0') {
    return;
  }
  portENTER_CRITICAL(&sosMux);
  if (sosPolicy.pending && strcmp(sosPolicy.eventId, msg.sosEventId) == 0) {
    sos_apply_http(&sosPolicy, outcome, kSosPermanentLimit);
    if (!sosPolicy.pending) {
      sosHeldValid = false;
      sosBackoffMs = 2000;
      if (outcome == TELEMETRY_HTTP_SUCCESS) {
        currentSosEventId[0] = '\0';
      }
    } else if (outcome == TELEMETRY_HTTP_TRANSIENT || outcome == TELEMETRY_HTTP_PERMANENT) {
      sosBackoffMs = telemetry_backoff_ms(sosBackoffMs);
      sosNextAttemptMs = millis() + static_cast<unsigned long>(sosBackoffMs);
    }
  }
  portEXIT_CRITICAL(&sosMux);
}

void networkTask(void* pvParameters) {
  configTime(0, 0, "pool.ntp.org", "time.google.com");

  TelemetryMessage msg;
  SmsMessage smsMsg;

  while (true) {
    if (smsQueue != nullptr && xQueueReceive(smsQueue, &smsMsg, 0) == pdTRUE) {
      sendSms(String(smsMsg.text));
    }

    bool sosReady = false;
    TelemetryMessage sosCopy{};
    portENTER_CRITICAL(&sosMux);
    if (sosHeldValid && sosPolicy.pending && millis() >= sosNextAttemptMs) {
      sosCopy = sosHeld;
      sosReady = true;
    }
    portEXIT_CRITICAL(&sosMux);

    if (sosReady) {
      TelemetryHttpClass outcome = TELEMETRY_HTTP_TRANSIENT;
      if (WiFi.status() == WL_CONNECTED && strlen(SMARTVEST_API_URL) > 0) {
        outcome = sendTelemetryHttp(sosCopy);
      }
      noteSosHttp(sosCopy, outcome);
      vTaskDelay(pdMS_TO_TICKS(20));
      continue;
    }

    if (xQueueReceive(telemetryQueue, &msg, pdMS_TO_TICKS(100)) == pdTRUE) {
      if (WiFi.status() == WL_CONNECTED && strlen(SMARTVEST_API_URL) > 0) {
        const TelemetryHttpClass outcome = sendTelemetryHttp(msg);
        noteSosHttp(msg, outcome);
      }
    }
    vTaskDelay(pdMS_TO_TICKS(20));
  }
}

TelemetryHttpClass sendTelemetryHttp(const TelemetryMessage& msg) {
  HTTPClient http;
  WiFiClientSecure secureClient;
  WiFiClient plainClient;

  bool isHttps = (strncmp(SMARTVEST_API_URL, "https://", 8) == 0);

  if (isHttps) {
#if defined(SMARTVEST_CA_CERT_PEM)
    if (strlen(SMARTVEST_CA_CERT_PEM) == 0) {
      Serial.println("HTTPS abortado: SMARTVEST_CA_CERT_PEM vacio");
      return TELEMETRY_HTTP_PERMANENT;
    }
    secureClient.setCACert(SMARTVEST_CA_CERT_PEM);
#else
    Serial.println("HTTPS abortado: SMARTVEST_CA_CERT_PEM no definido");
    return TELEMETRY_HTTP_PERMANENT;
#endif
    secureClient.setTimeout(4);
    if (!http.begin(secureClient, SMARTVEST_API_URL)) {
      Serial.println("HTTP begin TLS fallo");
      return TELEMETRY_HTTP_TRANSIENT;
    }
  } else {
    plainClient.setTimeout(4);
    if (!http.begin(plainClient, SMARTVEST_API_URL)) {
      Serial.println("HTTP begin plain fallo");
      return TELEMETRY_HTTP_TRANSIENT;
    }
  }

  http.setTimeout(4000);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-SmartVest-Api-Key", SMARTVEST_IOT_API_KEY);

  String payload = String("{") +
                   "\"deviceId\":\"" + SMARTVEST_DEVICE_ID + "\"," +
                   "\"bootId\":\"" + String(bootId) + "\"," +
                   "\"sequence\":" + String(msg.sequence) + "," +
                   "\"distanceCm\":" + (msg.distanceCm < 9000.0f ? String(msg.distanceCm, 1) : "null") + "," +
                   "\"gpsFix\":" + (msg.gpsFix ? "true" : "false") + "," +
                   "\"latitude\":" + (msg.gpsFix ? String(msg.latitude, 6) : "null") + "," +
                   "\"longitude\":" + (msg.gpsFix ? String(msg.longitude, 6) : "null") + "," +
                   "\"batteryLevel\":" + (msg.batteryLevel >= 0 ? String(msg.batteryLevel) : "null") + "," +
                   "\"sosActive\":" + (msg.sosActive ? "true" : "false") + "," +
                   "\"sosEventId\":" + (strlen(msg.sosEventId) > 0 ? ("\"" + String(msg.sosEventId) + "\"") : "null") +
                   "}";

  const int responseCode = http.POST(payload);
  if (responseCode > 0) {
    Serial.printf("Telemetry HTTP v1 [%lu] -> %d\n", msg.sequence, responseCode);
  } else {
    Serial.print("Telemetry HTTP error -> ");
    Serial.println(http.errorToString(responseCode));
  }
  http.end();
  return classify_telemetry_http(responseCode);
}

void printTelemetry() {
  Serial.println(buildTelemetryJson());
}

String buildTelemetryJson() {
  String json = String("{") +
                "\"deviceId\":\"" + SMARTVEST_DEVICE_ID + "\"," +
                "\"distanceCm\":" + String(currentDistanceCm, 1) + "," +
                "\"obstacle\":\"" + obstacleLevelToText(getObstacleLevel(currentDistanceCm)) + "\"," +
                "\"gpsFix\":" + String(gpsState.fix ? "true" : "false") + "," +
                "\"latitude\":" + String(gpsState.latitude, 6) + "," +
                "\"longitude\":" + String(gpsState.longitude, 6) + "," +
                "\"sosActive\":" + String(sosPressed ? "true" : "false") + ",";

  if (batteryLevel >= 0) {
    json += "\"batteryLevel\":" + String(batteryLevel);
  } else {
    json += "\"batteryLevel\":null";
  }

  json += "}";
  return json;
}

bool sendSms(const String& message) {
  if (!SMARTVEST_ENABLE_SIM800 || strlen(SMARTVEST_SOS_PHONE) < 8) {
    return false;
  }

  if (!sendAtCommand("AT", "OK", 3000)) {
    Serial.println("SMS: modulo GSM sin respuesta AT");
    return false;
  }

  if (!sendAtCommand("AT+CMGF=1", "OK", 3000)) {
    Serial.println("SMS: modo texto no disponible");
    return false;
  }

  while (gsmSerial.available()) {
    gsmSerial.read();
  }

  Serial.print("SMS destino: ");
  Serial.println(SMARTVEST_SOS_PHONE);

  gsmSerial.print("AT+CMGS=\"");
  gsmSerial.print(SMARTVEST_SOS_PHONE);
  gsmSerial.print("\"\r");

  unsigned long startMs = millis();
  while (millis() - startMs < 15000UL) {
    if (gsmSerial.find(">")) {
      gsmSerial.print(message);
      gsmSerial.write(26);
      if (gsmSerial.find("OK")) {
        return true;
      }
      Serial.println("SMS: envio sin confirmacion OK");
      return false;
    }
  }

  Serial.println("SMS: timeout esperando prompt >");
  return false;
}

bool sendAtCommand(const char* command, const char* expected, unsigned long timeoutMs) {
  while (gsmSerial.available()) {
    gsmSerial.read();
  }

  gsmSerial.print(command);
  gsmSerial.print("\r");

  const unsigned long startMs = millis();
  String response;

  while (millis() - startMs < timeoutMs) {
    while (gsmSerial.available()) {
      response += static_cast<char>(gsmSerial.read());
      if (response.indexOf(expected) >= 0) {
        return true;
      }
    }
  }

  return false;
}
