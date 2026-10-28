"""Turn the engine output of BacktestRunnerTest into the numbers and chart inputs for docs/validation.md.

  TAHADHARI_BACKTEST=1 ./gradlew :composeApp:desktopTest --tests "ke.tahadhari.BacktestRunnerTest"
  python -I tools/backtest/analyze.py        # prints markdown tables, writes tools/backtest/out/*.csv
  java tools/backtest/Charts.java            # writes docs/validation/*.png

Standard library only.
"""
import csv, collections, datetime, json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RESULTS = os.path.join(ROOT, "composeApp", "build", "backtest", "results.csv")
DATA = os.path.join(HERE, "data")
OUT = os.path.join(HERE, "out")
os.makedirs(OUT, exist_ok=True)
NAMES = ["Low", "Moderate", "High", "Severe"]
SOURCES = ["ecmwf_ifs025", "gfs_seamless", "era5"]
SRC_LABEL = {"ecmwf_ifs025": "ECMWF IFS forecast", "gfs_seamless": "GFS forecast", "era5": "ERA5 (perfect-forecast bound)"}

# Documented impacts. date = day the flooding was reported / began (see docs/validation.md for sources).
# sites = rainfall points to evaluate: the county HQ the app really uses, plus the event site where different.
EVENTS = [
    dict(id="E1", name="Nairobi flash floods", county="Nairobi", date="2024-04-24", sites=["c47"]),
    dict(id="E2", name="Athi River bursts banks", county="Kajiado", date="2024-04-24", sites=["c34", "x_kitengela"]),
    dict(id="E3", name="Mai Mahiu / Old Kijabe dam burst", county="Nakuru", date="2024-04-29", sites=["c32", "x_maimahiu"]),
    dict(id="E4", name="Kitengela flooding", county="Kajiado", date="2024-04-30", sites=["c34", "x_kitengela"]),
    dict(id="E5", name="Nyando River bursts (Ahero)", county="Kisumu", date="2024-05-05", sites=["c42"]),
    dict(id="E6", name="River Tana floods Garissa town", county="Garissa", date="2024-05-06", sites=["c7"]),
    dict(id="E7", name="River Tana overflows (Hola, Tana Delta)", county="Tana River", date="2024-05-11", sites=["c4"]),
]
UPSTREAM = ["c19", "c21", "c14", "c12", "c20"]  # Nyeri, Murang'a, Embu, Meru, Kirinyaga: Tana headwaters
UPSTREAM_EVENTS = {"E6", "E7"}
SCENARIOS = ["default", "rain_lower", "rain_lower_river_x3"]
RIVER_LOCS = ["c4", "c7", "c33", "c40", "c41", "c42"]


def add_days(s, k):
    return (datetime.date.fromisoformat(s) + datetime.timedelta(days=k)).isoformat()


rows = list(csv.DictReader(open(RESULTS)))
idx = {(r["source"], r["loc"], r["issue"]): r for r in rows}
loc_name = {r["loc"]: r["county"] for r in rows}


def level(source, loc, issue, scenario="default", rain_only=False):
    r = idx.get((source, loc, issue))
    if r is None:
        return None
    return int(r[scenario + ("_rainonly" if rain_only else "_level")])


def event_eval(ev, source, loc, scenario="default"):
    """Max level for issue days E-3..E-1, and lead time (days) of the first High+ issued up to 6 days before E."""
    e = ev["date"]
    pre = [level(source, loc, add_days(e, -k), scenario) for k in (1, 2, 3)]
    pre = [p for p in pre if p is not None]
    first_lead = None
    for k in range(6, -1, -1):  # earliest issue day first
        l = level(source, loc, add_days(e, -k), scenario)
        if l is not None and l >= 2:
            first_lead = k
            break
    return max(pre), first_lead


def md_table(header, body):
    out = ["| " + " | ".join(header) + " |", "|" + "|".join("---" for _ in header) + "|"]
    out += ["| " + " | ".join(str(c) for c in r) + " |" for r in body]
    return "\n".join(out)


print("## Per-event results (default thresholds)\n")
print("Cell format: max level over forecasts issued 1-3 days before / earliest High+ lead time (days before event; - = none in the 7 days up to the event)\n")
body, chart = [], []
tally = collections.defaultdict(lambda: [0, 0, 0])  # (scenario, source) -> [n, >=moderate, >=high]
for ev in EVENTS:
    for loc in ev["sites"]:
        cells = []
        for s in SOURCES:
            mx, lead = event_eval(ev, s, loc)
            cells.append(f"{NAMES[mx]} / {'-' if lead is None else lead}")
            chart.append([ev["id"], loc, s] + [level(s, loc, add_days(ev["date"], -k)) for k in range(6, -1, -1)])
        site = "county HQ" if loc.startswith("c") else "event site"
        body.append([ev["id"], ev["name"], ev["date"], f"{ev['county']} ({site}: {loc_name[loc]})"] + cells)
