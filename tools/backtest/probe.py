"""One-off probe: find GloFAS cells with real Tana River discharge near Garissa and Garsen/Hola.
Results are cached under build/backtest-cache. Writes tools/backtest/data/flood_alt.json."""
import json, os, urllib.request, time

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
CACHE = os.path.join(ROOT, "build", "backtest-cache")
DATA = os.path.join(ROOT, "tools", "backtest", "data")


def fetch(name, lats, lons, start, end):
    path = os.path.join(CACHE, name + ".json")
    if not os.path.exists(path):
        u = ("https://flood-api.open-meteo.com/v1/flood?latitude=%s&longitude=%s&daily=river_discharge&start_date=%s&end_date=%s"
             % (",".join(map(str, lats)), ",".join(map(str, lons)), start, end))
        for attempt in range(5):
            try:
                body = urllib.request.urlopen(u, timeout=60).read()
                break
            except Exception as e:
                print("retry", e); time.sleep(10)
        open(path, "wb").write(body)
        time.sleep(1)
    d = json.load(open(path))
    return d if isinstance(d, list) else [d]


def grid(c_lat, c_lon, n=1):
    return [(round(c_lat + dy * 0.05, 4), round(c_lon + dx * 0.05, 4)) for dy in range(-n, n + 1) for dx in range(-n, n + 1)]


out = {}
for name, centre, n in (("garissa", (-0.45, 39.65), 2), ("garsen", (-2.27, 40.12), 1)):
    pts = grid(*centre, n)
    res = fetch("probe_" + name + str(n), [p[0] for p in pts], [p[1] for p in pts], "2024-05-10", "2024-05-10")
    print(name)
    for p, r in zip(pts, res):
        print("  ", p, "-> grid", round(r["latitude"], 3), round(r["longitude"], 3), r["daily"]["river_discharge"])
    best = max(zip(pts, res), key=lambda t: t[1]["daily"]["river_discharge"][0] or 0)
    out[name] = (best[0], best[1]["latitude"], best[1]["longitude"])
print(out)
# Fetch the full 2024 series for the best cell of each
alt = {}
for name, (pt, glat, glon) in out.items():
    r = fetch("alt_" + name, [pt[0]], [pt[1]], "2024-02-10", "2024-06-07")[0]
    alt[name] = {"lat": pt[0], "lon": pt[1], "grid_lat": r["latitude"], "grid_lon": r["longitude"],
                 "daily": dict(zip(r["daily"]["time"], r["daily"]["river_discharge"]))}
json.dump({"note": "GloFAS discharge at best-guess main-stem Tana cells (probe), m3/s", "flood": alt},
          open(os.path.join(DATA, "flood_alt.json"), "w"), separators=(",", ":"))
