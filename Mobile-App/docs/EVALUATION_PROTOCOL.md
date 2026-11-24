# PlantID — Evaluation Protocol (draft for the research lead)

> **Draft.** This proposes how to run the Sprint 8 evaluation with the app. The
> research lead decides the final protocol: sample sizes, participants, consent,
> questionnaires. Change this file to match what is approved.

The spec requires **two separate evaluations** that must not be mixed up:

| | A. Model accuracy | B. Usability |
|---|---|---|
| Question | How often is the identification right on new photos? | Can people complete the tasks and understand the results and care information? |
| Who | The team (testers) | Approved participants |
| Data | Results CSV + true plant per photo | Task observations, times, comments |
| Not | A usability measure | An accuracy measure |

The ML team's notebook reports test-set results (87.9% accuracy; 86.5% of
supported plants answered, 97.4% right when answered). Evaluation A checks
whether this holds on **phone photos taken by the team**, which can differ (other
cameras, lighting, backgrounds). Report both, side by side, and don't present
either as the other.

## A. Model accuracy

### Photos

- **Supported plants:** for each of the 30 species, an agreed number of new
  photos (for example 5–10), taken with the study phones in normal home or
  garden conditions. Don't use photos from the training sources (Plant100-CLS,
  iNaturalist).
- **Unsupported plants:** photos of plants that are **not** among the 30.
- **Non-plants:** a few photos of other objects.
- Record for each photo: the true plant (model label, e.g. `golden_pothos`, or
  `other_plant` / `not_a_plant`), phone model and lighting (indoor, outdoor,
  dim).

### Procedure

1. Install the signed **release** build (`RELEASE.md`). Start with empty History.
2. For each photo: **Identify plant**, then **Save to history**, whatever the
   result (identified, not sure or no supported plant). If identification
   **fails** (error screen), note it on the sheet: failures can't be saved.
3. When done: **History → Export results (CSV)** and send the file to the team.
4. In the CSV, fill `true_label` for every row (and `notes` if useful). Rows are
   in the order photos were identified.

### Measures (per species and overall)

For photos of **supported** plants (`true_label` is one of the 30):

| Measure | Formula |
|---|---|
| Answered rate | rows with `outcome = identified` / all rows |
| Accuracy when answered | `identified` rows where `predicted_label = true_label` / `identified` rows |
| Correct overall | `identified` and correct / all rows |
| Wrong answer rate | `identified` and wrong / all rows |
| "Not sure" rate | `not_sure` rows / all rows |

For **unsupported plants and non-plants**:

| Measure | Formula |
|---|---|
| Correctly not named | rows with `outcome` = `not_sure` or `no_supported_plant` / all rows |

Optional confidence check: group `identified` rows by `confidence` (e.g. 72–80%,
80–90%, 90%+) and compare each group's accuracy with its confidence. This shows
whether "87%" means about 87% right on these photos.

### Reporting rules

- State the number of photos per species. Don't report percentages from very
  few photos without saying so.
- Keep failed identifications (from the sheet) in the totals.
- Exclude any row with `demo = true` (only device tests produce those).
- Don't generalise beyond the photos tested (phones, places, lighting).

## B. Usability

Run only with **approved** participants and the agreed consent process.

### Tasks (proposed)

1. Identify a plant from a photo you take.
2. Find how often the plant typically needs water, and how much light.
3. Find whether the plant is safe for pets.
4. Read a "not sure" result: what does PlantID say, and what would you do next?
5. Save a result, then find it again in History.
6. Find what PlantID does with your photos (About → Privacy).

### Record per task

- Completed without help / with help / not completed.
- Time taken (optional).
- What the participant said they understood (e.g. how they read "Model
  confidence: 87%", and whether they think it means "guaranteed right").
- Problems observed, in the participant's words where possible.

A standard questionnaire (for example the System Usability Scale) can be added
after the tasks if the research lead approves.

### Reporting rules

- Describe what was observed. Don't claim that the app improves plant care or
  reduces plant deaths (spec §2.1).
- If a participant's photo is misidentified during a task, that's a usability
  observation (how they reacted). It is not part of the accuracy results.

## Data handling

- The results CSV holds no photos and no participant details: only dates,
  outcomes, plant names, confidence and model version.
- Keep observation notes and any consent forms as the research lead requires.
- After the study, testers can clear History in the app (History → Clear history).
