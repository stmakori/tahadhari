"""Would a 14-day accumulated-rain signal separate the event days from ordinary wet-season days? (ERA5, HQ points)

  python -I tools/backtest/analyze_antecedent.py
"""
import datetime, json, os

HERE = os.path.dirname(os.path.abspath(__file__))
era5 = json.load(open(os.path.join(HERE, "data", "era5.json")))["era5"]


def d(s, k):
    return (datetime.date.fromisoformat(s) + datetime.timedelta(days=k)).isoformat()


def acc(loc, e, n=14):
    return sum(era5[loc][d(e, -j)] for j in range(n))


events = [("E1 Nairobi", "c47", "2024-04-24"), ("E2/E4 Kajiado", "c34", "2024-04-24"), ("E3 Nakuru", "c32", "2024-04-29"),
          ("E5 Kisumu", "c42", "2024-05-05"), ("E6 Garissa", "c7", "2024-05-06"), ("E7 Tana River", "c4", "2024-05-11")]
season = [d("2024-03-28", k) for k in range(0, 65)]  # days with a full 14-day window inside the data, to 31 May
for name, loc, e in events:
    own = sorted(acc(loc, x) for x in season)
    v = acc(loc, e)
    pct = 100 * sum(o <= v for o in own) / len(own)
    all_hq = sorted(acc(l, x) for l in era5 if l.startswith("c") for x in season)
    pct_all = 100 * sum(o <= v for o in all_hq) / len(all_hq)
    print(f"{name:14s} 14-day ERA5 total to {e}: {v:6.1f} mm  = percentile {pct:5.1f} of its own season, {pct_all:5.1f} of all county-days;"
          f"  max single day in window {max(era5[loc][d(e, -j)] for j in range(14)):.1f} mm")
