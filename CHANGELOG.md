## 1.5
- Added guided pulse-measurement phases: connect, place finger, stabilize, capture, complete.
- Added non-clinical optical signal-quality engine to guide capture quality.
- Added median pulse display from recent valid pulse samples.
- Added dedicated Measurement Result summary screen.
- History now stores signal score, captured sample count and session duration.
- Clarified that signal score and contact temperature are not health/clinical assessments.

## v1.4 — Sensor Setup & Optical Signal Check
- Added a guided BLE sensor onboarding flow.
- Added explicit finger-contact feedback from the `FINGER` protocol field.
- Added a non-clinical PPG signal-condition engine for UI guidance.
- Added Simple/Science mode to the setup experience.
- Added readiness gating before starting the guided measurement.


## v1.3 — Body Lab
- Added a dedicated multi-signal Body Lab screen with a floating abstract body visualization.
- Added live PPG, contact-temperature and respiratory-rate cards from the BLE sensor stream.
- Added phone accelerometer motion analysis with a simple Still/Gentle/Active indicator.
- Added Science mode messaging and a Physics explainer for PPG and motion.
- Added Body Lab entry point from the main dashboard.
- Retained age-friendly light UI and the “Made with ❤️ by Aman Kumar Patel” footer.
# Changelog

## 1.2 - Guided Measurement Experience

- Added a dedicated 30-second guided Pulse/PPG measurement screen.
- Added live optical signal quality messaging.
- Added measurement progress and completion state.
- Added local session history for the most recent measurements.
- Added a clear separation between contact temperature and clinical body temperature.
- Added `FINGER` field to the reference ESP32 BLE payload.
- Increased demo sensor stream cadence to approximately 20 Hz for smoother live visualization.
- Kept light floating UI, accessibility-oriented controls, Simple/Science concept, and creator footer.
