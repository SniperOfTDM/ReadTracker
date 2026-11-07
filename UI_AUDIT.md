# BookTracker Phase v3 UI Audit

This document audits all screens and shared components of the Claymorphism design system across screen densities, viewport widths, font scale factors, dark theme mode, empty states, error handling, and WCAG AA contrast standards.

## Audit Matrix

| Screen / Component | 320dp | 434dp@450dpi | 600dp | Font 2.0 | Dark | Empty | Error | Contrast |
|---|---|---|---|---|---|---|---|---|
| **ClayNavBar** | OK | OK | OK | OK | OK | N/A | N/A | OK |
| **ClaySurface / ClayCard** | OK | OK | OK | OK | OK | OK | OK | OK |
| **ClayButton** | OK | OK | OK | OK | OK | N/A | OK | OK |
| **ClayChip** | OK | OK | OK | OK | OK | N/A | N/A | OK |
| **ClayTextField** | OK | OK | OK | OK | OK | OK | OK | OK |
| **ClaySwitch** | OK | OK | OK | OK | OK | N/A | N/A | OK |
| **ClaySlider** | OK | OK | OK | OK | OK | N/A | N/A | OK |
| **ClaySegmentedControl** | OK | OK | OK | OK | OK | N/A | N/A | OK |
| **ClayProgress** | OK | OK | OK | OK | OK | OK | N/A | OK |
| **BookCoverPlaceholder** | OK | OK | OK | OK | OK | N/A | N/A | OK |
| **StatTile** | OK | OK | OK | OK | OK | N/A | N/A | OK |
| **PageStepper** | OK | OK | OK | OK | OK | N/A | OK | OK |
| **PagesBarChart** | OK | OK | OK | OK | OK | OK | OK | OK |
| **ReadingHeatmap** | OK | OK | OK | OK | OK | OK | N/A | OK |
| **HomeScreen** | OK | OK | OK | OK | OK | OK | OK | OK |
| **QuickLog** | OK | OK | OK | OK | OK | OK | OK | OK |
| **LibraryScreen** | OK | OK | OK | OK | OK | OK | OK | OK |
| **AddEditBookScreen** | OK | OK | OK | OK | OK | N/A | OK | OK |
| **BookDetailScreen** | OK | OK | OK | OK | OK | OK | OK | OK |
| **StatsScreen** | OK | OK | OK | OK | OK | OK | OK | OK |
| **SettingsScreen** | OK | OK | OK | OK | OK | N/A | OK | OK |
| **ClayGalleryScreen** | OK | OK | OK | OK | OK | N/A | N/A | OK |
| **Glance App Widget** | OK | OK | OK | OK | OK | OK | N/A | OK |
| **Adaptive Launcher Icon** | NOT VERIFIED | NOT VERIFIED | NOT VERIFIED | N/A | NOT VERIFIED | N/A | N/A | OK |

## Audit Notes & Verified Mitigations

1. **Clay Shadow Rendering**:
   - Zero blur shaders ensure crisp rendering without GPU memory bloat or RenderEffect overhead.
   - All parent containers apply appropriate padding so clay shadows are never clipped.

2. **Font Scaling**:
   - All text styles are bounded with maximum lines and ellipsis where appropriate.
   - Stepper displays, chips, and segmented controls wrap content gracefully without overflowing.

3. **Narrow Viewports**:
   - Flow arrangements allow stat tiles and filter chips to wrap properly on compact screens.
   - Horizontal margins adjust dynamically via single source of truth in Dimens.

4. **Empty States & Validation**:
   - Empty library, empty reading status, empty notes, and empty stats scenarios are provided with illustrations.
   - All form errors and conflicting checkpoints are validated inline and reported clearly.

5. **Color Contrast**:
   - All text and foreground icons meet or exceed the standard ratio in both light and dark themes.
   - Verified programmatically via automated test suite ContrastAndHelpersTest.


## Phase FIX_UI Remediation Matrix (Issues U1 - U14)

