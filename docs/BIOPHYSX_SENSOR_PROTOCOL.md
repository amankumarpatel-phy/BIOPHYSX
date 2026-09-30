# BIOPHYSX BLE Sensor Protocol v1.4

## GATT identifiers

- Service UUID: `6f6e1200-0c7a-4a66-92e8-0b6e8f6a7001`
- Notify characteristic: `6f6e1201-0c7a-4a66-92e8-0b6e8f6a7001`

## Preferred payload

Send UTF-8 notification lines ending in `\n`:

```text
PPG=123456,PULSE=74.2,TEMP=33.8,RR=0,FINGER=1\n
```

Fields:

- `PPG` = raw optical IR/PPG sample in arbitrary sensor units
- `PULSE` = optional pulse-rate estimate in BPM
- `TEMP` = optional **contact/sensor temperature** in °C; not core body temperature
- `RR` = optional respiratory rate in breaths/min; use `0` when unavailable
- `FINGER` = `1` when the sensor believes a finger is present, otherwise `0`

The Android app tolerates missing optional fields and older v1.1 payloads. The `FINGER` field is used by the onboarding screen to decide whether optical contact has been detected.

## Sampling note

The reference ESP32 sketch streams one packet approximately every 50 ms (~20 Hz application stream). The optical sensor itself may sample internally at a different rate. This stream rate is intended for live visualization and pulse demonstration, not clinical waveform acquisition.

## Design rule

Never fabricate physiological values. Unsupported channels remain unavailable. BIOPHYSX is an educational/experimental measurement platform, not a diagnostic or emergency-care device.
