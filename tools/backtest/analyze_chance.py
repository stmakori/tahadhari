"""Chance baseline: how often does the engine show High+ (or Moderate+) in some 3-day window of issue days
at an event county on a randomly chosen date? This is what a hit has to beat.

  python -I tools/backtest/analyze_chance.py
"""
import csv, datetime, os, collections

HERE = os.path.dirname(os.path.abspath(__file__))
rows = list(csv.DictReader(open(os.path.join(HERE, "..", "..", "composeApp", "build", "backtest", "results.csv"))))
by = collections.defaultdict(dict)
for r in rows:
    by[(r["source"], r["loc"])][r["issue"]] = r
HQ = {"E1": "c47", "E2": "c34", "E3": "c32", "E4": "c34", "E5": "c42", "E6": "c7", "E7": "c4"}


def d(s, k):
    return (datetime.date.fromisoformat(s) + datetime.timedelta(days=k)).isoformat()


start, end = "2024-03-18", "2024-05-31"  # so that 3 prior issue days exist
for sc in ("default", "rain_lower"):
    print("scenario", sc)
    for source in ("ecmwf_ifs025", "gfs_seamless", "era5"):
        exp_h = exp_m = 0.0
        for ev, loc in HQ.items():
            s = by[(source, loc)]
            days = [x for x in s if start <= x <= end]
            hh = mm = 0
            for x in days:
                lv = [int(s[d(x, -k)][sc + "_level"]) for k in (1, 2, 3) if d(x, -k) in s]
                hh += max(lv) >= 2
                mm += max(lv) >= 1
            exp_h += hh / len(days)
            exp_m += mm / len(days)
        print(f"  {source:13s} expected High+ hits by chance over 7 events: {exp_h:.1f}; Moderate+: {exp_m:.1f}")
