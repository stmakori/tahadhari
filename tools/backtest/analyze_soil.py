"""Evaluate the proposed 'saturated ground' rule (run after BacktestRunnerTest and fetch_soil.py).

  python -I tools/backtest/analyze_soil.py

Rule, per county-day: if soil moisture >= T AND the rainfall-only level (heavy day / wet spell) is at least
MODERATE, raise the overall level one step, capped at HIGH. It never raises LOW and never creates SEVERE.
The overall level comes from the engine (results.csv: current points, 5 m3/s floor, landslide signal).
Soil moisture is the daily mean on the issue day itself (what a morning forecast would call 'now').
Two soil series are tried (see fetch_soil.py): ifs025 (9-27 cm, forecast-model layer) and era5 (7-28 cm, reanalysis).
Hit/scoring definitions are the same as analyze_rerun.py (HQ points, issue days 1-3 days before the event).
"""
import collections, csv, datetime, json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
DATA = os.path.join(HERE, "data")
SOURCES = ["ecmwf_ifs025", "gfs_seamless", "era5"]
SOIL = ["ifs025", "era5"]
T_GRID = [0.30, 0.33, 0.36, 0.40, 0.45]
HQ_EVENT = {"E1": ("Nairobi", "c47", "2024-04-24"), "E2": ("Kajiado", "c34", "2024-04-24"), "E3": ("Nakuru", "c32", "2024-04-29"),
            "E4": ("Kajiado", "c34", "2024-04-30"), "E5": ("Kisumu", "c42", "2024-05-05"), "E6": ("Garissa", "c7", "2024-05-06"),
            "E7": ("Tana River", "c4", "2024-05-11")}


def add_days(s, k):
    return (datetime.date.fromisoformat(s) + datetime.timedelta(days=k)).isoformat()


def rain_level(w, m3):
    a = 3 if w >= 80 else 2 if w >= 50 else 1 if w >= 20 else 0
    b = 3 if m3 >= 120 else 2 if m3 >= 80 else 1 if m3 >= 40 else 0
    return max(a, b)


def apply_rule(level, rain_lv, soil, t):
    if t is not None and soil is not None and soil >= t and rain_lv >= 1 and level >= 1:
        return min(level + 1, 2) if level < 2 else level  # raise one step, capped at High; High stays, Severe stays
    return level


rows = [r for r in csv.DictReader(open(os.path.join(ROOT, "composeApp", "build", "backtest", "results.csv"))) if r["loc"].startswith("c")]
soil = json.load(open(os.path.join(DATA, "soil.json")))["soil"]
base, rainlv, cty = {}, {}, {}
for r in rows:
    k = (r["source"], r["loc"], r["issue"])
    base[k] = int(r["default_level"])
    rainlv[k] = rain_level(float(r["wettest_mm"]), float(r["max3_mm"]))
    cty[r["loc"]] = r["county"]

missing = sum(1 for (s, l, d) in base if soil["ifs025"].get(l, {}).get(d) is None or soil["era5"].get(l, {}).get(d) is None)
print("county-days (all sources) with a missing soil value:", missing, "of", len(base))

print("\n## Soil moisture distribution on issue days, 47 HQs (m3/m3); percentiles 5/25/50/75/90/95/99\n")
for sn in SOIL:
    v = sorted(x for loc, d in soil[sn].items() if loc.startswith("c") for dd, x in d.items() if x is not None and "2024-03-15" <= dd <= "2024-05-31")
    n = len(v)
    print(sn, "n", n, [round(v[min(n - 1, int(n * q))], 3) for q in (.05, .25, .5, .75, .9, .95, .99)], "max", round(v[-1], 3))
sm = [(sn, loc) for sn in SOIL for loc in ("c47", "c34", "c32", "c42", "c7", "c4")]
print("\nHQ mean over the season (m3/m3):")
for loc in ("c47", "c34", "c32", "c42", "c7", "c4"):
    print(f"  {cty[loc]:11s}", {sn: round(sum(x for dd, x in soil[sn][loc].items() if x is not None) / len(soil[sn][loc]), 3) for sn in SOIL})
a = [(soil["ifs025"][l][d], soil["era5"][l][d]) for l in soil["ifs025"] if l.startswith("c") for d in soil["ifs025"][l]
     if soil["ifs025"][l][d] is not None and soil["era5"][l][d] is not None]
