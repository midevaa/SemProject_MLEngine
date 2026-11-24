# PlantID Android — Release Build

How to build the signed app the study uses, and what to check before giving it
to anyone (DECISIONS D44).

## 1. Create the signing key (once)

The key identifies the app. Keep it safe: if it's lost, installed copies can't be
updated with a new build. **Never commit the key or its passwords.** `*.jks`,
`*.keystore` and `keystore.properties` are git-ignored.

From `Mobile-App/`, as **one line** (Windows PowerShell and cmd don't accept the
`\` line breaks some guides use):

```powershell
keytool -genkeypair -v -keystore plantid-release.jks -alias plantid -keyalg RSA -keysize 2048 -validity 10000
```

- `keytool` comes with the JDK (Android Studio's is in `<Android Studio>\jbr\bin`).
- It asks for a keystore password twice: choose a strong one and write it down.
- The name and organisation questions go into the certificate. Use the project
  name, or press Enter to skip, then type `yes`.
- Newer Java versions don't ask for a separate key password: use the same
  password for both in `keystore.properties`.

Keep a backup of the `.jks` file and the password somewhere private (a password
manager or private drive, **not** the repository). Only whoever builds releases
needs it.

Then create `Mobile-App/keystore.properties` (for example `notepad keystore.properties`):

```properties
storeFile=plantid-release.jks
storePassword=<the store password>
keyAlias=plantid
keyPassword=<the key password>
```

`storeFile` is relative to `Mobile-App/`. Without this file, release builds still
work but are **unsigned** (they can't be installed). Check with `git status` that
neither file appears: both are git-ignored.

## 2. Build

From `Mobile-App/` (in PowerShell write `.\gradlew.bat` instead of `./gradlew`):

```bash
./gradlew testDebugUnitTest          # must pass first
./gradlew assembleRelease            # signed APK for testers' phones
./gradlew bundleRelease              # App Bundle (.aab), only for a store upload
```

| Output | Path |
|---|---|
| APK | `app/build/outputs/apk/release/app-release.apk` |
| App Bundle | `app/build/outputs/bundle/release/app-release.aab` |

Release builds are optimised and shrunk with R8. The APK holds LiteRT's native
code for all four CPU types (about 27 MB). An App Bundle lets a store deliver
only one.

**Before each build given to testers,** raise `versionCode` (and `versionName`
if you like) in `app/build.gradle.kts`, so phones accept it as an update.

## 3. Install

A release build can't be installed over a debug build. Uninstall the debug app
first (this also removes its saved history).

**Over USB** (phone with USB debugging on). Android Studio installs `adb` but
doesn't put it on the PATH; in PowerShell, from `Mobile-App/`:

```powershell
$env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"   # this window only
adb uninstall io.github.ephym6.plantid                           # removes the debug build
adb install -r app\build\outputs\apk\release\app-release.apk
```

- `INSTALL_FAILED_USER_RESTRICTED` on Xiaomi/HyperOS phones: turn on
  *Developer options → Install via USB* and try again.
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: the debug build is still installed.

**Or send the APK file.** Testers allow "Install unknown apps" for the app they
open it with (for example Files or Drive) when Android asks.

## 4. Check before handing it out

R8 can break things that only show in release builds, so test the **installed
release APK**, not just the debug build:

- [ ] Run `MANUAL_TESTS.md` Sprint 8 on the release APK (identify, care guide,
      save, history, export, About).
- [ ] **Privacy review**: the About privacy text still matches the app:
  - [ ] no permissions requested (`aapt2 dump permissions app-release.apk`
        lists none besides the AndroidX-internal signature permission);
  - [ ] no internet use (no `INTERNET` permission);
  - [ ] photos: temporary copy of the current photo; saved results keep a
        smaller copy without location data; nothing uploaded;
  - [ ] backups and device transfer exclude PlantID's data;
  - [ ] the CSV export happens only when the user chooses it, without photos.
- [ ] `docs/TEST_REPORT.md` updated with the build, devices and results.
