# PlantID Android — Sprint 6 Test Report and Known Issues

Date: 2026-11-20 · Branch: `feature/hardening` · Identifier: **demo mock only**

This report states only what was actually run. "Not run" means not run.

## 1. Results

| Check | How | Result |
|---|---|---|
| Debug build | `./gradlew assembleDebug` | Passes, no compiler warnings |
| Release build | `./gradlew assembleRelease` (R8 minify + resource shrink) | Builds (unsigned, about 2.1 MB). **Not installed or run**: there is no signing config yet |
| Unit tests (JVM) | `./gradlew testDebugUnitTest` | **70 tests, 0 failures** (11 classes) |
| Device tests | `./gradlew connectedDebugAndroidTest` | **74 tests compile. Not run in this session** (no device attached). Run and record below |
| Lint | `./gradlew lintDebug` | 0 errors, 6 warnings, all intentional version pins (see K2) |
| Colour contrast | `ColorContrastTest` (WCAG 2.1) | 16 pairs × 2 themes pass. Lowest text pair 5.26:1 (needs 4.5:1) |
| Architecture boundary | `ArchitectureTest` | Passes. Confirmed it fails if a UI file references the mock |
| Manual testing, Sprints 0–5 | Team member on their phone | Reported on 2026-11-20 that all sprint features work |
| Manual testing, Sprint 6 | `MANUAL_TESTS.md` Sprint 6 section | Not run yet |

Device test results (fill in):

| Date | Device / Android | Command | Passed / total | Notes |
|---|---|---|---|---|
| | | `connectedDebugAndroidTest` | / 74 | |

## 2. Sprint 6 checks against the spec

| Spec task | Covered by |
|---|---|
| Small and large screens, larger fonts | `LayoutStressTest`: every main screen at 320 × 568 dp, font 200 %, primary actions reachable. Manual 6.1–6.3 |
| Permission denial and cancellation | No runtime permissions exist (D7, D12), so there is nothing to deny. Cancelling picker and camera is covered by `PhotoSelectionViewModelTest` and manual 2.5/2.7 |
| Corrupt or unsupported images | `CachePhotoStoreTest` (corrupt, missing, empty capture). Manual 2.14 |
| Rapid repeated taps | ViewModel guards (identify, retry, save, delete, photo) + **fix F1** with device test |
| Leaving and returning, process recreation | Capture survives process death (Sprint 2), **fix F2**: results survive process death. Manual 6.4–6.6 |
| Error messages and empty states | Reviewed: plain language, no technical details, a recovery action on each |
| Accessibility labels and contrast | `ColorContrastTest`, label and 48 dp touch-target check in `LayoutStressTest`. Manual TalkBack 6.7 |
| Remove unused dependencies and template artifacts | Template example tests removed, unused icon removed, lint cleanups (**F3**) |
| Mock clearly separated from production | Mock only wired in `AppContainer`, every mock result labelled, `ArchitectureTest`, `AI_INTEGRATION.md` |

## 3. Fixes made in Sprint 6

| # | Problem | Fix |
|---|---|---|
| F1 | Double-tapping Back during the exit animation popped **two** screens (care guide → result → preview); double taps could also navigate twice | Navigation callbacks run only while their screen is RESUMED. Device test `doubleTappingBackOnlyLeavesOneScreen` |
| F2 | After Android stopped the app in the background, the result screen **re-ran identification**: the demo could show a different result, and a new event id let the same photo be saved twice | Finished outcome + event id + time kept in `SavedStateHandle` and restored as-is. 4 unit tests |
| F3 | Lint: redundant activity label, obsolete `mipmap-anydpi-v26`, `Uri.parse` instead of `toUri()`. Template tests | Fixed / removed |

## 4. Known issues

