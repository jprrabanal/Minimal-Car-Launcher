# Minimal Android Car Launcher (1080p + ZLink)

A lightweight, distraction-free, and high-contrast Android Home Launcher designed specifically for **1080p landscape built-in Android head units** with dedicated **ZLink (Android Auto / CarPlay)** integration, **customizable bottom dock**, **autostart app on boot**, and an **in-app GitHub updater**.

---

## Highlights & Features

- **Minimal Home Screen**: a large 12-hour clock with the date (for example *Friday, Aug. 21, 2026*) in the center, and one centered bottom bar with All Apps, **Android Auto**, your pinned apps and About.
  - Always a soft dark theme (no pure black or pure white). Pick from 6 colors in **About → Minimal Colors**: Slate, Midnight Blue, Graphite, Forest, Plum and Mocha.
  - Scales to common head-unit screens: 1024x600, 1280x720, 1280x800 and 2000x1200.
- **Android Auto Button**: a round icon in the center of the bar. Auto-detects your head unit's ZLink installation (`com.zjinnova.zlink`, `com.xyauto.zlink`, etc.) and launches Android Auto in one tap.
- **Hardware Back Button**: closes open windows. On the home screen it takes you back to the last app you opened from the launcher (for example Android Auto).
- **Night Dimming**: from 6 PM to 6 AM (you can change the times) the whole screen uses darker colors, so it does not glare at night. Turn it on or off in **About → Night Dimming**.
- **Head Unit Top Bar**: the status bar stays visible and matches the launcher color. Turn it off in **About → Show Top Bar**.
- **Customizable Bottom Dock**:
  - **Long-Press Any Icon**: Choose to **Remove** it or **Replace** it with another installed app.
  - Pinned apps are automatically saved across reboots and power cycles.
- **Autostart App on Boot**:
  - Choose any installed app (for example ZLink, music, or your dashcam) to open automatically when the car starts.
  - Open **About** on the dock, tap the **Autostart App** row to pick the app, and use the switch to turn it on or off.
  - The app opens a few seconds after the head unit boots, and only **once per boot**. It does not reopen when you press Home or when the launcher restarts during a drive.
  - Note: head units that only "sleep" when the car is turned off (instead of fully shutting down) will not reopen the app until the next full reboot.
- **All Apps Drawer with Pinning**:
  - Open All Apps and **long-press any app** to quickly pin it to the bottom bar.
  - 6-column grid with instant search filtering.
- **Direct In-App GitHub Updater**:
  - Tap the update icon on the bottom dock to fetch new releases directly from this GitHub repository.
  - Downloads the latest APK with real-time progress and triggers the Android system installer.
- **Zero Local Installs Needed**: Automated GitHub Actions CI workflow compiles a signed `app-release.apk` and publishes releases in the cloud.

---

## How to Build the APK via GitHub Actions (Cloud Build)

Whenever you push to `main`, GitHub Actions automatically compiles the APK and creates a new GitHub Release:

1. Go to your repository on GitHub: `https://github.com/jprrabanal/Minimal-Car-Launcher`
2. Click on **"Releases"** (on the right sidebar) or the **"Actions"** tab.
3. Download `app-release.apk` from the latest release.

### Release signing (one-time setup)

The release signing key is private and is **not** stored in this repository. Add these four secrets in **Settings → Secrets and variables → Actions → New repository secret**:

| Secret name | Value |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | The keystore file (`.p12`), encoded as Base64 |
| `SIGNING_STORE_PASSWORD` | The keystore password |
| `SIGNING_KEY_ALIAS` | `carlauncher` |
| `SIGNING_KEY_PASSWORD` | The key password (same as the keystore password) |

Keep a private backup of the keystore file and its password. If you lose them, installed copies of the app can no longer be updated.

Each release also contains `app-release.apk.sha256`. The in-app updater checks the downloaded APK against it and refuses to install a file that does not match.

---

## How to Install on Your Head Unit or Phone

1. **Copy to USB**:
   - Copy `app-release.apk` onto a USB flash drive (or download it directly on your phone).
2. **Install**:
   - Open your head unit's **File Manager** (or phone Files app) and tap `app-release.apk` to install.
3. **Set as Default Launcher (on Car)**:
   - Press the physical or on-screen **Home** button on your car unit.
   - Select **Car Launcher** and tap **"Always"**.
   - *(On a phone: tap "Just Once" to keep your standard phone launcher default).*
4. **Future Updates**:
   - Once installed, you can simply tap the **Update** icon on the bottom dock to fetch and install new versions directly!
