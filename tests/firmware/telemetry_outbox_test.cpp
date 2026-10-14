#include "telemetry_outbox.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int failures = 0;

static void expect(bool cond, const char* message) {
  if (!cond) {
    fprintf(stderr, "FAIL: %s\n", message);
    failures++;
  }
}

int main() {
  expect(classify_telemetry_http(200) == TELEMETRY_HTTP_SUCCESS, "200 es éxito de duplicado");
  expect(classify_telemetry_http(202) == TELEMETRY_HTTP_SUCCESS, "202 es éxito");
  expect(classify_telemetry_http(429) == TELEMETRY_HTTP_TRANSIENT, "429 se reintenta");
  expect(classify_telemetry_http(500) == TELEMETRY_HTTP_TRANSIENT, "500 se reintenta");
  expect(classify_telemetry_http(0) == TELEMETRY_HTTP_TRANSIENT, "sin respuesta se reintenta");
  expect(classify_telemetry_http(401) == TELEMETRY_HTTP_PERMANENT, "401 no entra en bucle");
  expect(classify_telemetry_http(403) == TELEMETRY_HTTP_PERMANENT, "403 no entra en bucle");

  SosPending slot{};
  uint32_t sequence = 10;
  const bool immediate = sos_capture_press(&slot, &sequence, "evt-corto");
  expect(immediate, "el flanco encola de inmediato");
  expect(slot.pending, "el evento queda pendiente");
  expect(strcmp(slot.eventId, "evt-corto") == 0, "un solo id");
  const uint32_t pressedSequence = slot.sequence;
  expect(pressedSequence == 11, "la secuencia se asigna en el flanco");

  // La pulsación termina antes del intervalo de publicación.
  sos_note_release(&slot);
  expect(slot.pending, "soltar el botón no borra el SOS");
  expect(slot.sequence == pressedSequence, "la secuencia no cambia al soltar");
  expect(strcmp(slot.eventId, "evt-corto") == 0, "el id sobrevive a la pulsación corta");

  const bool second = sos_capture_press(&slot, &sequence, "evt-otro");
  expect(!second, "no se genera otro id mientras el primero no está confirmado");
  expect(strcmp(slot.eventId, "evt-corto") == 0, "se conserva el primer id");

  sos_apply_http(&slot, TELEMETRY_HTTP_TRANSIENT, 3);
  expect(slot.pending, "429/500 no dan el envío por terminado");
  expect(slot.sequence == pressedSequence, "el reintento conserva la secuencia");

  sos_apply_http(&slot, TELEMETRY_HTTP_SUCCESS, 3);
  expect(!slot.pending, "202 confirma y libera el evento");

  SosPending denied{};
  uint32_t deniedSeq = 0;
  expect(sos_capture_press(&denied, &deniedSeq, "evt-401"), "nuevo evento tras confirmar");
  sos_apply_http(&denied, TELEMETRY_HTTP_PERMANENT, 3);
  sos_apply_http(&denied, TELEMETRY_HTTP_PERMANENT, 3);
  expect(denied.pending, "401 todavía no se abandona al primer golpe");
  sos_apply_http(&denied, TELEMETRY_HTTP_PERMANENT, 3);
  expect(!denied.pending, "tras 3 permanentes se detiene");
  expect(denied.abandoned, "el abandono evita un bucle infinito");

  expect(telemetry_backoff_ms(2000) == 4000, "el backoff transitorio se duplica");
  expect(telemetry_backoff_ms(30000) == 30000, "el backoff no supera 30 s");

  expect(queue_full_decision(false, false) == QUEUE_DROP_OLDEST, "dos normales: se reemplaza el más antiguo");
  expect(queue_full_decision(true, false) == QUEUE_DROP_OLDEST, "un SOS desplaza telemetría normal");
  expect(queue_full_decision(false, true) == QUEUE_KEEP_OLDEST, "una cola llena no borra el SOS por telemetría normal");
  expect(queue_full_decision(true, true) == QUEUE_KEEP_OLDEST, "un SOS nuevo no borra el SOS ya encolado");

  if (failures != 0) {
    fprintf(stderr, "%d afirmaciones fallaron\n", failures);
    return 1;
  }
  printf("telemetry_outbox_test ok\n");
  return 0;
}
