/*
  BIOPHYSX ESP32 BLE demo sender.
  This is a connectivity prototype only: it sends synthetic values so the
  Android app can be tested before the physical sensors are connected.

  Library: NimBLE-Arduino
*/
#include <NimBLEDevice.h>

static const char* SERVICE_UUID = "6f6e1200-0c7a-4a66-92e8-0b6e8f6a7001";
static const char* DATA_UUID    = "6f6e1201-0c7a-4a66-92e8-0b6e8f6a7001";
NimBLECharacteristic* dataChar = nullptr;

void setup() {
  Serial.begin(115200);
  NimBLEDevice::init("BIOPHYSX Sensor");
  auto* server = NimBLEDevice::createServer();
  auto* service = server->createService(SERVICE_UUID);
  dataChar = service->createCharacteristic(DATA_UUID, NIMBLE_PROPERTY::NOTIFY);
  service->start();
  auto* adv = NimBLEDevice::getAdvertising();
  adv->addServiceUUID(SERVICE_UUID);
  adv->start();
  Serial.println("BIOPHYSX BLE demo ready");
}

void loop() {
  static float phase = 0;
  phase += 0.08f;
  float bpm = 74.0f + 2.5f * sin(phase * 0.35f);
  float temp = 36.5f + 0.08f * sin(phase * 0.20f);
  float rr = 15.0f + 1.0f * sin(phase * 0.18f);
  char payload[64];
  snprintf(payload, sizeof(payload), "PULSE=%.1f,TEMP=%.2f,RR=%.1f\n", bpm, temp, rr);
  dataChar->setValue((uint8_t*)payload, strlen(payload));
  dataChar->notify();
  delay(1000);
}
