# EZ Launcher

A full-featured Android home screen replacement designed for elderly users. Large buttons, high contrast, and safety-first features — no clutter, no confusion.

---

## Features

### Home Screen
The home screen shows up to six large, colour-coded quick-action buttons. Each button can be individually shown or hidden from the **Settings** tab.

| Button | What it does |
|---|---|
| **Phone** | Opens the system dialler |
| **Text** | Opens the default messaging app |
| **Camera** | Launches the system camera |
| **Magnifier** | Opens the built-in magnifier (camera zoom) |
| **All Apps** | Shows every installed app |
| **Flashlight** | Toggles the rear torch on/off |

Long-pressing any button reads its label aloud via text-to-speech.

---

### Weather
A **weather card** at the top of the home screen shows the current temperature, conditions, and city name using the [Open-Meteo](https://open-meteo.com/) API (no account or API key required). Tapping the card opens a **7-day forecast screen** showing high/low temperatures, weather conditions, and precipitation chance for each day. Location permission is requested on first use; if denied, the card shows a tap-to-enable prompt.

---

### Voice Commands
Tap **"Say a Command"** (the indigo microphone button above SOS) and speak naturally. Microphone permission is requested on first use.

| Say | Result |
|---|---|
| "Call John" | Looks up John in contacts and dials |
| "Text Mary" | Opens SMS composer to Mary |
| "Open Camera" / "Take a photo" | Launches the camera |
| "Open YouTube" | Finds and launches YouTube (or any installed app by name) |
| "All apps" | Navigates to the full app list |
| "Flashlight on" / "Flashlight off" | Sets the torch to the requested state |
| "Flashlight" | Toggles the torch |
| "Go home" | Returns to the home screen |

Unrecognised phrases show a "not understood" card with example commands and a Retry button.

---

### SOS
The large red SOS button at the bottom of the home screen starts a **10-second countdown** before sending an emergency alert. During the countdown the user can tap **CANCEL** to abort.

When the countdown completes:
- Places a phone call to the primary emergency contact
- Sends an SMS to all configured emergency contacts
- Shares the device's current GPS location in the SMS (if granted)

Emergency contacts are configured in **Settings → Emergency Contacts**.

---

### Fall Detection
When enabled (Settings → Fall Detection), a foreground service monitors the accelerometer continuously in the background and survives device reboots.

When a fall is detected:
1. A full-screen alert appears (even on the lock screen)
2. A **30-second countdown** begins
3. Tapping **"I'm OK"** dismisses the alert
4. If the countdown reaches zero, the app automatically calls the primary emergency contact

Sensitivity is adjustable: **Low** (fewer false alerts), **Medium** (recommended), or **High** (most sensitive).

---

### Speed Dial
A dedicated tab shows saved contacts as large tap-to-call cards. Contacts are added from the device's address book and stored locally.

---

### Medication Reminders
Add medications with one or more daily reminder times. At each scheduled time a notification appears with **Take** and **Snooze** actions. Alarms are rescheduled automatically after a reboot.

---

### Magnifier
A full-screen camera preview with pinch-to-zoom. Useful for reading small print, labels, or menus.

---

### Clock
A large full-screen clock display, accessible from the bottom navigation bar.

---

### Settings
Accessible at any time from the **Settings** tab in the bottom navigation bar. Lets the user:
- Show or hide any of the six home screen quick-action buttons
- Enable / disable fall detection and adjust its sensitivity

---

## Navigation

The bottom navigation bar provides access to five top-level destinations:

| Tab | Description |
|---|---|
| **Home** | Quick-action buttons, weather, SOS, and voice commands |
| **Speed Dial** | One-tap calling for saved contacts |
| **Meds** | Medication reminder schedule |
| **Clock** | Full-screen clock |
| **Settings** | Home button customisation and fall detection |

---

## Permissions

| Permission | Why it's needed |
|---|---|
| `CAMERA` | Magnifier and flashlight |
| `RECORD_AUDIO` | Voice commands |
| `ACCESS_FINE_LOCATION` | GPS coordinates in SOS SMS |
| `ACCESS_COARSE_LOCATION` | Weather widget |
| `CALL_PHONE` | SOS and voice "Call" command |
| `SEND_SMS` | SOS alert messages |
| `READ_CONTACTS` | Speed Dial contact picker and voice "Call/Text" lookup |
| `POST_NOTIFICATIONS` | Medication reminders and fall detection alerts |
| `FOREGROUND_SERVICE_HEALTH` | Fall detection background service |
| `HIGH_SAMPLING_RATE_SENSORS` | Accelerometer access for fall detection |
| `RECEIVE_BOOT_COMPLETED` | Restore alarms and fall detection after reboot |

All dangerous permissions are requested at first launch with a plain-language explanation before the system dialog appears.

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Clean Architecture (domain / data / presentation layers)
- **DI:** Hilt
- **Persistence:** Room (medications, speed dial, emergency contacts) + DataStore (preferences)
- **Background work:** Android `AlarmManager` (medication reminders), foreground `Service` (fall detection)
- **Speech:** Android `SpeechRecognizer` (on-device, no third-party SDK)
- **Weather:** [Open-Meteo](https://open-meteo.com/) REST API — free, no API key
- **Location:** `FusedLocationProviderClient`
- **Torch:** Android `CameraManager.setTorchMode()` — direct system API, no camera binding required
