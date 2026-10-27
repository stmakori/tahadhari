# Tahadhari

**El Niño flood alerts for Kenya, built with Kotlin Multiplatform.**

Kenya is facing a very strong El Niño during the October–December 2026 short rains. Official
forecasts exist, but they are often technical, national-level and in English. Tahadhari turns open
rainfall and river forecasts into simple, county-level flood risk alerts in **English and Swahili**,
with clear advice on what to do.

- **Android**: pick your county, see the 7-day rainfall outlook, flood risk and what to do.
- **Desktop dashboard**: see all 47 counties ranked by risk, for county disaster teams and responders.

> ⚠️ Tahadhari provides guidance based on forecast data. It is **not an official warning**.
> Always follow the Kenya Meteorological Service and your county government.

## Run it

Requirements: JDK 17+, Android Studio (Ladybug or newer) with the Kotlin Multiplatform plugin,
or just the JDK for the desktop app.

```bash
./gradlew :composeApp:run              # desktop dashboard
./gradlew :composeApp:installDebug     # Android (device or emulator connected)
./gradlew :composeApp:desktopTest :shared:jvmTest :server:test   # tests
```

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

Weather and river discharge data from [Open-Meteo](https://open-meteo.com/) (forecast API and
flood API, based on GloFAS), licensed under CC BY 4.0.

## License

MIT — see [LICENSE](LICENSE).