| # | Issue | Impact | Plan |
|---|---|---|---|
| K1 | ~~Identification is the demo mock~~ | Resolved in Sprint 7: real on-device model | — |
| K2 | Kotlin compiler pinned at 2.2.10, so several libraries are pinned too: Coil 3.3.0, kotlinx-serialization-json 1.9.0, Compose BOM 2026.02.01. KSP must be 2.3.x | Missing newer library fixes; lint warns | Team decision on a Kotlin upgrade (would lift all pins) |
| K3 | ~~Release build never run on a device; no signing config~~ | Resolved in Sprint 8: signed release installed and checked on a phone (§7) | — |
| K4 | Landscape and tablets not optimised (not required for v1) | Works and scrolls, but photos are large in landscape | Only if the team needs it |
| K5 | The Demo mode picker choice resets to *Plant identified* after process death | Demo only; results already shown are preserved (F2) | Goes away with the real model |
| K6 | A full device storage during photo import shows "Can't open this photo" | Misleading message in a rare case | Optional: distinguish storage errors |
| K7 | Light needs shown in lux | Less plain language | Q12 (research lead) |
| K8 | Care data is a sample dataset; no plant descriptions | Content quality | Q13, Q14 (research lead) |
| K9 | Device tests clear the debug app's saved history | Tester data loss on the debug build | Documented; run tests before manual checks |
| K10 | Accessibility automation is basic (labels, touch targets, token contrast), not a full Accessibility Test Framework audit (its artifact conflicts with Espresso's dependencies) | Some issues only found manually | Manual TalkBack pass (6.7) |
| K11 | App icon is the Android Studio template icon | Branding | Q9 |

## 5. AI integration boundary

Ready for review: [`AI_INTEGRATION.md`](AI_INTEGRATION.md) describes the
contract, the output → plant mapping, what the AI lead must deliver, and where
the model plugs in.

## 6. Sprint 7 — model integration

Date: 2026-11-23 · Branch: `feature/model-integration` · Identifier: **PlantID v1 model**

| Check | Result |
|---|---|
| Debug build | Passes, no compiler warnings |
| Unit tests | **85 tests, 0 failures**, incl. the decision rule, metadata/labels consistency, the catalog from the real `model/` files, and confidence formatting |
| Device tests | Team member ran them on their phone (reported 2026-11-23: "it runs well"). Pass counts and timings not yet recorded below |
| Real-photo check | Same report: the model classified some of the tester's own plants, "not as accurate" as on the test set. Expected with different cameras, lighting and backgrounds; record per-photo results with `MANUAL_TESTS.md` 7.2–7.7 |
| Inference time | Measured 2026-11-24 on one phone (table below). Slower than expected and variable; see K18 |

| Date | Device / Android | First identification (incl. model load) | Warm identifications |
|---|---|---|---|
| 2026-11-24 | 2412DPC0AG / Android 16 | 8763 ms | 5462, 8392, 2624 ms |

Each time includes decoding and preprocessing a synthetic photo (3000×4000,
4000×3000, 300×300), so it isn't the model run alone.

New known issues:

| # | Issue | Impact | Plan |
|---|---|---|---|
| K12 | Debug APK about 33 MB (LiteRT native code for 4 CPU types) | Larger debug installs | Release as an App Bundle (Sprint 8) |
| K13 | ~~24 of 30 species had no care data~~ | Resolved: `model/care_tips.json` covers all 30 | — |
| K14 | Saved *not sure* results don't keep the possible-match guesses (history schema unchanged) | Saved record shows the warning only | Optional schema v2 with a migration |
| K15 | ~~Identification timeout still the provisional 30 s~~ | Resolved: confirmed from measured timings (D48) | — |
| K16 | Confidence is calibrated on the ML team's validation photos; on users' own photos it can be over-confident | A high % can still be wrong | Shown with a caveat (D43); evaluate on study photos (Sprint 8) |

## 7. Sprint 8 — release and evaluation support

Date: 2026-11-23 · Branch: `feature/release-evaluation`

| Check | Result |
|---|---|
| Unit tests | **93 tests, 0 failures** (adds the results CSV format and export flow) |
| Release signing | Checked with a throwaway key (deleted afterwards): `assembleRelease` and `bundleRelease` produce an APK and an `.aab` signed with it; without `keystore.properties` the release builds unsigned |
| Lint | 0 errors; 8 warnings, all version notices |
| Device tests | Compile (incl. new `CsvResultsExporterTest`). Not run in this session |
| Release APK on a phone | **Done 2026-11-24:** signed with the team's key, installed over USB on a team phone (model 2412DPC0AG); the `MANUAL_TESTS.md` Sprint 8 checks were reported all OK by the tester |

### Measuring inference time (D48)

With a phone connected, from `Mobile-App/`. The test installs the debug build,
so uninstall the release app first (this removes its saved history) and
reinstall it afterwards (`RELEASE.md` §3). In PowerShell, keep the `-P…`
argument in quotes (otherwise it's split at the first dot):

```powershell
adb logcat -c
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=io.github.ephym6.plantid.data.identification.model.ModelInferenceTest"
adb logcat -d -s PlantIdModel
```

Gradle says `BUILD SUCCESSFUL` even when the test couldn't be installed: check
the output for `Failed to install`. On Xiaomi/HyperOS phones, tap **Install** on
the phone's prompts, and if the test package fails with
`INSTALL_GRANT_RUNTIME_PERMISSIONS`, install and run it with adb instead:

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest
adb install -r -t app\build\outputs\apk\debug\app-debug.apk
adb install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb logcat -c
adb shell am instrument -w -e class io.github.ephym6.plantid.data.identification.model.ModelInferenceTest io.github.ephym6.plantid.test/androidx.test.runner.AndroidJUnitRunner
adb logcat -d -s PlantIdModel
adb uninstall io.github.ephym6.plantid.test
adb uninstall io.github.ephym6.plantid
```

Copy the `first (incl. model load) … ms; warm … ms` line into the table in
section 6 and check `IdentificationViewModel.TIMEOUT_MILLIS` still leaves a
wide margin (D48).

New known issue:

| # | Issue | Impact | Plan |
|---|---|---|---|
| K17 | Failed identifications can't be saved, so they don't appear in the CSV | Must be noted by hand during the evaluation | Covered by `EVALUATION_PROTOCOL.md` |
| K18 | Identification took 2.6–8.8 s per photo on the measured phone, even for a 300×300 photo, and varied widely between runs. The cause hasn't been investigated (model run, decoding, or the phone limiting a background test) | Users wait several seconds; the progress screen covers it | Optional: time the model run separately from decoding, then decide whether to optimise |
