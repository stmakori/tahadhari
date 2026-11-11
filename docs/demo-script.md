# Tahadhari demo video script (target 2:45, hard limits 2:30 to 3:00)

Format: 1080p, 16:9. Phone footage is an emulator window centred on a dark background; the
dashboard is full screen. English voice-over, English captions burned in (many viewers watch with
sound off). Keep every caption under about 9 words and on screen for at least 2.5 s.

Timings are cumulative. Voice-over (VO) is about 150 words per minute, so each block's word count
fits its time.

## Facts used in the problem section, with sources

Use only what you re-check on the day of recording. Figures differ by source and date, so always
say "as of" and name the source on screen.

| Claim | Figure | Source |
|---|---|---|
| Long-rains floods, 1 March to 16 May 2024 | at least 291 people killed, 188 injured, 75 missing; 278,380 displaced; about 412,763 affected (National Disaster Operations Centre figures as quoted by OCHA) | OCHA, "Kenya: Heavy Rains and Flooding Update, Flash Update #6" (17 May 2024), https://reliefweb.int/report/kenya/kenya-heavy-rains-and-flooding-update-flash-update-6-17-may-2024 |
| Early snapshot | 32 deaths, 103,485 affected, 40,265 displaced as of 18 April (Kenya Red Cross) | OCHA Flash Update #2, https://reliefweb.int/report/kenya/kenya-heavy-rains-and-flooding-update-flash-update-2-19-april-2024 |
| Old Kijabe dam burst, Mai Mahiu, 29 April 2024 | dozens killed; first reports 42 to 45 dead, later reports higher (61 by 7 May per Wikipedia summary) | OCHA Flash Update #3, https://reliefweb.int/report/kenya/kenya-heavy-rains-and-flooding-update-flash-update-3-29-april-2024 ; Al Jazeera, 29 April 2024, https://www.aljazeera.com/news/2024/4/29/dozens-of-people-dead-in-kenya-floods |
| Disaster record | ReliefWeb "Kenya: Floods - Apr 2024" (FL-2024-000045-KEN) | https://reliefweb.int/disaster/FL-2024-000045-KEN |
| El Niño 2026 to 2027 | FEWS NET expects a wetter than normal October to December 2026 short-rains season in much of East Africa (moderate to strong El Niño, positive Indian Ocean Dipole); WMO outlook of 29 July 2026 expects El Niño to exceed strong thresholds Aug to Oct and peak near year end | https://fews.net/topics/el-nino-2026-2027 ; https://fews.net/east-africa/seasonal-monitor/july-2026 |

Not verified (do before recording):
- The ReliefWeb pages returned HTTP 403 to our fetch tool. The 291, 188, 75, 278,380 and 412,763
  figures come from a search-engine summary of OCHA Flash Update #6; open the page and confirm them.
- I found no single consolidated final death toll for the 2024 season. Say "at least 291 by mid-May"
  rather than a round "300".
- NOAA's current ENSO status and the Kenya Meteorological Department's official October to December
  2026 outlook were not retrieved. Check both and change the El Niño line if either has moved.
- Claims about forecasts being "technical and in English" are the app's design premise, not a
  sourced statistic. Phrase it as what the app sets out to change, with no number.

## Shot list

### 1. The problem (0:00 to 0:22)

| Time | On screen | Voice-over | Caption |
|---|---|---|---|
| 0:00 to 0:08 | Slow pan of 2024 flood news imagery that you have a licence to use, or a plain dark card with the text below. Do not use unlicensed press photos. | "In the 2024 long rains, floods across Kenya killed at least 291 people and displaced more than 278,000, according to OCHA." | "2024 floods: 291+ killed, 278,000+ displaced (OCHA, as of 16 May 2024)" |
| 0:08 to 0:15 | Screenshot of a technical forecast bulletin (blur the logo or use one you may show), then a phone with a plain-English and Swahili forecast text. | "Forecasts exist, but they are technical, and mostly in English. Most people need a plain answer: is my area at risk, and what do I do?" | "Forecasts are technical. People need plain answers." |
| 0:15 to 0:22 | Map or chart of the 2026 El Niño outlook; text card "Short rains, Oct to Dec 2026". | "This season, El Niño is expected to bring wetter than normal short rains to much of East Africa. Tahadhari, Swahili for caution, turns open forecast data into flood alerts for all 47 counties." | "El Niño 2026 to 2027: wetter short rains expected (FEWS NET)" |

