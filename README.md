# Tahadhari

**El Niño flood alerts for Kenya, built with Kotlin Multiplatform.**

Kenya is facing a very strong El Niño during the October–December 2026 short rains. Official
forecasts exist, but they are often technical, national-level and in English. Tahadhari turns open
rainfall and river forecasts into simple, county-level flood risk alerts in **English and Swahili**,
with clear advice on what to do.

- **Android**: pick your county, see the 7-day rainfall outlook, flood risk and what to do.
- **Desktop dashboard**: see all 47 counties ranked by risk, for county disaster teams and responders.

## Features

**Android app (residents)**
- County picker with search, or "Use my location" (coarse location, asked only when tapped; you
  confirm the suggested county). The app remembers your county and language.
- Risk card with the level shown by colour, shape and words; 7-day rain chart with threshold lines;
  river outlook; plain-language explanation; a checklist of what to do.
- Works offline: the last forecast is saved and shown with "Updated …" or a "Showing saved
  forecast" warning. Saved data older than 3 days is never used to show a risk level.
- **Safety** tab: offline guides for floods, landslides, cholera and safe water, livestock and
  farms, lightning and flash floods, with sources.
- **Emergency** tab: tap-to-dial 999, 112 and 1199 (Kenya Red Cross), your county's known hotspots,
  and "I'm safe" / "I need help" messages shared through any app.
- English and Swahili throughout.

**Desktop dashboard (county teams, Red Cross, NGOs)**
- Kenya county map coloured by risk (hover for details, click to select). Counties without data
  are hatched and labelled "No data", never shown as Low.
- Summary chips per level that filter a ranked list (wettest day, 3-day total, river rise).
- Side-by-side county detail, overall "last updated" time, refresh, EN/SW toggle.
- **Export CSV** of all 47 counties for reports and spreadsheets.

> ⚠️ Tahadhari provides guidance based on forecast data. It is **not an official warning**.
> Always follow the Kenya Meteorological Service and your county government.

## Run it

Requirements: JDK 17+, Android Studio (Ladybug or newer) with the Kotlin Multiplatform plugin,
or just the JDK for the desktop app.

Set `JAVA_HOME` to your JDK before running Gradle. On Windows, open a new terminal after installing
the JDK so it picks up the variable.

```bash
./gradlew :composeApp:run                        # desktop dashboard
./gradlew :composeApp:run --args="--mobile"      # phone layout in a desktop window (quick preview)
./gradlew :composeApp:installDebug               # Android (device or emulator connected)
./gradlew :composeApp:assembleDebug              # APK in composeApp/build/outputs/apk/debug/
./gradlew :composeApp:allTests :shared:jvmTest :server:test   # all tests
./gradlew :composeApp:packageDistributionForCurrentOS          # desktop installer (MSI/DMG/DEB)
```

### Try the key features

1. **Dashboard**: run the desktop app, wait for the 47 counties to load, click a "High" or
   "Severe" chip, hover over the map, click a county, then press **Export CSV**.
2. **Offline mode**: open a county, turn off your network and press refresh. The saved forecast is
   shown with a warning, never an error that looks like "Low".
3. **Phone flow**: on Android (or with `--args="--mobile"`), pick a county, switch to **SW**, open
   **Safety → Floods**, then **Emergency → I'm safe** to share a check-in message.
4. **Screenshots** of every screen with sample data (no network needed):
   `TAHADHARI_SCREENSHOTS=1 ./gradlew :composeApp:desktopTest`, output in `composeApp/build/screenshots/`.

## AI explanations (optional)

The app works fully offline with built-in English and Swahili explanations. With the AI server
running, it shows friendlier, county-specific explanations written by Claude, and falls back to
the offline text automatically if the server is slow or unavailable.

```bash
export ANTHROPIC_API_KEY=your-key       # never commit this
./gradlew :server:run                   # starts on http://localhost:8080
./gradlew :composeApp:run               # desktop picks up the server automatically
```

For Android, the emulator reaches your computer at `10.0.2.2`, which is the default. For a real
phone, deploy the server with HTTPS and add `tahadhari.proxyUrl=https://your-server` to
`local.properties`.

The AI only receives forecast facts calculated by the app. It never decides the risk level.

## How risk is calculated

Each county gets the highest level triggered by any of these signals (heuristic thresholds, not
official KMSA criteria):

| Signal | Moderate | High | Severe |
|---|---|---|---|
| Wettest single day | ≥ 20 mm | ≥ 50 mm | ≥ 80 mm |
| Wettest 3-day window | ≥ 40 mm | ≥ 80 mm | ≥ 120 mm |
| River forecast peak vs 30-day baseline | – | ≥ 2× | ≥ 3× |

## Data sources

- Weather and river discharge data from [Open-Meteo](https://open-meteo.com/) (forecast API and
  flood API, based on GloFAS), licensed under CC BY 4.0.
- County boundaries from [geoBoundaries](https://www.geoboundaries.org/) gbOpen KEN ADM1 (source:
  RCMRD), public domain. Runfola et al. (2020), *geoBoundaries: A global database of political
  administrative boundaries*, PLoS ONE 15(4): e0231866. Simplified with
  `tools/geo/build_county_shapes.py`.
- Safety guide content is based on Kenya Red Cross, Kenya Ministry of Health, WHO, USGS and US
  National Weather Service guidance. Each guide lists its sources in the app.

## License

MIT — see [LICENSE](LICENSE).
