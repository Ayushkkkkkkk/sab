# PawCare

Android pet-care app written in **Java**. Accounts, pets, routines, checklists, SMS handoff, and gesture controls are stored locally in **Room (SQLite)** so data survives app restarts.

## What you need

| Tool | Version |
| --- | --- |
| JDK | **17** (Temurin / Oracle / Zulu all work) |
| Android Studio | **Hedgehog** or newer (Koala / Ladybug / Meerkat are fine) |
| Android SDK | Platform **34**, Build-Tools 34+ |
| Device | Phone or emulator, **API 26+** (Android 8.0) |

Assign **only one** desirable feature: **gestures** (no geotagging / extra external-app feature besides SMS, which the brief requires).

---

## macOS (your friend)

### 1. Install JDK 17

```bash
brew install --cask temurin@17
```

Confirm:

```bash
java -version
```

You should see `17.x`. If another Java is first in PATH, set:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

Add that line to `~/.zshrc` so it stays.

### 2. Install Android Studio

1. Download [Android Studio](https://developer.android.com/studio).
2. Open it once and finish the setup wizard so the **Android SDK** installs.
3. In **Settings → Languages & Frameworks → Android SDK**:
   - SDK Platforms: tick **Android 14.0 (API 34)**
   - SDK Tools: tick **Android SDK Build-Tools**, **Platform-Tools**, and an emulator system image if you want a virtual device.

Typical SDK path on Mac:

`~/Library/Android/sdk`

### 3. Open this project

1. Unzip / copy the `sab` folder.
2. Android Studio → **Open** → select the folder that contains `settings.gradle` (this repo root).
3. When prompted, **Trust Project**.
4. Let Gradle sync. First sync downloads Gradle 8.7 and dependencies (needs internet).

If sync asks for `local.properties`, Android Studio usually writes it. If not, create `local.properties` in the project root:

```properties
sdk.dir=/Users/YOUR_MAC_USERNAME/Library/Android/sdk
```

Do **not** commit that file.

### 4. Run in one go

1. Plug in a phone with **USB debugging**, or start an emulator (Device Manager → Create Device → Pixel → system image API 34).
2. Select the app module **app** and the device in the toolbar.
3. Press the green **Run** button (or `Control + R`).

The welcome screen should appear. Create an account, add a pet, add a routine (daily feeding works well), then open **Checklist** — today’s tasks generate automatically.

### Optional: command line on Mac

```bash
cd /path/to/sab
chmod +x gradlew
./gradlew assembleDebug
```

APK path: `app/build/outputs/apk/debug/app-debug.apk`

Install on a connected device:

```bash
./gradlew installDebug
```

---

## Windows

1. Install [Temurin 17](https://adoptium.net/) and [Android Studio](https://developer.android.com/studio).
2. Open this folder in Android Studio and wait for Gradle sync.
3. `local.properties` should contain something like:

```properties
sdk.dir=C:\\Users\\YOUR_NAME\\AppData\\Local\\Android\\Sdk
```

4. Run on an emulator or USB device.

Command line:

```bat
gradlew.bat assembleDebug
```

---

## Linux

```bash
sudo pacman -S jdk17-openjdk   # or your distro equivalent
```

Install Android Studio from the official site or your package manager. SDK is often:

`~/Android/Sdk`

```bash
chmod +x gradlew
./gradlew assembleDebug
```

Create `local.properties`:

```properties
sdk.dir=/home/YOUR_USER/Android/Sdk
```

---

## First-run demo (so every feature is visible)

1. **Sign up** with a real-looking email and password (6+ characters).
2. Home → **+** → add a pet (name + animal type required). Add a photo if you want.
3. **Routines → +** → e.g. Breakfast, category Feeding, Daily, time 8:00, turn on reminder.
4. Open **Checklist**. The routine becomes today’s task.
   - Swipe **right** to complete
   - Swipe **left** to delete (confirm dialog)
   - **Shake** the phone to reset today (confirm dialog)
5. **More → Delegate care by SMS** → pick the pet, enter a number, **Open SMS app**.
6. **More → Expenses / Appointments** for optional tracking and vaccination/vet reminders.

Log out from **More**. The same email logs you back in; pets and tasks are still there.

---

## Feature map

| Area | Where in the app |
| --- | --- |
| Welcome / sign up / log in / log out / validation / local accounts | Welcome, Sign up, Log in, More |
| Dashboard, pets, pending/done, FAB, bottom nav | Home |
| Add / edit / delete pet, photos, medical + vaccines | Pets + pet profile |
| Daily/weekly routines, categories, times, notes, supplies | Routines |
| Auto checklist, daily/weekly, complete, edit, delete, search/filter | Checklist |
| SMS with feeding, walking, meds, instructions | More → SMS |
| Swipe complete/delete, shake reset | Checklist |
| Room persistence | All of the above |
| Material 3, FAB, pickers, dialogs, snackbars | Throughout |
| Expenses, monthly total, vet/vaccination appointments, reminders | More |

Passwords are stored as **SHA-256 hashes**, not plain text. Photos are copied into app storage so they remain after restart.

Notifications: allow them when Android asks. Feeding/medication routines with the reminder switch, and appointments with “remind 1 hour before”, use the local notification channel. Exact-alarm timing can vary by OEM battery settings.

SMS uses the device messaging app (`smsto:` intent). It does not send silently in the background.

---

## Troubleshooting

**Gradle sync failed / SDK not found**  
Create `local.properties` with the correct `sdk.dir` (see Mac / Windows / Linux above).

**JDK version error**  
The project uses Java 17. In Android Studio: **Settings → Build → Gradle → Gradle JDK → 17**.

**`gradlew: Permission denied` on Mac**  
`chmod +x gradlew`

**App installs then crashes on first open**  
Uninstall any old build with the same package `com.sab.pawcare`, then Run again. If Room schema was from an older experiment, a reinstall is enough (`fallbackToDestructiveMigration` is enabled).

**No SMS app on emulator**  
Use a phone, or an emulator image that includes Messages. The preview still generates.

**Shake does nothing**  
Use a physical device; most emulators do not simulate a strong accelerometer shake unless you inject sensor data.
