# PlantID — Model Integration

How the trained model is connected to the Android app, and how to update it.

> **Status (Sprint 7):** the app runs the ML team's **PlantID v1** model on the
> device (30 species + `other_plant`, MobileNetV2, float16 TFLite, 4.6 MB).
> The demo identifier is now used **only by device tests**.

## 1. Files and where they come from

The ML team owns the repository's root [`model/`](../../model) folder. The app
packages it **directly as assets** (`app/build.gradle.kts`), with no copy in
`Mobile-App/`:

| File | Used for |
|---|---|
| `plantid_v1.tflite` | The model (stored uncompressed in the APK so it can be memory-mapped) |
| `labels.txt` | Output order: index *i* is line *i* |
| `model_metadata.json` | Input size, labels, `other_plant_index`, the "not sure" rule (temperature, threshold), model name and file |
| `classes.json` | Common and scientific names per label |
| `care_tips.json` | Care values, light category, confidence, sources and pet safety for all 30 species, keyed by label |

The build stops with a clear message if `model/` is missing.

## 2. The contract

The UI depends only on `IdentificationService`. `ModelIdentificationService`
implements it:

- **Off the main thread.** Decode and preprocess the photo, then run inference
  on `Dispatchers.Default`. The model is loaded once, on first use.
- **Never throws, never fakes.** A missing photo gives `Failed(PhotoUnavailable)`.
  A model that can't load gives `Failed(ModelUnavailable)`. An undecodable photo,
  an inference error or a label missing from the catalog gives
  `Failed(ProcessingFailed)`.
- **`ModelInfo`** comes from the metadata: name `PlantID v1`, version
  `plantid_v1.tflite float16`, `isDemo = false`. It is stored with every saved
  result.
- **Scores:** the calibrated probability of every class is carried with the
  outcome. The UI shows the answer's as **"Model confidence: X%"** and the
  possible matches' percentages, with a note that they were measured on the ML
  team's photos (D43). Saved results keep the confidence.

### Preprocessing (from `model_metadata.json`)

1. Apply EXIF orientation (Coil decodes the photo upright, at a reduced size
   with the shorter side at least 448 px, to save memory).
2. Centre-crop the largest square.
3. Resize to 224 × 224, bilinear.
4. Raw RGB 0–255 as float32, **no normalisation** (the model does it).

### Decision rule (from `model_metadata.json`, resolves Q2)

```
p = softmax(log(probs) / temperature)          temperature = 0.7
if argmax(p) == other_plant or max(p) < threshold:  "not sure"   threshold = 0.72
else: labels[argmax(p)]
```

App mapping (DECISIONS D37):

| Model answer | App outcome | UI |
|---|---|---|
| Top answer is `other_plant` | `Unsupported` | "No supported plant found", no names |
| Top answer below threshold | `Uncertain` | "We couldn't confidently identify this plant" + **Possible matches (not confirmed)**: up to 3 guesses (never `other_plant`) with their %, no care-guide links |
| Otherwise | `Identified` | Plant name, **model confidence %** (always at least the 72% threshold), care guide, save |

The temperature and threshold are read from the metadata, so the ML team can
retune them without app code changes.

### Labels → plants

```
output index i → labels.txt line i → plant with that label (classes.json + care_tips.json) → name, care guide
```

The catalog is built at runtime from `model/classes.json` and
`model/care_tips.json` (`PlantDataParser`). Unit tests against the real files
fail if any model label (except `other_plant`) has no plant, if a species lacks
complete care values or pet safety, or if a cited source site has no name.

## 3. Updating the model (for example, more species)

1. ML team: replace the files in `model/` (model, `labels.txt`, `classes.json`,
   `model_metadata.json`, `care_tips.json`).
2. `cd Mobile-App && ./gradlew testDebugUnitTest`. The parser and metadata
   tests check that the files are consistent and that the app can read them.
3. Run `ModelInferenceTest` on a phone and record the timings
   (`TEST_REPORT.md`).
4. Commit the `model/` changes. Nothing in `Mobile-App/` needs regenerating.

If the input format, output layout or decision rule changes in kind (not just
values), the app code in `data/identification/model/` must change too.

## 4. Runtime

**LiteRT 1.4.2** (`com.google.ai.edge.litert:litert`, the classic
`org.tensorflow.lite.Interpreter` API). LiteRT 2.x is compiled with Kotlin 2.4
and can't be used with the project's Kotlin 2.2.10 compiler (DECISIONS D38).

## 5. Still to do

| Item | Owner |
|---|---|
| Measure inference time on the team's phones (`ModelInferenceTest`), then set `IdentificationViewModel.TIMEOUT_MILLIS` from it (now a provisional 30 s) | App team |
| Test with real photos of each supported species on the phones, recording correct / not sure / wrong separately from usability findings | Research + AI leads |
| Check the terms of use of the care and toxicity sources other than FloraDB (NC State Extension, Missouri Botanical Garden, UF/IFAS, ASPCA) before any distribution | Research lead (Q19) |
