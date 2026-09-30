# BIOPHYSX
### Your Body. Your Signals. Your Physics.

BIOPHYSX is a light, age-inclusive Android prototype for exploring body-connected sensors through a Physics-first interface.

## Included in this prototype

- Light floating/abstract UI
- Large accessible typography and controls
- Simple / Science mode switch
- Pulse, temperature, breathing and force dashboard cards
- Animated live waveform (demo mode)
- BLE sensor discovery architecture
- Custom BLE service + notify characteristic protocol
- Educational "See the Physics" explanation
- ESP32 synthetic BLE demo sender
- Safety language: experimental/educational, not diagnosis
- Footer branding: **Made with ❤️ by Aman Kumar Patel**

## First hardware target

ESP32 + optical pulse sensor + temperature sensor + IMU. Add ECG/EMG only with appropriate biomedical-grade front-end design, isolation, electrodes, and safety review.

## Android build

Open this folder in Android Studio and build `app` -> `assembleDebug`.

A GitHub Actions workflow is included under `.github/workflows/android.yml` so the project can build a debug APK in GitHub without installing the Android toolchain locally.

## BLE protocol

See `docs/BIOPHYSX_SENSOR_PROTOCOL.md`.

## v1.1 real sensor prototype

### Hardware

- ESP32 development board
- MAX30102 optical pulse/PPG module
- Optional DS18B20 contact temperature sensor
- Android phone with BLE

### Wiring

**MAX30102 → ESP32** (typical ESP32 DevKit)

- VIN → 3.3V
- GND → GND
- SDA → GPIO 21
- SCL → GPIO 22

**DS18B20 (optional)**

- VDD → 3.3V
- GND → GND
- DATA → GPIO 4
- 4.7 kΩ pull-up between DATA and 3.3V

Open `esp32/BIOPHYSX_MAX30102.ino` and install the listed Arduino libraries. Then flash the ESP32 and open the Android app.

### Safety

BIOPHYSX is an educational/experimental biosignal platform. It must not be presented as a medical diagnostic device or used to make medical decisions. Skin/contact temperature is not equivalent to clinically measured core body temperature.

### GitHub route

1. Create a GitHub repository named `BIOPHYSX`.
2. Upload the contents of this folder to the repository root.
3. Push to `main`.
4. Open **Actions → Build BIOPHYSX APK**.
5. Download the generated `BIOPHYSX-debug-apk` artifact.

The APK artifact is for development/testing. For public distribution, create a signed release build and review Android permissions, hardware enclosure, electrical safety, and measurement validation first.

## v1.2 guided measurement

- Dedicated 30-second measurement flow with live progress.
- Finger-placement guidance and simple optical-signal quality feedback.
- Local measurement history stored on-device.
- Reference ESP32 stream includes `FINGER=0/1` and sends approximately every 50 ms.

The app does not turn these signals into a medical diagnosis. Treat the measurements as experimental/educational.


## v1.3 Body Lab

The Body Lab combines external BLE sensor data with the phone's own accelerometer:
- PPG / pulse from MAX30102-class sensor via ESP32 BLE
- Contact temperature from supported external sensor
- Respiratory rate when supplied by the sensor hub
- Motion level from the Android accelerometer

All values are presented as experimental/educational measurements, not medical diagnoses.

## v1.4 Sensor Setup

- Added a guided BLE sensor onboarding screen with large, age-friendly steps.
- Added explicit finger-contact feedback using the `FINGER` BLE field.
- Added UI-only optical signal condition feedback: Place finger / Stabilizing / Weak signal / Usable signal / Strong signal.
- Added readiness gating before the guided 30-second measurement starts.
- Added Science mode on the setup screen for signal metrics.
