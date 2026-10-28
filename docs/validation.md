# Back-test: how would Tahadhari's risk levels have done in the March-May 2024 Kenya floods?

**Short answer.** On this one season, with free open forecast data, the app's risk levels were a weak
detector of the documented flood events. With the shipped thresholds, the ECMWF-based forecast
reached **High in 0 of 7** documented events (Moderate or higher in 4 of 7). The GFS-based forecast
reached High or higher in 3 of 7, but it also shows High or Severe on twice as many county-days, so
that is not clearly better than chance. Even a perfect rainfall forecast (ERA5) reached High in
0 of 7, because the rain a 25-30 km grid cell reports at the event sites stayed well below the
single-day thresholds. The river signal looked useful in principle, but it is currently attached to
the wrong map cells for the two Tana River counties. Details, caveats and tuning ideas follow.

Nothing here changes the app. `RiskEngine` thresholds were **not** modified; the tuning ideas at the
end are suggestions only.

## 1. What was tested

The back-test runs the app's real `RiskEngine` (and the real `RiverOutlook.fromSeries` and
`CountyForecast` types), not a re-implementation. For every one of the 47 county headquarters (the
same points the app uses) plus three event sites (Kitengela, Mai Mahiu, Budalang'i), and for every
"issue day" from 15 March to 31 May 2024 (78 days), it builds the 7-day forecast the app would have
seen that morning and records the level.

Three rainfall inputs are compared:

| Input | What it is | Honest description |
|---|---|---|
| **ECMWF IFS 0.25 deg forecast** | Archived model forecasts from Open-Meteo's Previous Runs API | A real forecast with real lead times (see below) |
| **GFS forecast** | Same API, NOAA GFS | A real forecast, wetter than ERA5 on average (mean 6.5 mm/day vs 4.4) |
| **ERA5 reanalysis** | Open-Meteo archive API | "If the forecast had been perfect" upper bound. It is itself a model estimate, not a gauge record |

### What "lead time" really means here

- For an issue day D, forecast day D+k is taken from the model run that was available **k days
  earlier** (Previous Runs API variables `precipitation_previous_dayK`, k = 0..6). So a day-3 value is a
  genuine 3-day-ahead forecast. Hours are summed into Nairobi (EAT) calendar days.
- This is an approximation of what the app saw: the app asks the live API on D and gets one
  consistent 7-day run, whereas here each day comes from a run issued at the same clock time k days
  earlier. Differences are small compared with the forecast errors reported below.
- **I first tried Open-Meteo's Historical Forecast API and the default `best_match` model, and
  rejected them.** The Historical Forecast API stitches the first hours of each model run into one
  series, so it is a short-lead forecast, not a "3 days ahead" one. Worse, for this period the numbers
  I checked (Nairobi, 22-25 April, and `best_match` day-0 at four points) were identical to ERA5 (for
  example 14.4 mm on 24 April), so they are not an independent forecast. That is why the two named
  models above are used. The live app uses `best_match`, whose make-up differs from these two; the
  results are a proxy for it, not the same thing.
- River discharge is a **hindcast**, not an as-issued forecast: the Flood API's historical series
  (GloFAS) is used for both the 30-day baseline (D-30..D-1) and the "forecast" peak (D..D+6). It therefore
  has perfect foresight of the river over the next 7 days and slightly flatters the river signal.

### Ground truth: documented impacts

A dated list of documented impacts, from the Kenya Red Cross, UN agencies, ACAPS and major news
outlets. Dates are the day the flooding was reported or began; several are approximate (reports lag
events, and rivers rise over days).

| ID | Event | County | Date used | Sources |
|---|---|---|---|---|
| E1 | Nairobi flash floods; bodies collected 25 Apr, Mathare, Kibera, Kayole worst hit | Nairobi | 24 Apr | [The Star](https://www.the-star.co.ke/news/2024-04-25-13-bodies-collected-after-heavy-rains-in-nairobi), [UN-Habitat](https://new.unhabitat.org/sites/default/files/2024/04/un-habitat_statement_on_the_nairobi_floods_26.04.2024.pdf) |
| E2 | Athi River bursts its banks, two deaths, road to Nairobi cut | Kajiado (Athi River/Kitengela area) | 24 Apr | [People Daily](https://peopledaily.digital/news/2-killed-by-floodwaters-in-athi-river), [People Daily](https://peopledaily.digital/news/athi-river-bursts-its-banks/amp) |
| E3 | Old Kijabe dam burst, Mai Mahiu, about 45 dead | Nakuru | 29 Apr | [Nation](https://nation.africa/kenya/counties/nakuru/maai-mahiu-tragedy-death-toll-rises-to-45-4606978), [Al Jazeera](https://www.aljazeera.com/amp/news/2024/4/29/dozens-of-people-dead-in-kenya-floods) |
| E4 | Kitengela homes flooded after heavy rain the night of 30 Apr; Red Cross rescues 1 May | Kajiado | 30 Apr | [The Star](https://www.the-star.co.ke/counties/central/2024-05-01-10-family-members-trapped-as-torrential-rains-cause-anguish-in-kajiado), [Citizen](https://citizen.digital/news/six-rescued-from-flooded-homes-in-kitengela-n341356), [People Daily](https://peopledaily.digital/news/kenya-red-cross-moving-families-in-kitengela) |
| E5 | River Nyando bursts banks at Ahero, thousands displaced, Kisumu-Nairobi highway cut | Kisumu | 5 May | [Capital FM](https://www.capitalfm.co.ke/news/2024/05/crisis-in-nyanza-as-river-nyando-bursts-its-banks/), [Citizen](https://citizen.digital/article/kisumu-nairobi-highway-rendered-impassable-after-river-nyando-bursts-its-banks-n341569) |
| E6 | River Tana bursts banks in Garissa town; Kenya Red Cross reports Garissa hardest hit, 550 households in 11 camps | Garissa | 6 May | [K24](https://www.k24tv.co.ke/news/garissa-most-affected-by-floods-138426/), [Eastleigh Voice](https://eastleighvoice.co.ke/heavy%20rains%20kenya/39684/red-cross-warns-garissa-residents-as-floods-intesify) |
| E7 | Tana River overflows after upstream downpours; Hola flooded, Tana Delta villages cut off (a Red Cross boat reached families about 15 May) | Tana River | 11 May | [OCHA Flash Update 6, 17 May](https://reliefweb.int/report/kenya/kenya-heavy-rains-and-flooding-update-flash-update-6-17-may-2024), [The Star, 26 May](https://www.the-star.co.ke/counties/coast/2024-05-26-red-cross-warns-of-worsening-flooding-crisis-in-tana-delta) |

Context: OCHA's update of 17 May reports about 278,000 people displaced and 291 deaths nationally
between 1 March and 16 May, with Nairobi (about 257,000 affected) and Garissa (about 36,000) the
two hardest-hit counties; ACAPS (14 May) lists Homa Bay, Kajiado, Mandera, Nairobi and Nakuru as most
severely affected as of 6 May and says 42 of 47 counties were affected
([ACAPS](https://www.acaps.org/en/countries/archives/detail/kenya-heavy-rainfall-and-floods)).
Note: ReliefWeb blocked my direct download, so the OCHA figures and the 11 May Hola date come from
search-result summaries of that page, not from my own reading of it. Please re-check them before
quoting.

**Not used in the rates:** Budalang'i (Busia) and Nzoia flooding, and Homa Bay and Mandera. I could
not find a reliably dated 2024 report for Budalang'i (one secondary source says about 400 families
had been sheltering at a school "since April"), and the sources for Homa Bay and Mandera give no event
dates. They are shown only as an informal check in section 6.

### How hits are scored

- **Hit at level X** = the engine showed at least level X for the county in any forecast issued
  **1 to 3 days before** the event date.
- **Lead time** = how many days before the event date the earliest High-or-higher forecast was issued
  (searching 6 days back).
- The headline rates use the **county HQ point**, because that is what a user of the app would see.
  Event sites are shown separately to show how much the point matters.
- Seven events is a tiny sample. A "chance baseline" is given: how often the engine would have
  shown the same level in a random 3-day window for the same counties.

## 2. Results per event (default thresholds)

Each cell: highest level in forecasts issued 1-3 days before / lead time in days of the first High or
Severe (a dash means none in the 7 days up to the event).

| ID | Event | Evaluated at | ECMWF forecast | GFS forecast | ERA5 (perfect) |
|---|---|---|---|---|---|
| E1 | Nairobi, 24 Apr | Nairobi HQ | Moderate / - | **High** / 2 | Moderate / - |
| E2 | Athi River, 24 Apr | Kajiado HQ | Moderate / - | Moderate / - | Moderate / - |
| E2 | | Kitengela site | Moderate / - | **High** / 3 | Low / - |
| E3 | Mai Mahiu, 29 Apr | Nakuru HQ | Moderate / - | **High** / 5 | Moderate / - |
| E3 | | Mai Mahiu site | Moderate / - | **High** / 2 | Moderate / - |
| E4 | Kitengela, 30 Apr | Kajiado HQ | **Low** / - | Moderate / - | **Low** / - |
| E4 | | Kitengela site | Moderate / - | **Severe** / 5 | **Low** / - |
| E5 | Nyando, 5 May | Kisumu HQ | Moderate / - | Moderate / 6 | **Low** / - |
| E6 | Tana, Garissa, 6 May | Garissa HQ | **Low** / - | **High** / 3 | **Low** / - |
| E7 | Tana River, 11 May | Tana River HQ | **Low** / - | **Low** / - | **Low** / - |

![Level by days before each event](validation/event-levels.png)

Summary at the county HQ points (what the app shows), with the chance baseline from section 5:

| Rainfall input | Moderate or higher | High or higher | Expected High+ by chance |
|---|---|---|---|
| ECMWF forecast | 4 / 7 | **0 / 7** | 0.9 |
| GFS forecast | 6 / 7 | 3 / 7 | 1.4 |
| ERA5 (perfect) | 3 / 7 | **0 / 7** | 0.9 |

At the event sites instead of HQs: ECMWF 5/7 Moderate+ and 0/7 High+; GFS 6/7 and 5/7; ERA5 2/7 and 0/7.

Reading it plainly:

- **Misses are the main result.** Tana River and Garissa (E6, E7) were Low in the ECMWF and perfect-rain
  cases, even though they were among the worst-hit counties. Kisumu (E5) was Low with perfect rain.
  The Kitengela site (24 April) and the Kajiado HQ for 30 April were Low with perfect rain.
- **A perfect forecast did not help.** ERA5 never reached High for any event. At the event points,
  its largest daily total in the window around each event was 1 to 24 mm
  (see the chart below), under the 50 mm High threshold and mostly under the 20 mm Moderate one.
  The floods came from intense local downpours that a 25-30 km grid smooths out, from rain falling
  upstream (Tana, Nyando), or from infrastructure failure (the Old Kijabe dam).
- **GFS looks better but is partly wetter, not smarter.** Its mean rain is 48 % above ERA5 and it shows
  High or Severe on 8.4 % of county-days (ECMWF 4.0 %, ERA5 4.8 %). Its 3/7 compares with 1.4 expected by
  chance, which is weak evidence with n = 7. Its best days (E4, Kitengela, Severe) came with a 35 mm
  forecast where ERA5 saw under 8 mm, so it can be right for the wrong reasons.
- **Lead times** exist only for GFS, and the values are scattered (2 to 5 days for the High hits, plus a High at 6 days
  for Kisumu that came from a 67 mm GFS day that did not verify). I would not claim a typical lead time from this.
- **Rainfall forecasts themselves were poor at finding heavy days.** Against ERA5, across the 47
  HQs (152 days with 20 mm or more), the same-day ECMWF run found 40 % and the day-1 run 28 %. Of the
  day-1 alarms at 20 mm, 80 % did not verify. Only 4 days reached 50 mm in ERA5 at these points. At day 1
  ECMWF caught none, and GFS caught 1 of 4 while 97 % of its 50 mm alarms were wrong.

![Forecast skill by lead time](validation/forecast-skill.png)

![Rain at the event points versus thresholds](validation/event-rainfall.png)

## 3. Upstream rain for the Tana events

Garissa and Tana River flooded after rain in the Mt Kenya and Aberdare headwaters, not local rain.
Checking the highest level reached in any of Nyeri, Murang'a, Embu, Meru and Kirinyaga in the week up to each event:

| Event | ECMWF | GFS | ERA5 |
|---|---|---|---|
| E6 Garissa | High (Murang'a) | Severe (Murang'a) | Moderate (Nyeri) |
| E7 Tana River | High (Embu) | Severe (Murang'a) | Moderate (Nyeri) |

So the signal existed upstream, but the app only looks at a county's own point.

## 4. The river signal

Counted over all 47 HQs and 78 days, **122 of ECMWF's 147 High-or-Severe county-days (83 %) were added only by the river
signal** (GFS: 109 of 309; ERA5: 118 of 177). Rain alone gave High or Severe on 25, 200 and 59 county-days.

Three problems are visible in the data:

1. **The Tana River and Garissa river points sit on dry grid cells.** The GloFAS cells used have a mean
   discharge of 0.07 and 0.04 m3/s over the period, against more than 2,000 m3/s nearby on 10 May.
   Their "rise ratio" is dominated by noise (up to 464x at Tana River, 345x at Garissa) and the signal fired
   on 25 and 9 days (April to early May), then was around 0.4-0.8 during the actual floods. Probing neighbouring 0.05-degree cells,
   cells near (-0.55, 39.55) for Garissa and (-2.32, 40.17) for Tana River carry the real river (2,300-2,700 m3/s
   at the time of the floods). Using those cells, the ratio was **4.0-7.8 on each of the 10 days before the Garissa
   event** and **2.3-6.9 before the Tana River event (3.4 or more from 3 to 10 days before)**, i.e. a lead of
   roughly 1-10 days at the app's current 2x/3x thresholds, on hindcast data. That is the single most promising
   finding here, with the caveats that it is hindcast and that the 2x level was exceeded on 41-42 % of all days in the season at those cells.
   This ratio uses the same formula as `RiverOutlook`; it is not a `RiskEngine` level.
2. **A wet season inflates the baseline.** At Kisumu (Nyando) the ratio on the days before the 5 May flooding was only
   1.1-1.9, so the river signal did **not** fire for E5, while it fired on 24 earlier days (1-24 April) that have no
   documented event in my list. At Busia (Nzoia) the ratio stayed around 2.3-2.8 for the 9 days before 1 May.
3. **Other river points are suspect.** Siaya's Nzoia point has a mean of 4.8 m3/s while Busia's has 356 m3/s, and
   Kisumu's Nyando cell has a mean of 2 m3/s. These need to be checked against the river's real position.

## 5. How often the engine says High or Severe (a false-alarm proxy)

All county HQs, all 78 issue days (3,666 county-days):

| Thresholds | Input | Moderate | High | Severe | High + Severe |
|---|---|---|---|---|---|
| default | ECMWF | 21.1 % | 1.8 % | 2.2 % | 4.0 % |
| default | GFS | 20.5 % | 4.4 % | 4.0 % | 8.4 % |
| default | ERA5 | 20.7 % | 2.5 % | 2.4 % | 4.8 % |

![Share of county-days at each level](validation/level-distribution.png)

- About **one county-day in five is Moderate** in a wet season. That is a lot, and it is not rare enough to read as a warning on its own.
- I **cannot give a real false-alarm rate**. My list of documented events is short and the real flooding affected 42 of 47 counties.
  Almost every High or Severe county-day (147 of 147 for ECMWF, 301 of 309 for GFS, 177 of 177 for ERA5) falls outside +/-3
  days of a documented event in the same county, so the number above is an **upper bound on false alarms, not a measurement**.
  Many were probably real heavy rain somewhere in a large county.
- Where the High and Severe days sit: for ECMWF, 123 of 147 are in the six river counties, led by Narok (33),
  Tana River (25), Kisumu (24) and Busia (23). Narok's river signal ran from 2 April to 4 May.
- Chance baseline: the expected number of High-or-higher "hits" by chance across the 7 events is 0.9 (ECMWF), 1.4 (GFS) and 0.9 (ERA5);
  for Moderate-or-higher, 2.6, 2.8 and 2.1 (this includes the river counties' persistent signal).

## 6. Informal check on undated impacts

Highest level between 20 April and 10 May (HQ points): Busia (Nzoia) High with all three inputs, through the river signal
(Moderate at the Budalang'i site); Homa Bay Moderate (ECMWF, GFS) and Low (ERA5); Mandera Moderate (ECMWF), High (GFS), Low (ERA5). Not scored.

## 7. Threshold and design suggestions

These are suggestions backed by the numbers above; none has been applied. They are tested on the same 7 events
they were derived from, so treat any improvement as optimistic, and re-run on other seasons (2023 October-December
short rains, the 2026-27 season) before adopting.

1. **Fix the river points first (highest value, no threshold change).** Move the Tana River and Garissa points to cells that carry the
   river (candidates above), and check every river point against its cell's mean discharge. On the right cells the signal showed a
   4-7x rise ten days before Garissa. Also add a floor so a tiny baseline cannot produce huge ratios (for
   example, ignore the signal when the 30-day baseline is below about 5-10 m3/s; the current Tana ratios reached 464x on 0.07 m3/s).
2. **Do not lower the rainfall thresholds just to catch these events.** A what-if with daily 15/35/60 mm and 3-day 30/60/100 mm
   (`rain_lower`) raised ECMWF High-or-higher hits from 0/7 to 1/7 at HQs (GFS 3/7 to 5/7, ERA5 still 0/7), while roughly doubling the High+Severe
   share (ECMWF 4.0 % to 8.1 %, GFS 8.4 % to 14.7 %, ERA5 4.8 % to 8.4 %) and raising Moderate from about 21 % to 26-33 %. The gain is
   small and the alarm cost is large. Keep the defaults until there is more evidence.
3. **Consider an urban setting for Nairobi** (and perhaps Mombasa). Nairobi's flood at 24 April came with only 14 mm (ERA5) to 24 mm
   (ECMWF day-1) at the HQ point. A lower Nairobi-only High threshold might be justified, but this is one event, so treat it as a hypothesis.
4. **Do not add a 14-day accumulated-rain signal on this evidence.** I checked it: the 14-day ERA5 total before the events ranged from the 29th to
   the 80th percentile of the county's own season (Nairobi 54th, Kajiado 80th, Nakuru 74th, Kisumu 63rd, Garissa 59th, Tana River 29th). It did not separate
   the events from ordinary wet-season days.
5. **Use upstream counties for Tana-basin alerts.** Section 3 shows Nyeri, Murang'a and Embu forecasts already high in the week before E6 and E7;
   surfacing "heavy rain upstream, river may rise" could be a better signal than a local reading for Garissa and Tana River.
6. **Wording.** Moderate on about one day in five means "Moderate" should be described as "keep watching", not as a warning, and the app should keep
   its disclaimer on every risk screen. The numbers here do not support presenting the level as a reliable flood detector.

## 8. Limitations

- **Seven events, one season, dates approximate.** No confidence interval worth quoting. The documented list is incomplete.
- **Point sampling.** The app samples one point per county (the HQ); events happen at specific places inside large counties. The three event-site checks show it matters (e.g. Kitengela vs Kajiado HQ).
- **Resolution.** ECMWF, GFS and ERA5 grids are about 25-30 km. Flash floods come from local convection a grid cannot show. ERA5 also tends to under-estimate extremes. I did not use gauge or satellite rainfall (for example IMERG or CHIRPS), which could show the true event-point rainfall better.
- **Forecast archive.** The Previous Runs archive starts in January 2024 for most models. Lead times are same-clock-time runs, not exactly what the app would have received; `best_match` (the live default) was not usable (see section 1).
- **River data is a hindcast** (reanalysis-like historical series, with perfect foresight of the next 7 days) and uses 0.05-degree cells, which can miss the river (as with the Tana points).
- **Event impacts are not equal.** A dam failure (Mai Mahiu), urban drainage flooding (Nairobi) and a river overtopping (Tana) are different hazards; only rainfall and river rise are modelled here.
- **Engine snapshot.** This used the three-signal `RiskEngine` on this branch. The landslide signal being added elsewhere is not evaluated.
- **Threshold scenarios are in-sample.**
- **Sources.** Death tolls and displacement numbers differ between sources and dates; none of them are used in the scoring, only the dates and places of events.

## 9. Re-running it

```bash
python -I tools/backtest/fetch.py            # about 50 API requests; cached under build/backtest-cache/ (git-ignored)
python -I tools/backtest/probe.py            # optional: finds the better Tana river cells (~4 requests)
TAHADHARI_BACKTEST=1 ./gradlew :composeApp:desktopTest --tests "ke.tahadhari.BacktestRunnerTest"
python -I tools/backtest/analyze.py          # event tables, distribution, skill; writes tools/backtest/out/*.csv
python -I tools/backtest/analyze_river.py    # river diagnostics
python -I tools/backtest/analyze_chance.py   # chance baseline
python -I tools/backtest/analyze_antecedent.py
java tools/backtest/Charts.java              # redraws docs/validation/*.png
```

The compact data used (`tools/backtest/data/*.json`, about 1.1 MB) is committed, so everything from the Gradle step onward works without
network access. Without `TAHADHARI_BACKTEST=1` the test does nothing and makes no network calls (nor does it with the gate on; only
`fetch.py` and `probe.py` use the network). The test also runs the what-if threshold sets using the public `RiskThresholds` parameters,
without changing the shipped defaults.

## 10. Sources and attribution

- Weather and river data: [Open-Meteo](https://open-meteo.com/), licensed [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
  APIs used: Previous Runs API (ECMWF IFS 0.25 deg and NOAA GFS runs), Archive API (ERA5, Copernicus Climate Change Service / ECMWF), Flood API (GloFAS, Copernicus Emergency Management Service).
  API docs: [Previous Runs](https://open-meteo.com/en/docs/previous-runs-api), [Historical Forecast](https://open-meteo.com/en/docs/historical-forecast-api).
- Events: the Star, Nation, Al Jazeera, Capital FM, Citizen Digital, People Daily, K24 and Eastleigh Voice (links in the table in section 1);
  [UN-Habitat statement on the Nairobi floods, 26 April 2024](https://new.unhabitat.org/sites/default/files/2024/04/un-habitat_statement_on_the_nairobi_floods_26.04.2024.pdf);
  [ACAPS, Kenya heavy rainfall and floods](https://www.acaps.org/en/countries/archives/detail/kenya-heavy-rainfall-and-floods);
  [OCHA Kenya Flash Update 6](https://reliefweb.int/report/kenya/kenya-heavy-rains-and-flooding-update-flash-update-6-17-may-2024) (read through search summaries only);
  Kenya Red Cross situation reports (my fetch timed out, not used for figures).

This is guidance, not an official warning. Follow the Kenya Meteorological Department and your county's advisories.