print(md_table(["ID", "Event", "Date", "Evaluated at"] + [SRC_LABEL[s] for s in SOURCES], body))
with open(os.path.join(OUT, "event_levels.csv"), "w", newline="") as f:
    csv.writer(f).writerows([["event", "loc", "source"] + [f"lead{k}" for k in range(6, -1, -1)]] + chart)

# Hit rates use only the county-HQ evaluation (that is what the app shows to a user), one per event county.
print("\n## Hit rates over the 7 events, county-HQ point (what the app uses)\n")
hq_event = {"E1": "c47", "E2": "c34", "E3": "c32", "E4": "c34", "E5": "c42", "E6": "c7", "E7": "c4"}
body = []
hits = {}
for sc in SCENARIOS:
    for s in SOURCES:
        m = h = 0
        for ev in EVENTS:
            mx, _ = event_eval(ev, s, hq_event[ev["id"]], sc)
            m += mx >= 1; h += mx >= 2
        hits[(sc, s)] = (m, h)
        body.append([sc, SRC_LABEL[s], f"{m}/7", f"{h}/7"])
print(md_table(["Thresholds", "Rainfall input", "Moderate or higher", "High or higher"], body))
print("\nEvent-site points (Kitengela, Mai Mahiu) where different from HQ:")
body = []
site_event = {"E2": "x_kitengela", "E3": "x_maimahiu", "E4": "x_kitengela"}
for sc in SCENARIOS:
    for s in SOURCES:
        m = h = 0
        for ev in EVENTS:
            loc = site_event.get(ev["id"], hq_event[ev["id"]])
            mx, _ = event_eval(ev, s, loc, sc)
            m += mx >= 1; h += mx >= 2
        body.append([sc, SRC_LABEL[s], f"{m}/7", f"{h}/7"])
print(md_table(["Thresholds", "Rainfall input", "Moderate or higher", "High or higher"], body))

print("\n## Upstream check for the Tana events (highest level in any of Nyeri, Murang'a, Embu, Meru, Kirinyaga, issues E-7..E)\n")
body = []
for ev in EVENTS:
    if ev["id"] not in UPSTREAM_EVENTS:
        continue
    cells = []
    for s in SOURCES:
        best = (-1, "")
        for loc in UPSTREAM:
            for k in range(0, 8):
                l = level(s, loc, add_days(ev["date"], -k))
                if l is not None and l > best[0]:
                    best = (l, loc_name[loc])
        cells.append(f"{NAMES[best[0]]} ({best[1]})")
    body.append([ev["id"], ev["name"]] + cells)
print(md_table(["ID", "Event"] + [SRC_LABEL[s] for s in SOURCES], body))

print("\n## Level distribution over all county-days (issue days 15 Mar - 31 May, 50 rain points = 47 HQs + 3 event sites; 47 HQs only below)\n")
hq = lambda r: r["loc"].startswith("c")
dist = []
for sc in SCENARIOS:
    for s in SOURCES:
        c = collections.Counter(int(r[sc + "_level"]) for r in rows if r["source"] == s and hq(r))
        n = sum(c.values())
        dist.append([sc, s] + [c[i] for i in range(4)])
        print(f"{sc:22s} {s:13s} n={n} " + " ".join(f"{NAMES[i]}={c[i]} ({100*c[i]/n:.1f}%)" for i in range(4)))
with open(os.path.join(OUT, "level_distribution.csv"), "w", newline="") as f:
    csv.writer(f).writerows([["scenario", "source", "low", "moderate", "high", "severe"]] + dist)

print("\n## High/Severe county-days by signal (default thresholds, 47 HQs)\n")
for s in SOURCES:
    both = rainonly = riveronly = 0
    for r in rows:
        if r["source"] != s or not hq(r):
            continue
        full, ro = int(r["default_level"]), int(r["default_rainonly"])
        if full >= 2:
            both += 1
            if ro >= 2:
                rainonly += 1
            else:
                riveronly += 1
    print(f"{s}: High+ county-days {both}; from rain signals {rainonly}; added only by the river signal {riveronly}")
print("\nRiver counties: share of issue days with river ratio >= 2 (High) / >= 3 (Severe), and ratio range")
for loc in RIVER_LOCS:
    rs = [r for r in rows if r["source"] == "era5" and r["loc"] == loc]
    ratios = [float(r["river_ratio"]) for r in rs]
    print(f"{loc} {loc_name[loc]:11s} >=2x: {100*sum(x>=2 for x in ratios)/len(ratios):5.1f}%  >=3x: {100*sum(x>=3 for x in ratios)/len(ratios):5.1f}%  min {min(ratios):.2f} max {max(ratios):.2f}")

print("\n## High+ county-days not within +-3 days of a documented event in that county (upper bound on false alarms; list of events is incomplete)\n")
ev_days = collections.defaultdict(set)
for ev in EVENTS:
    for k in range(-3, 4):
        ev_days[ev["county"]].add(add_days(ev["date"], k))
