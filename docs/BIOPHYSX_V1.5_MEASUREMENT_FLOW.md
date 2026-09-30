# BIOPHYSX v1.5 — Measurement Flow

## User experience
1. Connect the BIOPHYSX ESP32 sensor hub over BLE.
2. Place one finger gently on the MAX30102 optical sensor.
3. The app waits for a usable optical window and shows signal quality.
4. A 30-second guided capture runs with a 5-second stabilization phase.
5. The app presents a measurement summary with pulse, contact temperature (when available), signal score, duration and sample count.

## Signal-quality design
The app uses `PpgSignalEngine` only for capture guidance. It considers window amplitude, normalized variation and short-term stability. The resulting score is a waveform-quality indicator, not a health score.

## Pulse display
The app accepts valid pulse samples in the range 35–220 BPM and displays the median of the recent sample window to reduce flicker from individual beat detections.

## Safety / scope
This prototype is educational/experimental. It is not a medical device, does not diagnose disease, and does not claim clinical accuracy. Contact temperature is not core body temperature.
