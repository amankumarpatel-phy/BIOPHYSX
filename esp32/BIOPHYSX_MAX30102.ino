/*
  BIOPHYSX real PPG prototype
  Hardware:
    - ESP32
    - MAX30102 / MAX3010x optical sensor
    - Optional DS18B20 contact temperature sensor

  Arduino libraries:
    - NimBLE-Arduino
    - SparkFun MAX3010x Pulse and Proximity Sensor Library
    - OneWire
    - DallasTemperature

  IMPORTANT:
    This is an educational/experimental prototype. Pulse rate is not a medical diagnosis.
    The temperature channel is skin/contact temperature, not core body temperature.
*/

#include <Wire.h>
#include <NimBLEDevice.h>
#include "MAX30105.h"
#include "heartRate.h"
#include <OneWire.h>
#include <DallasTemperature.h>

static const char* SERVICE_UUID = "6f6e1200-0c7a-4a66-92e8-0b6e8f6a7001";
static const char* DATA_UUID    = "6f6e1201-0c7a-4a66-92e8-0b6e8f6a7001";
static const int TEMP_PIN = 4; // Optional DS18B20 data pin

NimBLECharacteristic* dataChar = nullptr;
MAX30105 particleSensor;
OneWire oneWire(TEMP_PIN);
DallasTemperature tempSensor(&oneWire);

const byte RATE_SIZE = 8;
byte rates[RATE_SIZE];
byte rateSpot = 0;
long lastBeat = 0;
float beatsPerMinute = 0.0f;
int beatAvg = 0;

void setup() {
  Serial.begin(115200);
  Wire.begin();

  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) {
    Serial.println("MAX30102 not found. Check wiring and power.");
    while (true) delay(1000);
  }

  // Sampling-friendly configuration for a first prototype.
  particleSensor.setup();
  particleSensor.setPulseAmplitudeRed(0x24);
  particleSensor.setPulseAmplitudeIR(0x24);
  particleSensor.setPulseAmplitudeGreen(0);

  tempSensor.begin();
  tempSensor.setResolution(9);
  tempSensor.setWaitForConversion(false);

  NimBLEDevice::init("BIOPHYSX Sensor");
  auto* server = NimBLEDevice::createServer();
  auto* service = server->createService(SERVICE_UUID);
  dataChar = service->createCharacteristic(DATA_UUID, NIMBLE_PROPERTY::NOTIFY);
  service->start();

  auto* adv = NimBLEDevice::getAdvertising();
  adv->addServiceUUID(SERVICE_UUID);
  adv->start();

  Serial.println("BIOPHYSX MAX30102 sensor ready");
}

void loop() {
  long irValue = particleSensor.getIR();

  // Finger detection heuristic. The threshold must be calibrated for the sensor and enclosure.
  bool fingerPresent = irValue > 50000;

  if (fingerPresent && checkForBeat(irValue)) {
    long delta = millis() - lastBeat;
    lastBeat = millis();

    if (delta > 250 && delta < 2000) {
      beatsPerMinute = 60.0f / (delta / 1000.0f);

      if (beatsPerMinute > 40 && beatsPerMinute < 180) {
        rates[rateSpot++] = (byte)beatsPerMinute;
        rateSpot %= RATE_SIZE;

        int total = 0;
        for (byte i = 0; i < RATE_SIZE; i++) total += rates[i];
        beatAvg = total / RATE_SIZE;
      }
    }
  }

  static unsigned long lastTempRead = 0;
  static float cachedTempC = NAN;
  if (millis() - lastTempRead >= 1000) {
    lastTempRead = millis();
    tempSensor.requestTemperatures();
    float t = tempSensor.getTempCByIndex(0);
    if (t > -50 && t < 100) cachedTempC = t;
  }
  float tempC = cachedTempC;

  // Stream enough optical samples for a smooth Android waveform.
  char payload[112];
  if (isnan(tempC)) {
    snprintf(payload, sizeof(payload), "PPG=%ld,PULSE=%.1f,TEMP=0,RR=0,FINGER=%d\n", irValue, (beatAvg > 0 ? beatAvg : beatsPerMinute), fingerPresent ? 1 : 0);
  } else {
    snprintf(payload, sizeof(payload), "PPG=%ld,PULSE=%.1f,TEMP=%.1f,RR=0,FINGER=%d\n", irValue, (beatAvg > 0 ? beatAvg : beatsPerMinute), tempC, fingerPresent ? 1 : 0);
  }

  dataChar->setValue((uint8_t*)payload, strlen(payload));
  dataChar->notify();

  delay(50);
}