for s in SOURCES:
    for sc in ("default", "rain_lower"):
        tot = unmatched = 0
        per = collections.Counter()
        for r in rows:
            if r["source"] != s or not hq(r) or int(r[sc + "_level"]) < 2:
                continue
            tot += 1
            if r["issue"] not in ev_days.get(r["county"], ()):
                unmatched += 1
                per[r["county"]] += 1
        print(f"{s} {sc}: High+ {tot}, unmatched {unmatched}; top counties {per.most_common(6)}")

print("\n## Forecast skill vs ERA5, 47 HQ points, daily rainfall\n")
era5 = json.load(open(os.path.join(DATA, "era5.json")))["era5"]
skill_rows = []
for model in ["ecmwf_ifs025", "gfs_seamless"]:
    fc = json.load(open(os.path.join(DATA, f"forecast_{model}.json")))["forecast"]
    for k in range(0, 7):
        stats = {}
        for thr in (20.0, 50.0):
            hit = miss = fa = 0
            for loc, byl in fc.items():
                if not loc.startswith("c"):
                    continue
                for d, v in byl[str(k)].items():
                    o = era5[loc].get(d)
                    if o is None or v is None:
                        continue
                    if o >= thr and v >= thr: hit += 1
                    elif o >= thr: miss += 1
                    elif v >= thr: fa += 1
            pod = hit / (hit + miss) if hit + miss else float("nan")
            far = fa / (hit + fa) if hit + fa else float("nan")
            stats[thr] = (hit + miss, pod, far)
        # correlation and mean bias
        xs, ys = [], []
        for loc, byl in fc.items():
            if not loc.startswith("c"):
                continue
            for d, v in byl[str(k)].items():
                o = era5[loc].get(d)
                if o is not None and v is not None:
                    xs.append(v); ys.append(o)
        mx, my = sum(xs) / len(xs), sum(ys) / len(ys)
        cov = sum((a - mx) * (b - my) for a, b in zip(xs, ys))
        r = cov / (sum((a - mx) ** 2 for a in xs) * sum((b - my) ** 2 for b in ys)) ** 0.5
        skill_rows.append([model, k, stats[20.0][0], round(stats[20.0][1], 3), round(stats[20.0][2], 3),
                           stats[50.0][0], round(stats[50.0][1], 3), round(stats[50.0][2], 3), round(r, 3), round(mx, 2), round(my, 2)])
print(md_table(["model", "lead (days)", "ERA5 >=20mm days", "POD>=20", "FAR>=20", "ERA5 >=50mm days", "POD>=50", "FAR>=50", "corr", "mean fcst mm", "mean ERA5 mm"], skill_rows))
with open(os.path.join(OUT, "skill_by_lead.csv"), "w", newline="") as f:
    csv.writer(f).writerows([["model", "lead", "n20", "pod20", "far20", "n50", "pod50", "far50", "corr", "mean_fc", "mean_era5"]] + skill_rows)

print("\n## Event-day rainfall at the evaluated points (mm)\n")
ev_rain = []
body = []
for ev in EVENTS:
    for loc in ev["sites"]:
        e = ev["date"]
        win = [add_days(e, k) for k in (-1, 0, 1)]
        peak_era = max(era5[loc][d] for d in win)
        fc = json.load(open(os.path.join(DATA, "forecast_ecmwf_ifs025.json")))["forecast"][loc]
        # forecast issued 1 day before for the event day (lead 1) and 3 days before (lead 3)
        f1 = fc["1"][e]; f3 = fc["3"][e]
        g = json.load(open(os.path.join(DATA, "forecast_gfs_seamless.json")))["forecast"][loc]
        body.append([ev["id"], loc_name[loc], e, peak_era, f1, f3, g["1"][e], g["3"][e]])
        ev_rain.append([ev["id"], loc_name[loc], e, peak_era, f1, f3, g["1"][e], g["3"][e]])
print(md_table(["ID", "Point", "Event date", "ERA5 max daily mm (E-1..E+1)", "ECMWF lead-1 mm on E", "ECMWF lead-3 mm on E", "GFS lead-1 mm on E", "GFS lead-3 mm on E"], body))
with open(os.path.join(OUT, "event_rain.csv"), "w", newline="") as f:
    csv.writer(f).writerows([["event", "point", "date", "era5_peak", "ecmwf_l1", "ecmwf_l3", "gfs_l1", "gfs_l3"]] + ev_rain)

print("\n## Undated impacts (Budalang'i, Homa Bay, Mandera): highest level 20 Apr - 10 May, HQ points\n")
for loc in ["c40", "x_budalangi", "c43", "c9"]:
    cells = []
    for s in SOURCES:
        ls = [level(s, loc, add_days("2024-04-20", k)) for k in range(0, 21)]
        cells.append(f"{s}: {NAMES[max(ls)]}")
    print(loc_name[loc], "|", "; ".join(cells))
