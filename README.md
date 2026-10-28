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
- **Home-screen widget** with your county's risk, refreshed every 3 hours (shows "No data" rather
  than Low when the forecast can't be loaded).
- First-launch intro, an **About / How it works** page (risk thresholds, data sources, privacy),
  and pull-to-refresh.
- English and Swahili throughout, light and dark themes, screen-reader labels, and layouts tested
  at 1.5× text size.

**Desktop dashboard (county teams, Red Cross, NGOs)**
- Kenya county map coloured by risk (hover for details, click to select). Counties without data
  are hatched and labelled "No data", never shown as Low.
- Summary chips per level that filter a ranked list (wettest day, 3-day total, river rise).
- Side-by-side county detail, overall "last updated" time, EN/SW toggle, About dialog.
- Refreshes automatically every 3 hours, keeping the current data on screen while it reloads.
- **Export CSV** of all 47 counties, or a printable **HTML briefing** (counts, ranked table, and
  what to do in High and Severe counties) for morning briefings.
  [Sample briefing](docs/briefing-sample.html).
- Remembers language, selected county and filter between runs.

## Screenshots

| Dashboard | Dashboard (Swahili, filtered to High) |
|---|---|
| ![Dashboard](docs/screenshots/dashboard-wide-light.png) | ![Dashboard in Swahili](docs/screenshots/dashboard-wide-sw-filtered-light.png) |

| County detail | Swahili, dark | Safety guide | Emergency |
|---|---|---|---|
| ![County detail](docs/screenshots/detail-english-light.png) | ![County detail in Swahili](docs/screenshots/detail-swahili-dark.png) | ![Flood guide](docs/screenshots/phone-guide-floods-sw-light.png) | ![Emergency](docs/screenshots/phone-emergency-en-light.png) |

| County picker | Location | About | Intro |
|---|---|---|---|
| ![Picker](docs/screenshots/picker-light.png) | ![Location card states](docs/screenshots/phone-location-card-en-light.png) | ![About](docs/screenshots/phone-about-en-light.png) | ![Intro](docs/screenshots/phone-intro-2-sw-dark.png) |

Screenshots are rendered from sample data by `TAHADHARI_SCREENSHOTS=1 ./gradlew :composeApp:desktopTest`.

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
   "Severe" chip, hover over the map, click a county, then use **Export → Export briefing** and
   open the file in a browser.
2. **Offline mode**: open a county, turn off your network and press refresh. The saved forecast is
   shown with a warning, never an error that looks like "Low".
3. **Phone flow**: on Android (or with `--args="--mobile"`), pick a county, switch to **SW**, open
   **Safety → Floods**, then **Emergency → I'm safe** to share a check-in message.
4. **Widget**: on Android, choose a county in the app, then long-press the home screen →
   Widgets → Tahadhari.
5. **Screenshots** of every screen with sample data (no network needed):
   `TAHADHARI_SCREENSHOTS=1 ./gradlew :composeApp:desktopTest`, output in `composeApp/build/screenshots/`.

### Release builds

- `./gradlew :composeApp:assembleRelease` builds a minified APK (R8). To sign it, copy
  `keystore.properties.example` to `keystore.properties` (git-ignored) or set the
  `TAHADHARI_KEYSTORE_*` environment variables. Without them the APK is debug-signed, with a
  warning, and must not be published.
- CI (`.github/workflows/ci.yml`) runs every test and builds the APK and a Linux installer on each
  push. Pushing a `v*` tag runs `release.yml`, which builds the APK, MSI, DMG and DEB and attaches
  them to a GitHub Release.

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
