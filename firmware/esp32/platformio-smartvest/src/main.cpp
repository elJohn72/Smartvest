#include <Arduino.h>
#include <HTTPClient.h>
#include <WiFi.h>

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
constexpr int8_t batteryAdc = -1;
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
constexpr float kDangerDistanceCm = 40.0f;
constexpr float kAlertDistanceCm = 100.0f;
constexpr float kCautionDistanceCm = 200.0f;

float measureDistanceCm();
void processGpsStream();
bool parseGprmc(const String& line);
double nmeaToDecimal(const String& raw, char hemisphere);
ObstacleLevel getObstacleLevel(float distanceCm);
const char* obstacleLevelToText(ObstacleLevel level);
AlertPattern getPatternForDistance(float distanceCm);
void applyAlertPattern();
void updateBatteryLevel();
void ensureWifiConnection();
void publishTelemetryToApi();
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

  WiFi.mode(WIFI_STA);

  if (SMARTVEST_ENABLE_SIM800) {
    delay(1500);
    sendAtCommand("AT", "OK", 2000);
    sendAtCommand("ATE0", "OK", 2000);
    sendAtCommand("AT+CMGF=1", "OK", 2000);
  }

  Serial.println();
  Serial.println("SmartVest PlatformIO iniciado");
  Serial.print("deviceId=");
  Serial.println(SMARTVEST_DEVICE_ID);
}

void loop() {
  const unsigned long now = millis();

  processGpsStream();

  if (now - lastDistanceReadMs >= kDistanceReadIntervalMs) {
    lastDistanceReadMs = now;
    currentDistanceCm = measureDistanceCm();
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

    if (SMARTVEST_ENABLE_SIM800) {
      String message = String("ALERTA SOS SmartVest\n") +
                       "deviceId: " + SMARTVEST_DEVICE_ID + "\n";

      if (gpsState.fix) {
        message += "Mapa: https://maps.google.com/?q=";
        message += String(gpsState.latitude, 6);
        message += ",";
        message += String(gpsState.longitude, 6);
      } else {
        message += "GPS sin fix valido";
      }

      sendSms(message);
    }
  }

  if (!sosPressed) {
    sosEdgeLatched = false;
  }

  applyAlertPattern();
  ensureWifiConnection();

  if (now - lastTelemetryMs >= kTelemetryIntervalMs) {
    lastTelemetryMs = now;
    printTelemetry();
  }

  if (SMARTVEST_WIFI_ENABLED && now - lastHttpPublishMs >= SMARTVEST_HTTP_INTERVAL_MS) {
    lastHttpPublishMs = now;
    publishTelemetryToApi();
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
  if (!SMARTVEST_WIFI_ENABLED || strlen(SMARTVEST_WIFI_SSID) == 0) {
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

void publishTelemetryToApi() {
  if (WiFi.status() != WL_CONNECTED || strlen(SMARTVEST_API_URL) == 0) {
    return;
  }

  HTTPClient http;
  http.begin(SMARTVEST_API_URL);
  http.addHeader("Content-Type", "application/json");

  String payload = String("{") +
                   "\"deviceId\":\"" + SMARTVEST_DEVICE_ID + "\"," +
                   "\"distanceCm\":" + String(currentDistanceCm, 1) + "," +
                   "\"latitude\":" + String(gpsState.fix ? gpsState.latitude : 0.0, 6) + "," +
                   "\"longitude\":" + String(gpsState.fix ? gpsState.longitude : 0.0, 6) + "," +
                   "\"sosActive\":" + String(sosPressed ? "true" : "false");

  if (batteryLevel >= 0) {
    payload += ",\"batteryLevel\":" + String(batteryLevel);
  }

  payload += "}";

  const int responseCode = http.POST(payload);
  if (responseCode > 0) {
    Serial.print("HTTP iot.php -> ");
    Serial.println(responseCode);
  } else {
    Serial.print("HTTP error -> ");
    Serial.println(http.errorToString(responseCode));
  }

  http.end();
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
  if (!sendAtCommand("AT+CMGF=1", "OK", 2000)) {
    return false;
  }

  while (gsmSerial.available()) {
    gsmSerial.read();
  }

  gsmSerial.print("AT+CMGS=\"");
  gsmSerial.print(SMARTVEST_SOS_PHONE);
  gsmSerial.print("\"\r");

  unsigned long startMs = millis();
  while (millis() - startMs < 5000UL) {
    if (gsmSerial.find(">")) {
      gsmSerial.print(message);
      gsmSerial.write(26);
      return gsmSerial.find("OK");
    }
  }

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
