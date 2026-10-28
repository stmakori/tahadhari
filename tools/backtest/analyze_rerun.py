"""Before/after of the river-point fix and the 5 m3/s baseline floor (run after BacktestRunnerTest).

  python -I tools/backtest/analyze_rerun.py

Three variants, all on the 47 county HQs, issue days 15 Mar - 31 May 2024, default thresholds:
  before  = old river cells (data/flood_before_fix.json), no baseline floor, no landslide signal.
            This reproduces the first back-test, so it is a check on this script as well.
  fixed   = current river cells (data/flood.json) + floor, no landslide signal. Isolates the river fix.
  engine  = the engine as it is now (results.csv): fixed + landslide signal.
"before" and "fixed" are assembled here as max(rain-only level, river level) from the same mm data and
the same thresholds; the script checks that this rebuild equals the engine's own output (results.csv)
for the "engine" variant before trusting it. Levels are only ever the engine's thresholds applied to data.
"""
import collections, csv, datetime, json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
DATA = os.path.join(HERE, "data")
NAMES = ["Low", "Moderate", "High", "Severe"]
SOURCES = ["ecmwf_ifs025", "gfs_seamless", "era5"]
HQ_EVENT = {"E1": ("Nairobi", "c47", "2024-04-24"), "E2": ("Kajiado", "c34", "2024-04-24"), "E3": ("Nakuru", "c32", "2024-04-29"),
            "E4": ("Kajiado", "c34", "2024-04-30"), "E5": ("Kisumu", "c42", "2024-05-05"), "E6": ("Garissa", "c7", "2024-05-06"),
            "E7": ("Tana River", "c4", "2024-05-11")}
RIVER_LOCS = ["c4", "c7", "c33", "c40", "c41", "c42"]


def add_days(s, k):
    return (datetime.date.fromisoformat(s) + datetime.timedelta(days=k)).isoformat()


def rain_level(w, m3):
    a = 3 if w >= 80 else 2 if w >= 50 else 1 if w >= 20 else 0
    b = 3 if m3 >= 120 else 2 if m3 >= 80 else 1 if m3 >= 40 else 0
    return max(a, b)


def river_level(series, issue, floor):
    base = [series[add_days(issue, -j)] for j in range(1, 31)]
    peak = max(series[add_days(issue, j)] for j in range(0, 7))
    b = sum(base) / 30
    if b < floor or b <= 0:
        return 0, peak / b if b > 0 else float("inf")
    r = peak / b
    return (3 if r >= 3 else 2 if r >= 2 else 0), r


rows = list(csv.DictReader(open(os.path.join(ROOT, "composeApp", "build", "backtest", "results.csv"))))
rows = [r for r in rows if r["loc"].startswith("c")]
new = {k: v["daily"] for k, v in json.load(open(os.path.join(DATA, "flood.json")))["flood"].items()}
old = {k: v["daily"] for k, v in json.load(open(os.path.join(DATA, "flood_before_fix.json")))["flood"].items()}

lv = {"before": {}, "fixed": {}, "engine": {}}
mism = collections.Counter()
ls_days = collections.Counter()
for r in rows:
    key = (r["source"], r["loc"], r["issue"])
    rl = rain_level(float(r["wettest_mm"]), float(r["max3_mm"]))
    eng_rain = int(r["default_rainonly"])
    if rl != eng_rain:
        ls_days[r["county"]] += 1  # engine rain-only is higher: landslide signal
    rv_new = rv_old = 0
    if r["loc"] in new:
        rv_new, ratio = river_level(new[r["loc"]], r["issue"], 5.0)
        assert not r["river_ratio"] or abs(float(r["river_ratio"]) - ratio) < 0.011, (key, r["river_ratio"], ratio)
        rv_old, _ = river_level(old[r["loc"]], r["issue"], 0.0)
    lv["before"][key] = max(rl, rv_old)
    lv["fixed"][key] = max(rl, rv_new)
    lv["engine"][key] = int(r["default_level"])
    if max(eng_rain, rv_new) != int(r["default_level"]):
        mism[r["county"]] += 1
print("rebuild check: engine level != max(engine rain-only, rebuilt river level) on", sum(mism.values()), "county-days")
print("county-days where the landslide signal raised the rain-only level:", sum(ls_days.values()), dict(ls_days))

