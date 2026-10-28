"""Evaluate the proposed 'upstream rain' rule (run after BacktestRunnerTest).

  python -I tools/backtest/analyze_upstream.py

Rule, per downstream county-day: if ANY upstream county's rainfall-only level (heavy day / wet spell, NOT river or
landslide) reaches UP_MIN (High by default), the downstream level is raised to at least FLOOR (Moderate or High).
It never lowers a level. The downstream level otherwise comes from the engine (results.csv: current river points,
5 m3/s floor, landslide signal). Mappings (see Counties.kt): Tana: Garissa c7, Tana River c4 <- Nyeri c19, Murang'a c21,
Embu c14, Meru c12, Kirinyaga c20, Tharaka-Nithi c13. Nzoia: Busia c40, Siaya c41 <- Trans Nzoia c26, Uasin Gishu c27,
Bungoma c39, Kakamega c37. Hit/lead definitions as in analyze_rerun.py (HQ points, issue days 1-3 before the event;
lead = earliest issue day within 6 days before the event that shows High+).
"""
import csv, datetime, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SOURCES = ["ecmwf_ifs025", "gfs_seamless", "era5"]
UP = {"Tana": (["c4", "c7"], ["c19", "c21", "c14", "c12", "c20", "c13"]),
      "Nzoia": (["c40", "c41"], ["c26", "c27", "c39", "c37"])}
EV = {"E6 Garissa": ("c7", "2024-05-06"), "E7 Tana River": ("c4", "2024-05-11")}
NAMES = ["Low", "Moderate", "High", "Severe"]


def add_days(s, k):
    return (datetime.date.fromisoformat(s) + datetime.timedelta(days=k)).isoformat()


def rain_level(w, m3):
    a = 3 if w >= 80 else 2 if w >= 50 else 1 if w >= 20 else 0
    b = 3 if m3 >= 120 else 2 if m3 >= 80 else 1 if m3 >= 40 else 0
    return max(a, b)


rows = [r for r in csv.DictReader(open(os.path.join(ROOT, "composeApp", "build", "backtest", "results.csv"))) if r["loc"].startswith("c")]
base, rl = {}, {}
for r in rows:
    k = (r["source"], r["loc"], r["issue"])
    base[k] = int(r["default_level"])
    rl[k] = rain_level(float(r["wettest_mm"]), float(r["max3_mm"]))
days = sorted({k[2] for k in base})


def lvl(s, loc, d, ups, up_min, floor):
    b = base[(s, loc, d)]
    if floor is None:
        return b
    if any(rl[(s, u, d)] >= up_min for u in ups):
        return max(b, floor)
    return b


def ev_max(s, loc, e, f):
    return max(f(s, loc, add_days(e, -k)) for k in (1, 2, 3))


def lead(s, loc, e, f):
    ks = [k for k in range(1, 7) if f(s, loc, add_days(e, -k)) >= 2]
    return max(ks) if ks else None


variants = [("off", None, None), ("up>=High -> Moderate", 2, 1), ("up>=High -> High", 2, 2), ("up>=Severe -> Moderate", 3, 1),
            ("up>=Severe -> High", 3, 2), ("up>=Moderate -> Moderate", 1, 1)]
print("## Events E6 / E7: highest level in issue days E-3..E-1 / lead (days) of first High+ (ECMWF ; GFS ; ERA5)\n")
print("| Variant | " + " | ".join(EV) + " |")
print("|---|---|---|")
for name, um, fl in variants:
    cells = []
    for ev, (loc, e) in EV.items():
        ups = UP["Tana"][1]
        parts = []
        for s in SOURCES:
            f = lambda src, l, d: lvl(src, l, d, ups, um, fl)
            ld = lead(s, loc, e, f)
            parts.append(f"{NAMES[ev_max(s, loc, e, f)]}/{ld if ld else '-'}")
        cells.append(" ; ".join(parts))
    print(f"| {name} | " + " | ".join(cells) + " |")

print("\n## Share of county-days (3 sources x 78 issue days) at Moderate+ / High+ per downstream group; days changed\n")
print("| Group | Variant | Moderate+ | High+ | Days raised | Raised to High+ |")
print("|---|---|---|---|---|---|")
for g, (downs, ups) in UP.items():
    for name, um, fl in variants:
        n = mp = hp = ch = chh = 0
        for s in SOURCES:
            for loc in downs:
                for d in days:
                    b = base[(s, loc, d)]
                    v = lvl(s, loc, d, ups, um, fl)
                    n += 1; mp += v >= 1; hp += v >= 2; ch += v > b; chh += v >= 2 > b
        print(f"| {g} | {name} | {100*mp/n:.1f} % | {100*hp/n:.1f} % | {ch} | {chh} |")

print("\n## Beyond the river signal: Tana counties, days with an upstream High+ and the base level then\n")
for loc in UP["Tana"][0]:
    for s in SOURCES:
        trig = [d for d in days if any(rl[(s, u, d)] >= 2 for u in UP["Tana"][1])]
        low = [d for d in trig if base[(s, loc, d)] == 0]
        mod = [d for d in trig if base[(s, loc, d)] == 1]
        hi = [d for d in trig if base[(s, loc, d)] >= 2]
        print(f"{loc} {s}: trigger days {len(trig)}/{len(days)}; base Low {len(low)}, Moderate {len(mod)}, already High+ {len(hi)}")

print("\n## Tana events, issue days E-6..E-1: (date, base level initial, max upstream rain-only level 0-3)\n")
for ev, (loc, e) in EV.items():
    for s in SOURCES:
        print(ev, s, [(add_days(e, -k)[5:], NAMES[base[(s, loc, add_days(e, -k))]][0],
                       max(rl[(s, u, add_days(e, -k))] for u in UP["Tana"][1])) for k in range(6, 0, -1)])

print("\n## Days the 'up>=High' rule raises on Tana counties (3 sources): how many within +-7 days of that county's event\n")
for fl in (1, 2):
    for s in SOURCES:
        tot = near = 0
        for loc, e in EV.values():
            for d in days:
                if lvl(s, loc, d, UP["Tana"][1], 2, fl) > base[(s, loc, d)]:
                    tot += 1
                    near += abs((datetime.date.fromisoformat(d) - datetime.date.fromisoformat(e)).days) <= 7
        print(f"floor {NAMES[fl]} {s}: raised {tot}, within +-7 d of that county's event {near}")