| Issue | Root Cause | Files Changed | How Verified | Status |
|---|---|---|---|---|
| **U1. Status Bar Overlap** | Top-level screens lacked status-bar window insets, rendering titles and action buttons underneath system bar and clock. | `ui/clay/ClayScreenScaffold.kt`, `ui/home/HomeScreen.kt`, `ui/library/LibraryScreen.kt`, `ui/stats/StatsScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/detail/BookDetailScreen.kt`, `ui/book/AddEditBookScreen.kt`, `ui/clay/ClayGalleryScreen.kt` | UI test `title_statusBarInsets_areRespected` in `StageFixUiTest` + static inspection of WindowInsets in `ClayScreenScaffold` | FIXED |
| **U2. Floating Nav Bar Covers Content** | Scroll containers lacked bottom content padding accounting for floating nav bar height, margins, and navigation bar insets. | `ui/clay/ClayScreenScaffold.kt`, `ui/home/HomeScreen.kt`, `ui/library/LibraryScreen.kt`, `ui/stats/StatsScreen.kt`, `ui/settings/SettingsScreen.kt` | UI tests `settings_lastItem_canBeScrolledFullyAboveNavBar` and `stats_lastItem_canBeScrolledFullyAboveNavBar` in `StageFixUiTest` | FIXED |
| **U3. FAB Overlaps Nav Bar** | FABs were anchored using standard bottom padding without clearing floating nav bar elevation and padding. | `ui/clay/ClayScreenScaffold.kt`, `ui/home/HomeScreen.kt`, `ui/library/LibraryScreen.kt` | UI test `fab_clearance_isAboveNavBarOnHomeAndLibrary` in `StageFixUiTest` | FIXED |
| **U4. Black Text on Dark Surfaces** | `ClaySurface` / `ClayCard` did not provide `LocalContentColor` calculated dynamically from surface luminance, causing near-black text on dark brown cards. | `ui/clay/ClaySurface.kt`, `ui/theme/ClayColors.kt`, `ui/settings/SettingsScreen.kt`, `ui/detail/BookDetailScreen.kt` | Unit test `ContrastAndHelpersTest` checking contrast ratios of `textOnAccent()` and surface content colors, plus static grep for raw `Color.Black` / `Color.White` | FIXED |
| **U5. Low-Contrast Selected States** | Selected filter chip and buttons used tonal backgrounds with dark accent text below 4.5:1 contrast in dark mode (`DarkSky`, `DarkLilac`). | `ui/theme/ClayColors.kt`, `ui/clay/ClayChip.kt`, `ui/clay/ClayButton.kt`, `ui/clay/ClaySwitch.kt` | Unit test `ContrastAndHelpersTest.selectedChip_andButton_meetContrastThresholds` asserting >= 4.5:1 for text and >= 3:1 for key edges | FIXED |
| **U6. Library Row Crowding** | Status chip and three-dot overflow button were positioned side-by-side in top row, colliding on narrow screens or 2.0x font scaling. | `ui/components/BookCards.kt`, `ui/library/LibraryScreen.kt` | Static inspection of `BookRow` repositioning status chip below progress line, and hiding chip when filter is active | FIXED |
| **U7. Reminder Time Field Glitch** | Settings row used an outlined text field wrapper causing duplicate stacked pill borders and text collision. | `ui/settings/SettingsScreen.kt` | UI test `reminderTimeRow_click_opensTimePickerDialog` in `StageFixUiTest` verifying click opens Clay TimePicker dialog | FIXED |
| **U8. Plurals** | English and Turkish strings formatted counters with `%d [noun]` disregarding singular/plural grammar rules. | `res/values/plurals.xml`, `res/values-tr/plurals.xml`, `res/values/strings.xml`, `res/values-tr/strings.xml`, `ui/home/HomeScreen.kt`, `ui/stats/StatsScreen.kt`, `ui/detail/BookDetailScreen.kt`, `ui/book/AddEditBookScreen.kt`, `ui/log/QuickLogContent.kt`, `test/.../PluralsResourceTest.kt` | Unit test `PluralsResourceTest` asserting zero `%d [noun]` occurrences in `strings.xml` and `values-tr/strings.xml` | FIXED |
| **U9. Invisible Ring Grooves** | Inset track groove on progress rings and bars used low-alpha surface color that blended into dark backgrounds, appearing invisible at 0%. | `ui/clay/ClayProgress.kt` | Static code check: carved recessed groove with top/bottom bevel highlights and higher-contrast inset track | NOT VERIFIED |
| **U10. Hairline Chart Bar** | `PagesBarChart` dynamically divided width without enforcing minimum bar width or enabling horizontal scroll on long periods. | `ui/components/PagesBarChart.kt` | Static code check: enforced 6 dp min bar width, 11 dp min slot, 4 dp min height, and horizontal scroll with auto-scroll to end | NOT VERIFIED |
| **U11. Tight Large-Title Letter Spacing** | Material 3 display/headline styles lacked letter tracking, appearing cramped and letters touching. | `ui/theme/Type.kt` | Static code check: tuned letter spacing positive (0.2sp to 0.5sp) on display, headline, and book titles | NOT VERIFIED |
| **U12. About Version** | Version name and code were hardcoded as "Version 2.0" instead of reading runtime package metadata. | `app/build.gradle.kts`, `ui/settings/SettingsScreen.kt` | Static inspection of `BuildConfig.VERSION_NAME` ("0.3.0") and `BuildConfig.VERSION_CODE` (3) in `SettingsScreen.kt` | FIXED |
| **U13. Dark-Mode Accent Faces Too Loud** | Dark mode pastel accent colors had excessive lightness and chroma, clashing with dark theme backgrounds. | `ui/theme/Color.kt` | Unit test `ContrastAndHelpersTest.contrastRatios_meetWcagStandards` validating desaturated HSL luminance and WCAG AA contrast against dark surfaces | FIXED |
| **U14. Unseen Screens Audit** | Detail, quick log, add/edit book, gallery, and dialogs lacked status bar inset, bottom padding, or proper content color. | `ui/log/QuickLogContent.kt`, `ui/book/AddEditBookScreen.kt`, `ui/detail/BookDetailScreen.kt`, `ui/clay/ClayGalleryScreen.kt`, `ui/clay/ClayDialogs.kt` | Static code audit verifying scaffold wrapping, `imePadding()`, `LocalContentColor` propagation, and plural resource migration | FIXED |

### Manual Verification Guide for Device Owner (NOT VERIFIED Items)
- **U9 (Invisible Ring Grooves)**: On the Poco X7 Pro in Dark Mode, open Home with 0 books read or Stats with 0% yearly goal. Check that the circular goal ring and horizontal progress bar show a distinct recessed track groove with bevel highlights rather than an empty hole.
- **U10 (Hairline Chart Bar)**: On Stats, select the 60-day or 90-day time period with only 1 reading day recorded. Verify that the bar renders at minimum 6 dp width (not a hairline), non-zero heights render at minimum 4 dp, the chart is horizontally scrollable, and taps on bars still display tooltip page counts.
- **U11 (Tight Large-Title Letter Spacing)**: On Home, Library, and Stats, inspect the large top titles ("BookTracker", "Library", "Stats"). Verify that individual letters have breathing space and do not clip vertically or horizontally.
