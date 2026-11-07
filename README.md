# ReadTracker 📚

> **Offline-first reading tracker for Android.**  
> Log checkpoints, track your daily pace, calculate accurate finish date estimates, and visualize reading habits with a soft, tactile Claymorphism UI.

---

## Overview

**ReadTracker** is a single-user, offline-first Android application designed to track real-life reading progress across multiple books in parallel. Rather than manually logging arbitrary durations, users record real page checkpoints ("I am now on page 142"). The app dynamically calculates velocity, rolling averages, streaks, and estimated completion dates on the fly.

Everything lives locally on device in **Room** and **DataStore**. There are no accounts, no cloud servers, no tracking telemetry, and no network permissions required.

---

## Key Features

- **Tactile Claymorphism UI**: Custom hand-crafted 3D clay components (`ClaySurface`, `ClayCard`, `ClayButton`, `ClayProgress`, `ClayNavBar`) using zero-blur canvas shaders for instant 60fps rendering without GPU memory overhead.
- **Dynamic Stats Calculation**: Real-time pace metrics calculated from raw checkpoints:
  - Daily average & rolling last-7-days average
  - Estimated completion date per book and total library ETA
  - Streak tracking (current & longest) and personal best day
  - Yearly reading goal progress tracking
- **Reading Visualization**:
  - Interactive Reading Heatmap showing activity density
  - 14-day page bar charts with goal reference lines
  - Tactile page steppers with direct input
- **Session Timer & Notes**:
  - Background-resilient reading session timer backed by DataStore
  - Rich reading notes and quote tracking per book
- **Home Screen Widget**:
  - Glance-based Android home screen widget displaying currently reading book and quick page logging shortcuts
- **Offline Backup & Restore**:
  - Export and import full library data, sessions, and notes via clean, schema-versioned JSON.
- **Accessibility & Density Ready**:
  - Full dark theme support, WCAG AA compliant contrast ratios, and responsive layouts across compact phones to foldables.

---

## Architecture & Tech Stack

The project follows modern Android architecture patterns with single-directional data flow (UDF) and clean MVVM separation:

```
app/
├── data/
│   ├── backup/         # Offline JSON backup and restore repository
│   ├── local/          # Room database, entities (Book, Session, Note), converters, DAOs
│   ├── model/          # Core domain data models & enums
│   └── repository/     # Repository implementations (Book, Settings, Timer)
├── domain/             # Pure Kotlin calculation engines (StatsCalculator, SessionValidator)
├── ui/
│   ├── book/           # Add/Edit book workflows
│   ├── clay/           # Reusable Claymorphism design primitives & custom modifiers
│   ├── components/     # High-level reading UI components (Heatmap, Charts, Steppers)
│   ├── detail/         # Book detail, session history, note dialogs
│   ├── home/           # Dashboard with active books & quick log actions
│   ├── library/        # Filterable book catalog
│   ├── log/            # Quick-log bottom sheet & standalone activity
│   ├── navigation/     # Jetpack Navigation Compose graph & routes
│   ├── settings/       # App preferences, yearly goals, backup management
│   ├── stats/          # Deep-dive reading analytics
│   ├── theme/          # Clay color tokens, typography, shapes, and dimensions
│   └── widget/         # Glance App Widget receiver & layout
└── worker/             # WorkManager daily reminder workers & notification scheduler
```

- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose, Material 3
- **Local Storage**: Room 2.6 (SQLite), Jetpack DataStore Preferences
- **Asynchrony**: Kotlin Coroutines & StateFlow
- **Widgets**: AndroidX Glance
- **Background Tasks**: AndroidX WorkManager
- **Build System**: Gradle Version Catalogs (`libs.versions.toml`)

---

## Building from Source

### Prerequisites
- Android Studio Ladybug | 2024.2.1 or newer
- JDK 17+
- Android SDK 35 (minSdk 26)

### Clone & Build
```bash
git clone https://github.com/SniperOfTDM/ReadTracker.git
cd ReadTracker
./gradlew assembleDebug
```

### Running Tests
```bash
# Unit tests (Stats calculations, validators, backup)
./gradlew testDebugUnitTest

# Instrumentation tests (Room DAOs, migrations, UI validation)
./gradlew connectedDebugAndroidTest
```

---

## License

This project is licensed under the terms of the MIT License.
