# ReadTracker 📚

<div align="center">

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-SDK%2026%E2%80%9335-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-Claymorphism-4285F4.svg?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Room%20DB-v3%20Migration-FF6F00.svg?style=for-the-badge&logo=sqlite&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)

**Tactile, privacy-respecting, offline-first reading companion for Android.**

*Log real checkpoints, estimate finish dates dynamically, and store custom book covers inside a bespoke 3D Claymorphism design.*

</div>

---

## 🌟 Why ReadTracker?

Most book trackers ask you to start a timer or guess arbitrary reading sessions. **ReadTracker** works how you actually read: you open your book, read, and log your checkpoint: *"I am on page 142."*

From that single number, ReadTracker dynamically derives:
- **Velocity & Consistency**: Daily averages, rolling 7-day velocity, and active streaks.
- **Finish Date Predictions**: Dynamic ETA per book and across your entire active library.
- **Visual Analytics**: Interactive GitHub-style reading heatmap and 14-day pace bars.
- **Local Privacy**: No telemetry, no accounts, no cloud database. Your data never leaves your device.

---

## 🎨 Tactile Claymorphism Design

ReadTracker features a custom, zero-dependency Claymorphism design system built entirely with Jetpack Compose canvas drawing:
- **Dual-tone soft shadows**: Inner and outer bevels simulate tactile, physical clay surfaces.
- **60 FPS Hardware Acceleration**: Zero blur passes and minimal GPU overhead.
- **Custom Primitives**: `ClaySurface`, `ClayCard`, `ClayButton`, `ClayDialog`, `ClayNavBar`, and `ClayProgress`.
- **Dynamic Book Covers**: Clay-framed covers with real spine accents, soft edge highlights, and gallery cover import.

---

## 📦 Download Flavors (Offline vs. Online)

ReadTracker is built from a single codebase into two specialized product flavors:

| Flavor | Internet Permission | Capabilities | Privacy Guarantee |
| :--- | :--- | :--- | :--- |
| **ReadTrackerOffline** | **None** (`android.permission.INTERNET` removed) | 100% offline. Manual book entry, checkpoint logging, local photo gallery cover picker, offline JSON backup. | Absolute zero network access enforced at the Android manifest level. |
| **ReadTrackerWithInt** | **Scoped** (`openlibrary.org` only) | All offline capabilities plus optional Open Library title/author/ISBN search and automatic cover downloading. | Only query text or scanned ISBN is sent to Open Library. No user data, notes, or statistics ever leave the phone. |

### Seamless In-Place Switching
Both flavors share the same `applicationId` (`com.sniperoftdm.booktracker`), Room database schema (`v3`), and signing key. You can install one build over the other at any time to upgrade or downgrade without losing your library data.

### Independent Permission Verification
Verify the offline APK independently before installing:
```bash
# Verify via Android Asset Packaging Tool
aapt2 dump permissions ReadTrackerOffline-<version>.apk
```
Confirm that `android.permission.INTERNET` does not appear anywhere in the manifest output.

---

## 🏗️ Architecture & Clean Domain Separation

```
app/
├── data/
│   ├── backup/         # Version-aware JSON backup repository (v1 & v2 support)
│   ├── local/          # Room database (v3), CoverStorage (downscale & cache), DAOs
│   ├── model/          # Pure domain models (Book, Session, Note, BookCandidate)
│   └── repository/     # Data repositories with reactive StateFlow streams
├── domain/             # Pure Kotlin algorithmic calculation engines (StatsCalculator)
├── flavor/             # Compile-time flavor dependencies (NoOp vs. Online repository)
├── ui/
│   ├── book/           # Add/Edit book workflows with Photo Picker integration
│   ├── clay/           # Bespoke Claymorphism components, dialogs, and shaders
│   ├── components/     # High-level reading components (Heatmap, Covers, Steppers)
│   ├── detail/         # Book detail inspection, reading logs, and quote collection
│   ├── home/           # Active shelf dashboard with quick log triggers
│   ├── library/        # Searchable, filterable library catalog
│   ├── settings/       # App preferences, yearly reading target, and JSON backup
│   └── stats/          # Reading velocity and pace visualization
└── widget/             # AndroidX Glance interactive home screen widget
```

---

## 🚀 Building & Testing

### Prerequisites
- **Android Studio** Ladybug (2024.2.1+)
- **JDK 17**
- **Android SDK 35** (Minimum SDK 26)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/SniperOfTDM/ReadTracker.git
cd ReadTracker

# Build offline debug APK
./gradlew assembleOfflineDebug

# Build online debug APK
./gradlew assembleOnlineDebug

# Run automated offline security permission verification
./gradlew verifyOfflineHasNoInternet
```

### Running Test Suites
```bash
# Run unit tests across both flavors
./gradlew testOfflineDebugUnitTest testOnlineDebugUnitTest

# Run instrumented tests on an emulator or connected device
./gradlew connectedOfflineDebugAndroidTest
```

---

## 📄 License

This project is open source and available under the terms of the [MIT License](LICENSE).
