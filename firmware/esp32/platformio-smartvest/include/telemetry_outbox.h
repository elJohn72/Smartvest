#pragma once

#include <stdint.h>
#include <string.h>

// Política de envío compartida por el firmware y por la prueba de host.
// No depende de Arduino ni de FreeRTOS.

enum TelemetryHttpClass {
  TELEMETRY_HTTP_SUCCESS = 0,
  TELEMETRY_HTTP_TRANSIENT = 1,
  TELEMETRY_HTTP_PERMANENT = 2
};

inline TelemetryHttpClass classify_telemetry_http(int code) {
  if (code == 200 || code == 202) {
    return TELEMETRY_HTTP_SUCCESS;
  }
  if (code == 400 || code == 401 || code == 403 || code == 404 || code == 409) {
    return TELEMETRY_HTTP_PERMANENT;
  }
  return TELEMETRY_HTTP_TRANSIENT;
}

inline int telemetry_backoff_ms(int previousMs) {
  if (previousMs < 2000) {
    return 2000;
  }
  const int next = previousMs * 2;
  if (next > 30000) {
    return 30000;
  }
  return next;
}

struct SosPending {
  bool pending;
  bool abandoned;
  char eventId[40];
  uint32_t sequence;
  int permanentHits;
};

inline void sos_pending_reset(SosPending* slot) {
  memset(slot, 0, sizeof(*slot));
}

// true = hay que encolar el evento ya, sin esperar el intervalo periódico.
// Una pulsación que termina antes del siguiente publish conserva id y secuencia.
inline bool sos_capture_press(SosPending* slot, uint32_t* sequenceCounter, const char* eventId) {
  if (slot->pending && !slot->abandoned) {
    return false;
  }
  sos_pending_reset(slot);
  slot->pending = true;
  if (eventId != nullptr) {
    strncpy(slot->eventId, eventId, sizeof(slot->eventId) - 1);
  }
  (*sequenceCounter)++;
  slot->sequence = *sequenceCounter;
  return true;
}

// Soltar el botón no confirma ni borra el evento.
inline void sos_note_release(SosPending* slot) {
  (void)slot;
}

inline void sos_apply_http(SosPending* slot, TelemetryHttpClass outcome, int maxPermanent) {
  if (!slot->pending) {
    return;
  }
  if (outcome == TELEMETRY_HTTP_SUCCESS) {
    slot->pending = false;
    slot->abandoned = false;
    slot->permanentHits = 0;
    return;
  }
  if (outcome == TELEMETRY_HTTP_PERMANENT) {
    slot->permanentHits++;
    if (slot->permanentHits >= maxPermanent) {
      slot->pending = false;
      slot->abandoned = true;
    }
  }
}

enum QueueFullDecision {
  QUEUE_DROP_OLDEST = 0,
  QUEUE_KEEP_OLDEST = 1
};

// Una cola llena puede reemplazar telemetría normal. Nunca desplaza un SOS ya encolado.
inline QueueFullDecision queue_full_decision(bool incomingIsSos, bool oldestIsSos) {
  (void)incomingIsSos;
  if (oldestIsSos) {
    return QUEUE_KEEP_OLDEST;
  }
  return QUEUE_DROP_OLDEST;
}
