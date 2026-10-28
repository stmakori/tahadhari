"""River-signal diagnostics for docs/validation.md (run after BacktestRunnerTest).

  python -I tools/backtest/analyze_river.py

The ratio here uses the same formula as RiverOutlook (peak of D..D+6 over the mean of D-30..D-1) so the
relocated-cell check can be compared with the app's numbers; levels are still only ever assigned by RiskEngine.
"""
import csv, datetime, json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RESULTS = os.path.join(ROOT, "composeApp", "build", "backtest", "results.csv")
DATA = os.path.join(HERE, "data")
OUT = os.path.join(HERE, "out")
os.makedirs(OUT, exist_ok=True)


def add_days(s, k):
    return (datetime.date.fromisoformat(s) + datetime.timedelta(days=k)).isoformat()


rows = list(csv.DictReader(open(RESULTS)))
idx = {(r["source"], r["loc"], r["issue"]): r for r in rows}
loc_name = {r["loc"]: r["county"] for r in rows}
flood = json.load(open(os.path.join(DATA, "flood.json")))["flood"]

print("## Current river points: grid cell, mean discharge over 10 Feb - 7 Jun 2024\n")
for loc, v in flood.items():
    vals = [x for x in v["daily"].values() if x is not None]
    print(f"{loc} {loc_name.get(loc, '?'):11s} {v['river']:13s} grid ({v['grid_lat']:.3f},{v['grid_lon']:.3f}) mean {sum(vals)/len(vals):8.2f} m3/s  min {min(vals):.2f} max {max(vals):.2f}")

print("\n## Relocated Tana cells (same ratio formula as RiverOutlook)\n")
alt = json.load(open(os.path.join(DATA, "flood_alt.json")))["flood"]
alt_rows = []
for name, event in (("garissa", "2024-05-06"), ("garsen", "2024-05-11")):
    ser = alt[name]["daily"]

    def ratio(d):
        base = [ser[add_days(d, -j)] for j in range(1, 31)]
        peak = max(ser[add_days(d, j)] for j in range(0, 7))
        return peak / (sum(base) / len(base))

    print(f"{name}: requested ({alt[name]['lat']},{alt[name]['lon']}), grid ({alt[name]['grid_lat']:.3f},{alt[name]['grid_lon']:.3f}), "
          f"event-day discharge {ser[event]} m3/s, mean {sum(ser.values())/len(ser):.0f} m3/s")
    print("  ratio by days before event:", " ".join(f"{k}:{ratio(add_days(event, -k)):.2f}" for k in range(10, -1, -1)))
    allr = [ratio(add_days("2024-03-15", k)) for k in range(78)]
    print(f"  share of issue days >=2x: {100*sum(x>=2 for x in allr)/len(allr):.1f}%  >=3x: {100*sum(x>=3 for x in allr)/len(allr):.1f}%  max {max(allr):.2f}")
    alt_rows += [[name, add_days("2024-03-15", k), round(r, 3), round(ser[add_days('2024-03-15', k)], 1)] for k, r in enumerate(allr)]
    # peak window of the discharge series
    pk = max(ser.items(), key=lambda kv: kv[1])
    print(f"  peak discharge in series {pk[1]} m3/s on {pk[0]}")
with open(os.path.join(OUT, "alt_river_ratio.csv"), "w", newline="") as f:
    csv.writer(f).writerows([["cell", "issue", "ratio", "discharge"]] + alt_rows)

print("\n## Ratio just before the Kisumu (5 May) and Busia events with the current points\n")
for loc, e in (("c42", "2024-05-05"), ("c40", "2024-05-01")):
    print(loc, [idx[("era5", loc, add_days(e, -k))]["river_ratio"] for k in range(10, -1, -1)])

print("\n## When does the river signal fire (>=2x)?\n")
for loc in flood:
    ds = [r["issue"] for r in rows if r["source"] == "era5" and r["loc"] == loc and float(r["river_ratio"]) >= 2]
    print(loc, loc_name[loc], (ds[0], ds[-1], len(ds)) if ds else None)
    # contiguous runs
    runs, start, prev = [], None, None
    for d in ds:
        if start is None: start = d
        elif add_days(prev, 1) != d: runs.append((start, prev)); start = d
        prev = d
    if start: runs.append((start, prev))
    print("   runs:", runs)
