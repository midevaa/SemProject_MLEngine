# PlantID — Demonstration Checklist

For presenting the research prototype. Everything shown is the real on-device
model; there is no demo mode in the app.

## Before

- [ ] Phone charged; signed **release** build installed (`RELEASE.md`); launcher
      shows the PlantID leaf icon.
- [ ] Real plants or clear printed photos ready: at least two supported species
      the model gets right in your tests (for example Golden pothos and Snake
      plant), one unsupported plant, and one non-plant.
- [ ] Try each one on the demo phone beforehand: results can differ with
      lighting and angle.
- [ ] History: either clear it, or keep a few saved results ready to show.
- [ ] Normal font size; screen brightness up; screen mirroring tested if used.
- [ ] Airplane mode on (shows that nothing needs the internet).

## Flow (about 5 minutes)

1. **Home**: what PlantID is, and that it supports 30 selected plants.
2. **Identify a plant → Take a photo** of a supported plant.
3. **Result**: name, scientific name, **Model confidence** and what it means
   (the model's calibrated estimate on the team's test photos, not a guarantee).
4. **View care guide**: light category, watering (typical interval, not a
   schedule), temperature, humidity, **pet safety** with its source and the
   not-veterinary-advice note, source credit.
5. **Save to history → Back to home**: the recent result appears.
6. **Unsupported plant or non-plant**: "No supported plant found", or "not sure"
   with possible matches and their percentages. Explain that this is the
   intended honest behaviour.
7. **History**: open the saved result; mention **Export results (CSV)** for the
   evaluation.
8. **About**: supported plants, privacy (no account, no internet, nothing
   uploaded), data sources.

## If something goes wrong

- Wrong or unsure result on a supported plant: retake from closer, with the
  leaves clearly visible and good light. Say plainly that real-world accuracy is
  lower than on the test set, and that this is what the evaluation measures.
- Camera won't open: use **Choose from photos** with a photo taken beforehand.

## Talking points: limitations (say them, don't hide them)

- 30 species only; anything else should come out as "not sure" or "no supported
  plant", but sometimes doesn't.
- Confidence is calibrated on the team's photos; on new photos it can be
  over-confident.
- Care values: 20 species from the FloraDB sample, 10 converted from university
  extension advice (marked in the app); general guidance, not a guarantee.
- Pet safety is not veterinary advice; 5 entries aren't ASPCA-verified (marked).
