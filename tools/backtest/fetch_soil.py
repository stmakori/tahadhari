"""Fetch historical soil moisture for the 'saturated ground' back-test (results are committed).

  python -I tools/backtest/fetch_soil.py        # about 20 requests; raw responses cached in build/backtest-cache/

Writes tools/backtest/data/soil.json: daily mean volumetric soil moisture (m3/m3) per rain point
(the same 47 county HQs + 3 event sites as the rain data), for issue days 15 Mar - 31 May 2024.

Two series, both Open-Meteo (CC BY 4.0):
  * ifs025 : historical-forecast API, models=ecmwf_ifs025, soil_moisture_9_to_27cm. Same variable and
             layer as the app's live forecast, but the 0.25 deg IFS, not the 9 km IFS the live app gets in Kenya.
  * era5   : archive API (ERA5 / ERA5-Land), soil_moisture_7_to_28cm. A reanalysis, a different layer depth.
Note: the historical-forecast API's default best_match returned values identical to ERA5 for these dates, so it
is not used as a third, independent series.
"""
import json, os, time, urllib.request, urllib.error

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
CACHE = os.path.join(ROOT, "build", "backtest-cache")
DATA = os.path.join(ROOT, "tools", "backtest", "data")
os.makedirs(CACHE, exist_ok=True)
START, END = "2024-03-15", "2024-05-31"
BATCH = 5
SERIES = {
    "ifs025": ("https://historical-forecast-api.open-meteo.com/v1/forecast", "soil_moisture_9_to_27cm", "&models=ecmwf_ifs025"),
    "era5": ("https://archive-api.open-meteo.com/v1/archive", "soil_moisture_7_to_28cm", ""),
}
calls = 0


def get(url, key):
    global calls
    path = os.path.join(CACHE, key + ".json")
    if os.path.exists(path):
        return json.load(open(path))
    for attempt in range(6):
        try:
            with urllib.request.urlopen(url, timeout=90) as r:
                body = r.read()
            calls += 1
            open(path, "wb").write(body)
            time.sleep(1.0)
            return json.loads(body)
        except urllib.error.HTTPError as e:
            if e.code == 429:
                print("429, waiting 65s"); time.sleep(65); continue
            raise
        except (urllib.error.URLError, ConnectionError, TimeoutError) as e:
            print("network error, retrying:", e); time.sleep(5 * (attempt + 1))
    raise RuntimeError("too many retries " + url)


def daily_mean(times, values):
    out = {}
    for t, v in zip(times, values):
        out.setdefault(t[:10], []).append(v)
    return {d: (None if len(vs) < 24 or any(x is None for x in vs) else round(sum(vs) / 24, 4)) for d, vs in out.items()}


def main():
    locs = json.load(open(os.path.join(DATA, "forecast_ecmwf_ifs025.json")))["locations"]
    out = {k: {} for k in SERIES}
    grid = {k: {} for k in SERIES}
    for name, (base, var, extra) in SERIES.items():
        for i in range(0, len(locs), BATCH):
            b = locs[i:i + BATCH]
            lat = ",".join(str(x["lat"]) for x in b); lon = ",".join(str(x["lon"]) for x in b)
            u = "%s?latitude=%s&longitude=%s&hourly=%s%s&timezone=Africa/Nairobi&start_date=%s&end_date=%s" % (base, lat, lon, var, extra, START, END)
            resp = get(u, f"soil_{name}_{i}")
            resp = resp if isinstance(resp, list) else [resp]
            for loc, r in zip(b, resp):
                h = r["hourly"]
                out[name][loc["id"]] = daily_mean(h["time"], h[var])
                grid[name][loc["id"]] = [round(r["latitude"], 3), round(r["longitude"], 3)]
        print(name, "done; calls so far", calls)
    json.dump({"note": "daily mean soil moisture m3/m3. ifs025 = historical-forecast ecmwf_ifs025 soil_moisture_9_to_27cm; "
                       "era5 = archive soil_moisture_7_to_28cm. Issue days 2024-03-15..2024-05-31.",
               "grid": grid, "soil": out}, open(os.path.join(DATA, "soil.json"), "w"), separators=(",", ":"))
    print("done; network calls this run:", calls)


main()