### 2. Phone walkthrough (0:22 to 1:35)

Record the emulator with the Kenya test data below. Show the real app; never fake a risk level.

| Time | On screen | Voice-over | Caption |
|---|---|---|---|
| 0:22 to 0:30 | Fresh install, first-launch intro, swipe through the three pages, tap the language buttons. | "A short intro explains what the app does, and that it is guidance, not an official warning. Language is a tap away." | "Guidance, not an official warning" |
| 0:30 to 0:40 | County picker. Either tap the location card (suggests the nearest county, you confirm) or type "Tana" in search, tap Tana River. | "Pick your county, or let the app suggest the nearest one from coarse location. You always confirm. Nothing is sent anywhere." | "Pick a county, or confirm a suggestion" |
| 0:40 to 0:55 | County screen: hero risk card with level name, shape mark and meter; reasons ("wettest day 63 mm on Thursday"); explanation card; checklist. Scroll to the 7-day rain chart with threshold lines and the river card. | "The risk level is computed from the forecast with open, published thresholds. It is never colour only: there is a name, a shape and a meter. Then, what it means and what to do, in short sentences. The chart shows the next seven days against the thresholds." | "Risk from rainfall and river forecasts" |
| 0:55 to 1:03 | Tap the EN/SW toggle; the whole screen switches to Kiswahili. | "One tap switches the whole app to Kiswahili." | "English and Kiswahili" |
| 1:03 to 1:13 | Turn on airplane mode, relaunch, open the county: status line "Showing saved forecast" with its date. Then pull to refresh with the network off to show the clear error that never says Low. | "Offline? You see the last saved forecast with its date. If there is nothing usable, the app says so. It never shows Low risk when it does not know." | "Offline: saved forecast with its date" |
| 1:13 to 1:23 | Safety tab: guide list, open "Floods", tick two steps. Mention it works offline. | "Safety guides for floods, landslides, cholera, livestock and lightning work offline, with sources." | "Offline safety guides" |
| 1:23 to 1:29 | Emergency tab: tap 999 (dialer opens), show 1199 Kenya Red Cross, then the county's flood-prone areas. | "One tap dials 999 or the Kenya Red Cross on 1199, and the tab lists flood-prone areas for your county." | "999, 112, Red Cross 1199" |
| 1:29 to 1:35 | Alerts tab, tap "I'm safe" (Niko salama); the share sheet opens with a ready message. Cut to the home screen widget showing the county risk. | "A check-in message tells family you are safe. And a home-screen widget keeps your county's level in view." | "I'm safe check-in and home-screen widget" |

### 3. Dashboard (1:35 to 2:05)

| Time | On screen | Voice-over | Caption |
|---|---|---|---|
| 1:35 to 1:45 | Desktop dashboard launches; all 47 counties load; coloured Kenya map beside the ranked list. | "For county disaster teams, Red Cross volunteers and NGOs, the desktop dashboard loads all 47 counties and ranks them by risk on a map." | "Desktop dashboard: 47 counties, ranked" |
| 1:45 to 1:55 | Click the High filter chip, hover a county on the map, select a row; detail pane opens beside the list. | "Filter to High and Severe, hover the map, and see the same plain-language detail side by side." | "Filter, hover, compare" |
| 1:55 to 2:05 | Header menu: export CSV and the HTML briefing; open the briefing in a browser. Show auto-refresh toggle. | "Export a CSV or a printable briefing for the morning meeting. Data refreshes on its own without blanking the screen." | "CSV and briefing export" |

### 4. Validation (2:05 to 2:20)

Numbers from `docs/validation.md` (back-test of the March-May 2024 floods, ECMWF forecast). Say them
exactly as written; do not round up.

| Time | On screen | Voice-over | Caption |
|---|---|---|---|
| 2:05 to 2:20 | The results-per-event table from `docs/validation.md` section 2, with the Tana River and Garissa rows highlighted. | "I back-tested the real risk engine on the 2024 floods. With free forecasts it reached High for two of seven documented events, both through the river signal. That is why it says guidance, not warning." | "Back-test, Mar-May 2024: 2 of 7 events reached High" |

