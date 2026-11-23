# PlantID Android — Manual Test Checklist

Automated tests can't drive other apps (the system photo picker and camera),
so check these by hand on a real device before merging a sprint. Record the
device, Android version, date and result. Use real results only: write
"not tested" rather than leaving a box ticked.

## Sprint 2 — Photo selection and preview

Device: ____________  Android: ____  Build/commit: ________  Tester: ____  Date: ________

| # | Steps | Expected | Result |
|---|---|---|---|
| 2.1 | Home → **Identify a plant** | Sheet shows *Take a photo* and *Choose from photos* | |
| 2.2 | History (empty) → **Identify a plant** | Same sheet opens | |
| 2.3 | Sheet → swipe down / tap outside / Back | Sheet closes, still on the same screen | |
| 2.4 | **Choose from photos** → pick a photo | Brief "Preparing your photo…", then *Check your photo* with the photo, not stretched or cropped | |
| 2.5 | **Choose from photos** → Back out of the picker without picking | Returns to the screen you started on, no error | |
| 2.6 | **Take a photo** → take and confirm a photo | Preview shows it the right way up | |
| 2.7 | **Take a photo** → cancel in the camera app | Returns to the starting screen, no error, no preview | |
| 2.8 | Check permissions: Settings → Apps → PlantID → Permissions | No permissions requested or listed; no permission prompt ever appeared | |
| 2.9 | On preview, **Choose another photo** → pick a different photo | Preview now shows the new photo; Back goes to where you started, not the old preview | |
| 2.10 | On preview, **Choose another photo** → cancel | The original photo is still shown | |
| 2.11 | On preview | **Identify plant** becomes enabled once the photo has loaded (disabled if the photo can't be shown) | |
| 2.12 | Preview → top-bar Back and system Back | Both return to the starting screen; bottom bar visible again | |
| 2.13 | Rotate the device on the preview (if rotation is on) | Same photo still shown | |
| 2.14 | Pick a non-image or damaged file if your picker allows it, or a HEIC photo on Android 8/9 | "Can't open this photo" dialog; OK returns you safely | |
| 2.15 | Camera taken, then background PlantID for a while (or enable Developer options → *Don't keep activities*), then repeat 2.6 | Photo still reaches the preview after returning from the camera | |
| 2.16 | Dark mode and large font size (Settings → Display) | Sheet, dialogs and preview readable, nothing clipped, page scrolls | |
| 2.17 | Gallery after taking a photo in PlantID | The photo is **not** added to the gallery | |

> **Since Sprint 7** the app uses the real model and no longer shows the Demo
> mode picker or demo banners. The demo-based checks in the Sprint 3–5 sections
> are now covered by device tests (which switch to the demo identifier). Use the
> Sprint 7 section for the real model.

## Sprint 3 — Mock identification flow

Device: ____________  Android: ____  Build/commit: ________  Tester: ____  Date: ________

All outcomes here come from the **demo** identifier. Do not record them as
model results.

| # | Steps | Expected | Result |
|---|---|---|---|
| 3.1 | Choose a photo → preview | **Demo mode** card lists 6 outcomes; *Plant identified* selected by default; **Identify plant** enabled once the photo shows | |
| 3.2 | Demo *Plant identified* → **Identify plant** | "Analyzing your plant…" with the photo, then result: **Demo result** banner, photo + "Golden Pothos" in one card, *Match found* | |
| 3.3 | Demo *Identified, some details missing* | "Demo Plant B", no scientific name, no *About this plant*, layout intact | |
| 3.4 | Demo *Not confident* | Warning banner "We couldn't confidently identify this plant", **no plant name**, *Try another photo* | |
| 3.5 | Demo *No supported plant* | "No supported plant found", **no plant name**, looks different from 3.4 | |
| 3.6 | Demo *Something goes wrong* → **Try again** | Error panel with Try again and Back to home; Try again shows Analyzing, then the error again (the demo outcome is still *Something goes wrong*) | |
| 3.7 | Demo *Plant identified (slow)* | Loading lasts about 6 s, no fake percentage, then the result | |
| 3.8 | Tap **Identify plant** several times quickly | Only one Analyzing screen; Back returns to the preview once | |
| 3.9 | During Analyzing, press Back | Returns to preview, no result appears later | |
| 3.10 | Result → **Identify another plant** / **Try another photo** → pick a photo | New preview; Back goes to where you started, not the old result | |
| 3.11 | Result → **Back to home** (also when started from History) | Home tab, bottom bar visible | |
| 3.12 | Result → top-bar Back | Returns to the preview with the same photo | |
| 3.13 | Rotate during Analyzing and on the result | Identification is not restarted; same result shown | |
| 3.14 | TalkBack on the result | Banner, status and plant name are read; status isn't conveyed by colour only | |
| 3.15 | Dark mode + large font on each result type | Readable, nothing clipped, page scrolls | |

## Sprint 4 — Care guide

Device: ____________  Android: ____  Build/commit: ________  Tester: ____  Date: ________

Identification results are still **demo** output.

| # | Steps | Expected | Result |
|---|---|---|---|
| 4.1 | Demo *Plant identified* → result | Golden Pothos / *Epipremnum aureum*, **Demo result** banner, **View care guide** button | |
| 4.2 | **View care guide** | Name, scientific name, family; *General guidance* note; Light 500–3,000 lux, Watering about every 7 days (with "not a fixed schedule" note), Temperature 15–30 °C, Humidity around 50 %; Source with CC BY-NC 4.0 | |
| 4.3 | Compare 4.2 with `care_tips.json` for *Epipremnum aureum* | Every number matches the source; nothing extra (no toxicity, no invented advice) | |
| 4.4 | Care guide → Back (top bar and system) | Returns to the same result | |
| 4.5 | Demo *Identified, some details missing* → **View care guide** | "No care guide available"; Back returns to the result | |
| 4.6 | Demo *Not confident* / *No supported plant* / *Something goes wrong* | **No** View care guide button | |
| 4.7 | About tab | Lists the supported plants (30 since Sprint 7) (common + scientific names); *Care information* card credits FloraDB and its licence | |
| 4.8 | Airplane mode, repeat 4.2 | Care guide still loads (offline) | |
| 4.9 | Dark mode + large font on the care guide | Readable, nothing clipped, page scrolls | |
| 4.10 | TalkBack on the care guide | Section titles are read as headings; values and notes are read | |

## Sprint 5 — Saving and history

Device: ____________  Android: ____  Build/commit: ________  Tester: ____  Date: ________

Device tests (`connectedDebugAndroidTest`) **clear the debug app's history**.
Run them before these checks, not after.

| # | Steps | Expected | Result |
|---|---|---|---|
| 5.1 | Fresh install (or Clear history) → Home | No "Recent identifications" section; no empty box | |
| 5.2 | Identify (demo *Plant identified*) → **Save to history** | Button turns into "Saved to history" confirmation | |
| 5.3 | Tap Save again / rotate after saving | Still saved; no second record appears in History | |
| 5.4 | **Back to home** | "Recent identifications" shows the record (thumbnail, Golden Pothos, Identified, date/time, Demo tag) | |
| 5.5 | Save results for *Not confident* and *No supported plant* | Listed as "Unidentified plant" with *Not confident* / *No supported plant* labels (icon + text) | |
| 5.6 | *Something goes wrong* result | No Save option | |
| 5.7 | Force-stop PlantID (Settings → Apps) and reopen | History and Home's recent section still show the saved records | |
| 5.8 | Open a saved identified record | Same photo, name and status as when identified; "Identified on …"; Demo banner; **View care guide** opens the guide and Back returns to the record | |
| 5.9 | Open a saved *Not confident* record | No plant name, no care guide button | |
| 5.10 | **Delete from history** → Cancel, then → Delete | Cancel keeps it; Delete returns to the previous screen and the record is gone everywhere | |
| 5.11 | History → **Clear history** (top bar) → Cancel, then → Clear all | Confirmation states how many records; Clear all empties History and Home's section | |
| 5.12 | After clearing, About and a care guide | Supported plants and care guides still there (bundled data not affected) | |
| 5.13 | History with 10+ records | Scrolls smoothly; newest first | |
| 5.14 | Settings → Google → Backup (if available) | PlantID isn't backed up (or backs up no data) | |
| 5.15 | Dark mode + large font + TalkBack on History and a saved record | Readable, nothing clipped; rows read name, status and date | |

## Sprint 6 — Hardening

Device: ____________  Android: ____  Build/commit: ________  Tester: ____  Date: ________

| # | Steps | Expected | Result |
|---|---|---|---|
| 6.1 | Settings → Display → **Font size** largest, **Display size** largest; walk Home → photo → preview → result → care guide → save → History → saved result → About | Nothing clipped or overlapping; every screen scrolls; all buttons reachable | |
| 6.2 | Same walk with the smallest display size | Layout still sensible | |
| 6.3 | Dark mode, same walk | Readable everywhere | |
| 6.4 | Developer options → **Don't keep activities** ON. Get a *Not confident* result, switch to another app, come back | Same *Not confident* result (not re-identified); Save still works once | |
| 6.5 | With 6.4 still ON: save a result, switch away and back | Still shows "Saved to history"; History has it once | |
| 6.6 | With 6.4 still ON: take a photo with the camera | Photo reaches the preview. Turn the option **OFF** afterwards | |
| 6.7 | **TalkBack** on: complete identify → result → care guide → save → History → delete using only TalkBack gestures | Every control is announced with a meaningful label; status isn't only colour; dialogs are announced | |
| 6.8 | Double-tap Back quickly on the care guide, the saved result and the preview | Goes back exactly one screen each time | |
| 6.9 | Double-tap **View care guide**, a History row, **Identify plant** | Opens one screen, once | |
| 6.10 | Airplane mode, full walk | Everything works offline | |
| 6.11 | Receive a call / open notifications / lock the screen during *Analyzing* (slow demo) | Returns to a sensible state; no crash | |

## Sprint 7 — Real model

Device: ____________  Android: ____  Build/commit: ________  Tester: ____  Date: ________

Record model results (correct / not sure / wrong) **separately** from
usability observations. Don't count them as the formal accuracy evaluation
unless the research protocol says so.

| # | Steps | Expected | Result |
|---|---|---|---|
| 7.1 | Any photo → preview | No Demo mode card; Home and results show no demo banners | |
| 7.2 | Clear photo of a supported FloraDB plant (e.g. Golden pothos, Snake plant, Monstera) | Correct name; **View care guide** shows light category + lux, watering, temperature, humidity, pet safety and the FloraDB source (CC BY-NC 4.0) | |
| 7.3 | Clear photo of a plant with converted values (Basil, Mint, Rosemary, Tomato, Duranta…) | Care guide shows **Converted from gardening advice**, **What the source says**, and its NC State / Missouri Botanical Garden source | |
| 7.4 | Blurry, dark or partial photo of a supported plant | Either identified, or "We couldn't confidently identify this plant" with **Possible matches (not confirmed)** (≤ 3 names, no percentages, no care links) | |
| 7.5 | Photo of a plant that isn't one of the 30 | "No supported plant found", or "not sure" with guesses. Note which | |
| 7.6 | Photo of something that isn't a plant (cup, wall, hand) | Not identified as a plant (ideally "No supported plant found"). Note what appears | |
| 7.7 | Same photo identified twice | Same answer both times | |
| 7.8 | First identification after a fresh start, then a second one | Note how long each "Analyzing" takes (first includes loading the model) | |
| 7.9 | Airplane mode, identify | Works offline | |
| 7.10 | Save a real result, then open it from History | Shows model result (no Demo tag); care guide link if identified | |
| 7.11 | Rotate during Analyzing; switch apps on the result (with *Don't keep activities*) | Not restarted; same result after returning | |
| 7.12 | Run `ModelInferenceTest` and copy the timing line into `TEST_REPORT.md` | Recorded | |
| 7.13 | Care guide of a toxic plant (e.g. Peace lily, Duranta) | **Pet safety** in warning style: toxic to dogs/cats, details, how it was established, source, and the "not veterinary advice" note | |
| 7.14 | Care guide of Ming aralia and Crown of thorns | Pet safety says **inferred from related plants (genus), not confirmed**; Ming aralia shows the lower-confidence warning | |
| 7.15 | Care guide of Bougainvillea and Peace lily | **Note from the PlantID team** explains the genus-level values | |
| 7.16 | About tab | Credits list FloraDB (CC BY-NC 4.0), NC State Extension, Missouri Botanical Garden, UF/IFAS and ASPCA sources | |
| 7.17 | Identified result | **Model confidence: N%** (N ≥ 72) with the note about the team's test photos; never 100% | |
| 7.18 | "Not sure" result | Possible matches show a % each (all below 72%); no confidence line for the result itself | |
| 7.19 | Save an identified result, open it from History | Same confidence shown; records saved before this update show none and still open | |
