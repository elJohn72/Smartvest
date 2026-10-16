#include <Arduino.h>
#include <Preferences.h>
#include <WiFi.h>
#include <WiFiProv.h>
#include <esp_coexist.h>
#include <esp_system.h>
#include <esp_wifi.h>
#include <wifi_provisioning/manager.h>

// Defined in the Arduino core. Referencing it pulls the real btInUse(),
// which keeps Bluetooth memory available. Without that, provisioning fails.
extern bool btStart();
__attribute__((used)) static bool (*const keepBluetoothLinked)() = btStart;

#include "prov_uuid.h"

static char bleName[32] = "";
static bool advertiseSetup = false;
static bool phoneWifi = false;

static void provEvent(arduino_event_t* event) {
  switch (event->event_id) {
    case ARDUINO_EVENT_PROV_CRED_SUCCESS: {
      Preferences prefs;
      prefs.begin("smartvest", false);
      prefs.putBool("wifi_saved", true);
      prefs.end();
      Serial.println("SMARTVEST:PROV:wifi_ok");
      break;
    }
    case ARDUINO_EVENT_PROV_CRED_FAIL:
      Serial.println("SMARTVEST:PROV:wifi_fail");
      // Allow the phone to retry after a wrong password without rebooting.
      wifi_prov_mgr_reset_sm_state_on_failure();
      break;
    case ARDUINO_EVENT_PROV_END:
      Serial.println("SMARTVEST:PROV:end");
      break;
    case ARDUINO_EVENT_WIFI_STA_GOT_IP:
      Serial.print("SMARTVEST:WIFI:");
      Serial.println(WiFi.localIP());
      break;
    default:
      break;
  }
}

void startSmartVestWifiProvision() {
  phoneWifi = true;
  (void)keepBluetoothLinked;
  uint8_t mac[6] = {};
  esp_read_mac(mac, ESP_MAC_WIFI_STA);
  snprintf(bleName, sizeof(bleName), "PROV_SmartVest_%02X%02X", mac[4], mac[5]);

  Preferences prefs;
  prefs.begin("smartvest", false);
  const bool saved = prefs.getBool("wifi_saved", false);
  prefs.end();

  WiFi.onEvent(provEvent);
  uint8_t uuid[16];
  memcpy(uuid, SMARTVEST_PROV_SERVICE_UUID_LE, sizeof(uuid));
  // Un Wi-Fi.mode previo deja la placa como "ya configurada" y no enciende el Bluetooth.
  WiFiProv.beginProvision(
      WIFI_PROV_SCHEME_BLE,
      WIFI_PROV_SCHEME_HANDLER_NONE,
      WIFI_PROV_SECURITY_0,
      nullptr,
      bleName,
      nullptr,
      uuid,
      !saved);
  if (!saved) {
    advertiseSetup = true;
    esp_coex_preference_set(ESP_COEX_PREFER_BT);
  } else {
    esp_coex_preference_set(ESP_COEX_PREFER_WIFI);
  }

  Serial.print("SMARTVEST:VEST:");
  Serial.println(bleName);
  Serial.println(saved ? "SMARTVEST:PROV:saved" : "SMARTVEST:PROV:ble");
}

static volatile unsigned long lastLabOkMs = 0;

void smartVestNoteLabReachable() {
  lastLabOkMs = millis();
}

static void forgetNetworkAndRestart() {
  Preferences prefs;
  prefs.begin("smartvest", false);
  prefs.putBool("wifi_saved", false);
  prefs.end();
  wifi_config_t empty = {};
  esp_wifi_set_config(WIFI_IF_STA, &empty);
  WiFi.disconnect(true, true);
  Serial.println("SMARTVEST:PROV:otra_red");
  Serial.flush();
  delay(200);
  esp_restart();
}

void smartVestAllowNewNetwork(unsigned long now) {
  static bool leaving = false;
  static unsigned long lostSince = 0;
  static unsigned long ipSince = 0;
  (void)now;
  if (!phoneWifi || advertiseSetup || leaving) return;

  const unsigned long tick = millis();
  const unsigned long labOk = lastLabOkMs;
  const bool labFresh = labOk != 0 && (labOk > tick || tick - labOk < 90000UL);

  if (WiFi.status() == WL_CONNECTED) {
    lostSince = 0;
    if (ipSince == 0) ipSince = tick;
    if (labFresh || (labOk == 0 && tick - ipSince < 90000UL)) return;
    leaving = true;
    forgetNetworkAndRestart();
    return;
  }

  ipSince = 0;
  if (lostSince == 0) lostSince = tick;
  if (tick - lostSince < 25000UL) return;
  leaving = true;
  forgetNetworkAndRestart();
}

bool smartVestSetupAdvertising() {
  return advertiseSetup;
}

bool smartVestPhoneProvidesWifi() {
  return phoneWifi;
}
