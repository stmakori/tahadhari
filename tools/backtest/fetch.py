"""Fetch data for the March-May 2024 backtest (run once; results are committed).

Raw API responses are cached under build/backtest-cache/ (git-ignored). A compact daily
summary is written to tools/backtest/data/ (committed) and read by the Kotlin backtest.

  python -I tools/backtest/fetch.py               # everything (about 50 requests)
  python -I tools/backtest/fetch.py --river-only  # only the GloFAS river series, re-read from Counties.kt (about 6 requests)

Sources (all Open-Meteo, CC BY 4.0):
  * previous-runs-api  : hourly precipitation as forecast 0..6 days earlier (explicit models: ECMWF IFS 0.25 and GFS)
  * archive-api        : ERA5 reanalysis daily precipitation ("observed" proxy)
  * flood-api          : GloFAS river discharge, historical dates
"""
import json, os, re, sys, time, urllib.request, urllib.error

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
CACHE = os.path.join(ROOT, "build", "backtest-cache")
DATA = os.path.join(ROOT, "tools", "backtest", "data")
os.makedirs(CACHE, exist_ok=True)
os.makedirs(DATA, exist_ok=True)

RAIN_START, RAIN_END = "2024-03-15", "2024-06-06"   # valid dates (issue days to 05-31, +6 lead)
RIVER_START, RIVER_END = "2024-02-10", "2024-06-07"
LEADS = range(0, 7)
BATCH = 5
MODELS = ["ecmwf_ifs025", "gfs_seamless"]
calls = 0


def get(url, key):
    global calls
    path = os.path.join(CACHE, key + ".json")
    if os.path.exists(path):
        return json.load(open(path))
    for attempt in range(6):
        try:
            with urllib.request.urlopen(url, timeout=60) as r:
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


def load_locations():
    src = open(os.path.join(ROOT, "composeApp/src/commonMain/kotlin/ke/tahadhari/data/Counties.kt"), encoding="utf-8").read()
    locs, rivers = [], []
    for m in re.finditer(r'c\((\d+), "([^"]+)", "[^"]+", (-?[\d.]+), (-?[\d.]+)(?:, "([^"]+)", (-?[\d.]+), (-?[\d.]+))?', src):
        code, name, lat, lon, rname, rlat, rlon = m.groups()
        locs.append({"id": f"c{code}", "name": name, "lat": float(lat), "lon": float(lon), "county": name})
        if rname:
            rivers.append({"id": f"c{code}", "name": name, "river": rname, "lat": float(rlat), "lon": float(rlon)})
    assert len(locs) == 47, len(locs)
    # Event sites that are not county HQs (used to test how much the HQ-point choice matters).
    locs += [
        {"id": "x_maimahiu", "name": "Mai Mahiu (Nakuru)", "lat": -0.9833, "lon": 36.5833, "county": "Nakuru"},
        {"id": "x_kitengela", "name": "Kitengela (Kajiado)", "lat": -1.4742, "lon": 36.9625, "county": "Kajiado"},
        {"id": "x_budalangi", "name": "Budalang'i (Busia)", "lat": 0.1353, "lon": 34.0217, "county": "Busia"},
    ]
    return locs, rivers


def daily_from_hourly(times, values):
    out = {}
    for t, v in zip(times, values):
        d = t[:10]
        out.setdefault(d, []).append(v)
    # a day with any missing hour is None (do not silently under-count)
    return {d: (None if any(x is None for x in vs) or len(vs) < 24 else round(sum(vs), 2)) for d, vs in out.items()}


def fetch_river(rivers):
    flood = {}
    for rv in rivers:
        u = ("https://flood-api.open-meteo.com/v1/flood?latitude=%s&longitude=%s&daily=river_discharge&start_date=%s&end_date=%s"
             % (rv["lat"], rv["lon"], RIVER_START, RIVER_END))
        # the cache key includes the coordinates so a moved river point is never served a stale cell
        r = get(u, f"flood_{rv['id']}_{rv['lat']}_{rv['lon']}")
        flood[rv["id"]] = {"river": rv["river"], "lat": rv["lat"], "lon": rv["lon"], "grid_lat": r["latitude"], "grid_lon": r["longitude"],
                           "daily": dict(zip(r["daily"]["time"], r["daily"]["river_discharge"]))}
    json.dump({"note": "GloFAS river discharge m3/s (historical)", "flood": flood}, open(os.path.join(DATA, "flood.json"), "w"), separators=(",", ":"))
    return flood


def main():
    locs, rivers = load_locations()
    if "--river-only" in sys.argv:
        fetch_river(rivers)
        print("river series written; network calls this run:", calls)
        return
    hourly_vars = ["precipitation"] + [f"precipitation_previous_day{k}" for k in LEADS if k > 0]
    forecast, era5 = {m: {} for m in MODELS}, {}
    for i in range(0, len(locs), BATCH):
        b = locs[i:i + BATCH]
        lat = ",".join(str(x["lat"]) for x in b); lon = ",".join(str(x["lon"]) for x in b)
        for model in MODELS:
            u = ("https://previous-runs-api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&hourly=%s&models=%s"
                 "&timezone=Africa/Nairobi&start_date=%s&end_date=%s" % (lat, lon, ",".join(hourly_vars), model, RAIN_START, RAIN_END))
            resp = get(u, f"prev_{model}_{i}")
            resp = resp if isinstance(resp, list) else [resp]
            for loc, r in zip(b, resp):
                h = r["hourly"]
                forecast[model][loc["id"]] = {str(k): daily_from_hourly(h["time"], h["precipitation" if k == 0 else f"precipitation_previous_day{k}"]) for k in LEADS}
        u = ("https://archive-api.open-meteo.com/v1/archive?latitude=%s&longitude=%s&daily=precipitation_sum"
             "&timezone=Africa/Nairobi&start_date=%s&end_date=%s" % (lat, lon, RAIN_START, RAIN_END))
        resp = get(u, f"era5_{i}")
        resp = resp if isinstance(resp, list) else [resp]
        for loc, r in zip(b, resp):
            era5[loc["id"]] = dict(zip(r["daily"]["time"], r["daily"]["precipitation_sum"]))
        print("batch", i, "calls so far", calls)

    fetch_river(rivers)

    for model in MODELS:
        json.dump({"note": "Open-Meteo previous-runs, model %s: daily mm by lead day; lead k = forecast issued ~k days earlier" % model,
                   "locations": locs, "forecast": forecast[model]}, open(os.path.join(DATA, "forecast_%s.json" % model), "w"), separators=(",", ":"))
    json.dump({"note": "ERA5 daily precipitation mm", "era5": era5}, open(os.path.join(DATA, "era5.json"), "w"), separators=(",", ":"))
    print("done; network calls this run:", calls)


main()