### 5. Kotlin Multiplatform (2:20 to 2:42)

| Time | On screen | Voice-over | Caption |
|---|---|---|---|
| 2:20 to 2:42 | Split screen: Android emulator and desktop window running the same screen. Then a simple diagram of modules: `commonMain` (domain, data, UI) shared by Android and desktop; `shared` AI contract used by the app and the Ktor server; the web target (Kotlin/Wasm), shown running in a browser at stmakori.github.io/tahadhari. | "One Kotlin codebase. The risk engine, data layer and Compose Multiplatform UI are shared by Android and desktop. The AI contract and prompt live in a shared module used by both the app and a small Ktor server, so the API key never ships in the app. AI text is advisory only, and always falls back to an offline template. The same code also runs in the browser." | "Kotlin Multiplatform: Android, desktop, web, Ktor server" |

### 6. Close (2:42 to 2:55)

| Time | On screen | Voice-over | Caption |
|---|---|---|---|
| 2:42 to 2:55 | The disclaimer text full screen, then GitHub URL and the APK / installer download. | "Tahadhari explains forecasts. It is not an official warning: follow the Kenya Meteorological Service and your county advisories. It is open source under the MIT licence. Try it, and share it with someone who needs it." | "Not an official warning. Follow KMD and county advisories." / "github.com/stmakori/tahadhari" |

Total about 2:55. If over 3:00, shorten section 3 first.

## Recording tips

Test data (Kenya-like, so the footage is realistic and repeatable)
- Use live data only if the forecast is really showing a notable level that day. Otherwise run the
  phone UI with fixed sample data. The screenshot tests already contain such data: Tana River with
  daily rainfall 4, 12, 28, 63, 41, 9, 0.5 mm and a river outlook of 340 baseline vs 812 peak gives a
  High or Severe card. Never edit a risk level by hand; change the rainfall input instead.
- For Swahili footage, have a fluent speaker review the on-screen text first.
- Pick counties that tell a story: Tana River and Garissa (Tana basin), Kisumu and Busia (Lake
  Victoria, Nzoia and Nyando basins), Nairobi (urban flooding).

Phone
- Android Studio emulator, Pixel 6 or 7 image with Google APIs, API 34. Set the emulator to 1080 x
  2400, demo mode on (clean status bar): `adb shell settings put global sysui_demo_allowed 1`, then
  the `am broadcast -a com.android.systemui.demo` commands for clock 1000, full battery and signal.
- Record with the emulator toolbar "Screen record" button (up to 3 minutes per clip, saves as
  WebM/MP4), or `adb shell screenrecord --size 1080x2400 /sdcard/clip.mp4` then `adb pull`.
  Record each block as its own clip; it is easier to retake one screen than the whole video.
- Show taps: Developer options, "Show taps". Keep gestures slow; hold each screen about 1.5 s
  before moving.
- Toggle airplane mode from the emulator's extended controls (three dots, Settings or Cellular).
- Widget: long-press the home screen, Widgets, Tahadhari. It refreshes every 3 hours; add it after
  choosing a county.

Desktop
- OBS Studio, Display Capture at 1920 x 1080, 30 fps, MP4. Run the app with
  `./gradlew :composeApp:run` (or the installed package) at a window size that fills the screen.
  Close notifications and use a clean desktop.
- To record the phone UI on desktop: `./gradlew :composeApp:run --args="--mobile"`.

Voice-over and editing
- Record the VO separately in a quiet room (Audacity or OBS audio source), then edit to picture in
  any editor (DaVinci Resolve is free). Leave about 0.3 s of silence between sentences.
- Captions: burn them in (required for sound-off viewers) and also upload an .srt to YouTube.
  Large, high-contrast text in the lower third, never over the risk card.
- Add alt text or a transcript when you publish. Keep the file under the contest's upload limits
  and check the length is within 3:00 before submitting.
- Do not use music you do not have a licence for. Do not show real people's faces or phone numbers
  other than the public emergency lines.
