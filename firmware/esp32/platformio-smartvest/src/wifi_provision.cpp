#include <Arduino.h>
#include <Preferences.h>
#include <WiFi.h>
#include <WiFiProv.h>
#include <esp_coexist.h>
#include <esp_random.h>
#include <wifi_provisioning/manager.h>

#include "prov_uuid.h"

static char bleName[32] = "";
static char blePop[12] = "";
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

static void ensurePop(Preferences& prefs) {
  String stored = prefs.getString("pop", "");
  if (stored.length() != 8) {
    const char charset[] = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    char generated[9];
    for (int i = 0; i < 8; i++) {
      generated[i] = charset[esp_random() % (sizeof(charset) - 1)];
    }
    generated[8] = '\0';
    prefs.putString("pop", generated);
    stored = generated;
  }
  snprintf(blePop, sizeof(blePop), "%s", stored.c_str());
}

void startSmartVestWifiProvision() {
  phoneWifi = true;
  uint8_t mac[6];
  WiFi.macAddress(mac);
  snprintf(bleName, sizeof(bleName), "PROV_SmartVest_%02X%02X", mac[4], mac[5]);

  Preferences prefs;
  prefs.begin("smartvest", false);
  ensurePop(prefs);
  const bool saved = prefs.getBool("wifi_saved", false);
  prefs.end();

  WiFi.onEvent(provEvent);
  uint8_t uuid[16];
  memcpy(uuid, SMARTVEST_PROV_SERVICE_UUID_LE, sizeof(uuid));
  WiFiProv.beginProvision(
      WIFI_PROV_SCHEME_BLE,
      WIFI_PROV_SCHEME_HANDLER_NONE,
      WIFI_PROV_SECURITY_1,
      blePop,
      bleName,
      nullptr,
      uuid,
      false);
  esp_coex_preference_set(ESP_COEX_PREFER_WIFI);

  if (!saved) {
    advertiseSetup = true;
    Serial.print("SMARTVEST:SETUP:");
    Serial.print("{\"ver\":\"v1\",\"name\":\"");
    Serial.print(bleName);
    Serial.print("\",\"pop\":\"");
    Serial.print(blePop);
    Serial.println("\",\"transport\":\"ble\"}");
  }
}

bool smartVestSetupAdvertising() {
  return advertiseSetup;
}

bool smartVestPhoneProvidesWifi() {
  return phoneWifi;
}