print("\n## Old vs new river cells (mean discharge 10 Feb - 7 Jun 2024, m3/s)\n")
for k in RIVER_LOCS:
    mo = sum(v for v in old[k].values() if v is not None) / len([v for v in old[k].values() if v is not None])
    mn = sum(v for v in new[k].values() if v is not None) / len([v for v in new[k].values() if v is not None])
    print(f"{k}: old {mo:.1f} -> new {mn:.1f}")

print("\n## High+ and Moderate+ share of county-days (47 HQs x 78 issue days)\n")
print("| Variant | Input | Moderate+ | High+ | Severe |")
print("|---|---|---|---|---|")
share = {}
for vn in lv:
    for s in SOURCES:
        vals = [v for (src, loc, i), v in lv[vn].items() if src == s]
        n = len(vals)
        share[(vn, s)] = (100 * sum(v >= 1 for v in vals) / n, 100 * sum(v >= 2 for v in vals) / n, 100 * sum(v >= 3 for v in vals) / n, sum(v >= 2 for v in vals), n)
        a = share[(vn, s)]
        print(f"| {vn} | {s} | {a[0]:.1f} % | {a[1]:.1f} % ({a[3]} of {n}) | {a[2]:.1f} % |")

print("\n## High+ county-days added only by the river signal (rain-only level < High, level >= High)\n")
for vn in ("before", "fixed", "engine"):
    for s in SOURCES:
        tot = riveronly = 0
        for r in rows:
            if r["source"] != s:
                continue
            key = (s, r["loc"], r["issue"])
            if lv[vn][key] >= 2:
                tot += 1
                rl = int(r["default_rainonly"]) if vn == "engine" else rain_level(float(r["wettest_mm"]), float(r["max3_mm"]))
                riveronly += rl < 2
        print(f"{vn} {s}: High+ {tot}, river-only {riveronly}")

print("\n## River counties: High+ county-days by county (fixed variant = river fix without landslide)\n")
for vn in ("before", "fixed"):
    for s in SOURCES[:1]:
        c = collections.Counter()
        for (src, loc, i), v in lv[vn].items():
            if src == s and loc in RIVER_LOCS and v >= 2:
                c[loc] += 1
        print(vn, s, dict(c))


def event_eval(vn, s, loc, e):
    pre = [lv[vn].get((s, loc, add_days(e, -k))) for k in (1, 2, 3)]
    pre = [p for p in pre if p is not None]
    lead = None
    for k in range(6, -1, -1):
        l = lv[vn].get((s, loc, add_days(e, -k)))
        if l is not None and l >= 2:
            lead = k
            break
    return max(pre), lead


print("\n## Per-event (HQ), max level 1-3 days before / lead of first High+ (days)\n")
print("| ID | County | Input | before | fixed | engine now |")
print("|---|---|---|---|---|---|")
hits = collections.defaultdict(lambda: [0, 0])
for eid, (cty, loc, e) in HQ_EVENT.items():
    for s in SOURCES:
        cells = []
        for vn in ("before", "fixed", "engine"):
            mx, lead = event_eval(vn, s, loc, e)
            cells.append(f"{NAMES[mx]} / {'-' if lead is None else lead}")
            hits[(vn, s)][0] += mx >= 1
            hits[(vn, s)][1] += mx >= 2
        print(f"| {eid} | {cty} | {s} | " + " | ".join(cells) + " |")
print("\n## Hits over 7 events (Moderate+ / High+)\n")
for s in SOURCES:
    print(s, {vn: tuple(hits[(vn, s)]) for vn in lv})

print("\n## Chance baseline: expected High+ / Moderate+ hits over 7 events (random issue day, same 3-day window rule)\n")
for vn in lv:
    for s in SOURCES:
        eh = em = 0.0
        for eid, (cty, loc, e) in HQ_EVENT.items():
            days = sorted(d for (src, l, d) in lv[vn] if src == s and l == loc and "2024-03-18" <= d <= "2024-05-31")
            hh = mm = 0
            for x in days:
                m = max(lv[vn][(s, loc, add_days(x, -k))] for k in (1, 2, 3))
                hh += m >= 2
                mm += m >= 1
            eh += hh / len(days)
            em += mm / len(days)
        print(f"{vn:7s} {s:13s} High+ {eh:.1f}  Moderate+ {em:.1f}")

out = os.path.join(HERE, "out", "rerun_levels.csv")
with open(out, "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["variant", "source", "moderate_pct", "high_pct", "severe_pct", "high_plus_days", "n"])
    for (vn, s), a in share.items():
        w.writerow([vn, s, round(a[0], 2), round(a[1], 2), round(a[2], 2), a[3], a[4]])
