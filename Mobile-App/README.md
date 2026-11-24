# PlantID — Android App

Android client for the PlantID research prototype: take or pick a photo of a
plant, see the identification result, read its care guide, and revisit past
identifications. All processing is meant to happen on the device, and the app
does not need an account.

> **Current status: Sprint 8 (release and evaluation).** Research prototype:
> the ML team's **PlantID v1** model runs on the phone (30 species, plus "not
> sure" and "no supported plant"), with care guides and pet safety for every
> species, model confidence, saved history and a CSV export for the evaluation.
> No account and no internet. Build a signed release with `docs/RELEASE.md`.

## Requirements

- Android Studio (current stable)
- Android SDK Platform 37 (install it from the SDK Manager)
- JDK: Gradle provisions the daemon toolchain (JDK 25) on its own through the
  foojay resolver, and Android Studio's bundled JDK also works
- An emulator or physical device running Android 8.0 (API 26) or newer

## Open and run

1. In Android Studio choose **File → Open** and select this `Mobile-App/`
   folder, not the repository root.
2. Wait for Gradle sync to finish.
3. Select a device and press **Run**.

## Command line

Run from `Mobile-App/` (use `gradlew.bat` on Windows cmd/PowerShell):

```bash
./gradlew assembleDebug              # build the debug APK
./gradlew testDebugUnitTest          # JVM unit tests
./gradlew connectedDebugAndroidTest  # instrumented + Compose UI tests (needs a device/emulator)
./gradlew installDebug               # install on a connected device
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project facts

| Item | Value |
|---|---|
| Application ID / namespace | `io.github.ephym6.plantid` |
| minSdk / targetSdk / compileSdk | 26 / 37 / 37 |
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Build | Android Gradle Plugin 9.5 (built-in Kotlin), Gradle wrapper 9.6, version catalog in `gradle/libs.versions.toml` |

## Code structure

Single `app` module. Current packages under `io.github.ephym6.plantid`:

```
PlantIdApplication.kt      # owns the AppContainer
MainActivity.kt            # sets the theme and PlantIdApp
di/AppContainer.kt         # manual dependency container
domain/identification/     # IdentificationService contract and outcome types
data/identification/model/ # real model: metadata, preprocessing, decision rule, LiteRT
domain/plant/              # Plant, PlantRepository, CareRecommendations (rule-based care tips)
data/image/                # PhotoStore: private copy of the selected photo
data/plant/                # builds the catalog from model/classes.json + model/care_tips.json
domain/history/            # HistoryRecord, HistoryRepository
data/history/              # Room database + downscaled photo copies for saved results
data/identification/mock/  # demo identifier, used only by device tests
ui/
├── PlantIdApp.kt          # app shell: bottom bar, navigation host, photo selection
├── navigation/            # type-safe routes, NavHost, navigation helpers
├── theme/                 # colours, typography, shapes, Spacing/Sizes tokens
├── components/            # buttons, cards, status message, loading/empty/error states, branding
├── identify/              # photo source sheet, preview, demo mode picker, identifying + result screens
├── care/                  # care guide screen
├── home/  history/  about/   # one package per screen
```

Planned: release preparation and research evaluation (Sprint 8).

### Saved history

- Saved results live in a Room database (`plantid.db`, schema in
  `app/schemas/`). Each record is a snapshot of what was shown, so old records
  don't change when plant data does.
- Each saved result keeps a downscaled photo copy (≤ 1024 px, no EXIF/location)
  in private storage. It's deleted with the record or by Clear history.
- PlantID's data is excluded from Android backups and device transfer.
- **Database changes** need a version bump and a migration in
  `PlantIdDatabase`. Commit the new schema JSON.
- **Device tests clear the debug app's history** (`HistoryFlowTest`).

### Plant data and care guides

- The catalog is built **at runtime** from the ML team's `model/classes.json`
  (the model's classes) and `model/care_tips.json` (care values and pet safety
  for all 30 species), both packaged straight from the root `model/` folder.
  There is no generated copy and no sync step. When the ML team updates those
  files, the next build uses them. `PlantDataParserTest` checks the real files.
- Care tips are **rule-based**: `CareRecommendations` keeps only plausible
  values that exist in the source (light category and lux, watering interval,
  temperature, humidity), and the screen words them with fixed templates in
  `strings.xml`. No advice is generated or invented.
- Each guide shows its **source** (FloraDB sample, CC BY-NC 4.0; or NC State
  Extension / Missouri Botanical Garden for 10 species), a note when values were
  **converted** from published advice (with the source's own wording), and the
  ML team's notes (for example, genus-level classes).
- **Pet safety** (dogs and cats) is shown with how it was established (ASPCA
  list, a university extension list, or inferred from the genus), its source,
  and a "not veterinary advice" note (DECISIONS D41).

### Photos and permissions

PlantID requests **no runtime permissions**. Photos come from the Android
Photo Picker (access to the chosen photo only) or the system camera app, which
writes into PlantID's cache through a FileProvider. The newest photo is kept as
a temporary private copy; nothing is uploaded. See `docs/DECISIONS.md` D12.

### UI conventions

- Use `MaterialTheme.colorScheme` / `typography` / `shapes`, `PlantIDTheme.extendedColors`,
  and the `Spacing` and `Sizes` tokens. Don't hard-code colours or dimensions in screens.
- Build screens from the shared components in `ui/components/`. Every component and
  screen has `@PreviewLightDark` previews.
- All user-facing text goes in `res/values/strings.xml`.
- Icons: Material Symbols Rounded as vector drawables in `res/drawable/ic_*.xml`.
- Screens take state and callbacks only. They never receive the `NavController`.

### Identification model

- The ML team's files in the repository's root `model/` folder are packaged
  directly as app assets (no copy here). The build fails if the folder is missing.
- `data/identification/model/` reads `model_metadata.json`, preprocesses the
  photo (upright, centre-crop, 224 × 224, raw RGB), runs the model with LiteRT
  1.4.2 off the main thread, and applies the ML team's "not sure" rule.
- Results show the model's **calibrated confidence** (e.g. "Model confidence:
  87%"), with a note that it was measured on the team's photos (D43).
- Details, the decision rule and how to update the model:
  [`docs/AI_INTEGRATION.md`](docs/AI_INTEGRATION.md).
- `MockIdentificationService` (demo outcomes) is now used **only by device
  tests**, through `AppContainer.useDemoIdentifier`.
- The debug APK is about 33 MB because LiteRT ships native code for four CPU
  types. A release App Bundle only delivers one.

## Documentation

- [`docs/DECISIONS.md`](docs/DECISIONS.md): technical and product decisions, and
  open questions with owners.
- [`docs/MANUAL_TESTS.md`](docs/MANUAL_TESTS.md): on-device checks for flows
  automated tests can't drive (photo picker, camera).
- [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md): test results, fixes and known
  issues.
- [`docs/RELEASE.md`](docs/RELEASE.md): signing, building and installing the
  release, and the pre-release privacy review.
- [`docs/EVALUATION_PROTOCOL.md`](docs/EVALUATION_PROTOCOL.md): draft protocol
  for the model-accuracy and usability evaluations (research lead to approve).
- [`docs/DEMO_CHECKLIST.md`](docs/DEMO_CHECKLIST.md): presentation preparation,
  flow and talking points.
- [`docs/AI_INTEGRATION.md`](docs/AI_INTEGRATION.md): how the trained model is
  integrated, its decision rule, and how to update it.

## Known limitations

- Identifies only the 30 supported species; other plants should come out as
  "not sure" or "no supported plant", but not always.
- Accuracy on new phone photos is lower than on the ML team's test set, and the
  calibrated confidence can be over-confident on such photos.
- Care values are general guidance: 20 species from the FloraDB sample, 10
  converted from university extension advice (marked in the app).
- Pet safety is not veterinary advice; 5 entries aren't ASPCA-verified
  (marked).
- Saved "not sure" results don't keep their possible matches.
- Phones only, portrait first; tablets and landscape aren't optimised.
- Full list: `docs/TEST_REPORT.md`.