mx = sum(p for p, q in a) / len(a); my = sum(q for p, q in a) / len(a)
cov = sum((p - mx) * (q - my) for p, q in a)
corr = cov / (sum((p - mx) ** 2 for p, q in a) * sum((q - my) ** 2 for p, q in a)) ** 0.5
print(f"ifs025 vs era5 on all county-days: mean {mx:.3f} vs {my:.3f}, correlation {corr:.2f}")

print("\n## Soil moisture on issue days E-3..E-1 at the event counties (mean) and its percentile within that county's own season\n")
for eid, (name, loc, e) in HQ_EVENT.items():
    cells = []
    for sn in SOIL:
        vals = [soil[sn][loc][add_days(e, -k)] for k in (1, 2, 3)]
        m = sum(vals) / 3
        season = sorted(x for x in soil[sn][loc].values() if x is not None)
        pct = 100 * sum(x <= m for x in season) / len(season)
        cells.append(f"{sn} {m:.3f} (p{pct:.0f})")
    print(f"{eid} {name:11s} " + "; ".join(cells))


def event_max(lvfun, s, loc, e):
    return max(lvfun(s, loc, add_days(e, -k)) for k in (1, 2, 3))


def chance(lvfun, s):
    eh = 0.0
    for eid, (name, loc, e) in HQ_EVENT.items():
        days = sorted(d for (src, l, d) in base if src == s and l == loc and "2024-03-18" <= d <= "2024-05-31")
        eh += sum(max(lvfun(s, loc, add_days(x, -k)) for k in (1, 2, 3)) >= 2 for x in days) / len(days)
    return eh


out = [["soil", "T", "source", "high_plus_hits", "high_plus_share_pct", "chance_hits", "moderate_to_high_days"]]
print("\n## Saturated-ground rule: High+ hits on the 7 events (HQ), High+ share of all county-days, chance baseline\n")
print("| Soil series | T | Input | High+ hits /7 | High+ share | Moderate days raised to High | Chance (hits) |")
print("|---|---|---|---|---|---|---|")
for sn in SOIL:
    for t in [None] + T_GRID:
        for s in SOURCES:
            f = lambda src, loc, d, sn=sn, t=t: apply_rule(base[(src, loc, d)], rainlv[(src, loc, d)], soil[sn][loc].get(d), t)
            hits = sum(event_max(f, s, loc, e) >= 2 for eid, (nm, loc, e) in HQ_EVENT.items())
            vals = [f(src, loc, d) for (src, loc, d) in base if src == s]
            hp = 100 * sum(v >= 2 for v in vals) / len(vals)
            raised = sum(f(*k) > base[k] for k in base if k[0] == s)
            ch = chance(f, s)
            lab = "off" if t is None else f"{t:.2f}"
            print(f"| {sn} | {lab} | {s} | {hits} | {hp:.1f} % | {raised} | {ch:.1f} |")
            out.append([sn, lab, s, hits, round(hp, 2), round(ch, 2), raised])
with open(os.path.join(HERE, "out", "soil_rule.csv"), "w", newline="") as fh:
    csv.writer(fh).writerows(out)

print("\n## Which events are newly caught (High+ in E-3..E-1) and by what input, per T (ifs025 soil / era5 soil)\n")
for sn in SOIL:
    for t in T_GRID:
        newly = []
        for eid, (nm, loc, e) in HQ_EVENT.items():
            for s in SOURCES:
                f = lambda src, l, d: apply_rule(base[(src, l, d)], rainlv[(src, l, d)], soil[sn][l].get(d), t)
                if event_max(f, s, loc, e) >= 2 > event_max(lambda a, b, c: base[(a, b, c)], s, loc, e):
                    newly.append(f"{eid}/{s[:4]}")
        print(sn, t, newly)

print("\n## Of the county-days the rule raises, how many fall within +-3 days of a documented event in that county?\n")
ev_days = collections.defaultdict(set)
for eid, (nm, loc, e) in HQ_EVENT.items():
    for k in range(-3, 4):
        ev_days[loc].add(add_days(e, k))
for sn in SOIL:
    for t in T_GRID:
        tot = near = 0
        for k in base:
            if k[0] != "ecmwf_ifs025":
                continue
            if apply_rule(base[k], rainlv[k], soil[sn][k[1]].get(k[2]), t) > base[k]:
                tot += 1
                near += k[2] in ev_days.get(k[1], ())
        print(f"{sn} T={t}: raised {tot}, within +-3 days of an event {near}")
