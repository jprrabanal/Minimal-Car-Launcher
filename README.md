# Minimal Android Car Launcher (1080p + ZLink)

A lightweight, distraction-free, and high-contrast Android Home Launcher designed specifically for **1080p landscape built-in Android head units** with dedicated **ZLink (Android Auto / CarPlay)** integration, **customizable bottom dock**, **autostart app on boot**, and an **in-app GitHub updater**.

---

## Highlights & Features

- **Minimal Home Screen**: a large 12-hour clock with the date (for example *Friday, Aug. 21, 2026*) in the center, and one centered bottom bar with All Apps, **Android Auto**, your pinned apps and About.
  - Always a soft dark theme (no pure black or pure white). Pick from 6 colors in **About → Minimal Colors**: Slate, Midnight Blue, Graphite, Forest, Plum and Mocha.
  - Scales to common head-unit screens: 1024x600, 1280x720, 1280x800 and 2000x1200.
- **Dashboard Home Screen** (optional): a second style with more features. Choose it in **About → Home Screen Style**. The Minimal style stays the default and does not change.
  - **Clock and Weather**: the time, the date, and the current weather (for example *31°C · Rain · Quezon City*). Weather comes from the free Open-Meteo service. It needs internet, such as your phone's hotspot.
  - **GPS Speed**: your speed in km/h from the head unit's GPS (Global Positioning System).
  - **Music Controls**: song name, artist and album picture, plus Previous, Play/Pause and Next. Works with Spotify, YouTube Music, radio apps and more. Tap the song to open the music app.
  - **Quick Buttons**: Brightness (Low, Medium, High), Volume, Bluetooth settings and Wi-Fi settings.
  - **Bigger Bottom Bar**: choose the button size in **About → Home Screen Style**: Normal, Large (default) or Extra Large.
  - **Permissions** (asked only when you use the Dashboard):
    - *Location*: for the speed and the weather.
    - *Notification Access*: for the music controls. Tap the Music card, then turn on **Car Launcher**. The launcher does not read or show your notifications.
    - *Modify system settings*: for the Brightness button. The first tap opens the screen where you allow it.
  - GPS and music updates run only while the Dashboard is on screen, to save power.
- **Android Auto Button**: a round icon in the center of the bar. Auto-detects your head unit's ZLink installation (`com.zjinnova.zlink`, `com.xyauto.zlink`, etc.) and launches Android Auto in one tap.
- **Hardware Back Button**: closes open windows. On the home screen it takes you back to the last app you opened from the launcher (for example Android Auto).
- **Night Dimming**: darker colors at night, so the screen does not glare. Choose in **About → Night Dimming**:
  - **Off**: always normal colors.
  - **Auto**: follows the head unit's own night mode (on many head units this turns on with the headlights).
  - **Schedule** (default): darker between two times you choose, 6:00 PM to 6:00 AM at first.
  - **Always**: always darker.
  - **Dimming Strength**: how much darker it gets: Light, Medium (default), Strong or Maximum. Text stays readable at every level.
  - **Dim All Apps**: also darkens Android Auto, maps and every other app, not only the launcher. It uses the same mode, schedule and strength. Turn it on in **About → Night Dimming → Dim All Apps**, then allow **Display over other apps**.
    - The dark layer never blocks taps.
    - It runs with a small "Screen dimmer is on" notification, so the head unit is less likely to close it. If the head unit closes it anyway, it comes back the next time the home screen opens.
    - Strength is limited to 80% darkness, because Android 12 and newer block taps through darker layers.
    - Turn off other dimmer apps (like Night Screen) so the screen is not dimmed twice.
    - **Cover Top Bar and Navigation Bar**: Android always keeps the top bar and the navigation bar above normal app layers. To darken them too, tap this row in **About → Night Dimming** and turn on **Car Launcher Screen Dimmer** in Accessibility settings. It only draws the dark layer; it does not read the screen or your apps. It also stays running better, so the "Display over other apps" permission and the notification are not needed while it is on.
      - On Android 13 and newer, sideloaded apps may show "Restricted setting". Open **Settings → Apps → Car Launcher**, tap the **⋮** menu, choose **Allow restricted settings**, then try again.
- **Android Settings Shortcuts**: many head units hide the normal Android Settings app and show only their own car settings. **About → Android Settings** opens it anyway: **All Settings**, **Apps**, **Accessibility** or **Display**.
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
