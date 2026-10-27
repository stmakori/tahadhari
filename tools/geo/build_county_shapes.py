"""
Builds composeApp/src/commonMain/kotlin/ke/tahadhari/data/geo/CountyShapeData.kt from the
geoBoundaries Kenya ADM1 (county) boundaries.

Source: geoBoundaries gbOpen KEN ADM1 (https://www.geoboundaries.org/api/current/gbOpen/KEN/ADM1/),
boundary data from the RCMRD GeoPortal, listed by geoBoundaries as Public Domain.
Cite: Runfola et al. (2020) geoBoundaries: A global database of political administrative boundaries.
PLoS ONE 15(4): e0231866.

Usage (download into an empty directory first; the file is treated as untrusted data):

    curl -sSL https://www.geoboundaries.org/api/current/gbOpen/KEN/ADM1/ -o build/geo-download/meta.json
    # take "simplifiedGeometryGeoJSON" from meta.json, then:
    curl -sSL <that url> -o build/geo-download/ken_adm1_simplified.geojson
    python -I tools/geo/build_county_shapes.py build/geo-download/ken_adm1_simplified.geojson \
        composeApp/src/commonMain/kotlin/ke/tahadhari/data/geo/CountyShapeData.kt

What it does:
  1. Matches each feature to our county code (data/Counties.kt) by normalised name, with
     aliases for spelling variants. Fails if any county is missing or unmatched.
  2. Keeps outer rings only (no holes), simplifies each with Douglas-Peucker (TOLERANCE degrees)
     and drops islands smaller than MIN_PART_AREA square degrees (keeps the largest part always).
  3. Writes compact strings: points "lon,lat" with 3 decimals separated by spaces, parts
     separated by ";". Strings live in the class constant pool, so the JVM 64 KB method limit
     does not apply (each string stays well under the 65,535-byte constant limit).
Standard library only.
"""
import json
import re
import sys

TOLERANCE = 0.008        # degrees (~0.9 km), well under a pixel at dashboard map sizes
MIN_PART_AREA = 0.0008   # square degrees (~10 km2): drops tiny islets, keeps Lamu, Pate, Manda, Mombasa island

# Our county codes and names, mirrored from composeApp/.../data/Counties.kt.
COUNTIES = {
    1: "Mombasa", 2: "Kwale", 3: "Kilifi", 4: "Tana River", 5: "Lamu", 6: "Taita-Taveta",
    7: "Garissa", 8: "Wajir", 9: "Mandera", 10: "Marsabit", 11: "Isiolo", 12: "Meru",
    13: "Tharaka-Nithi", 14: "Embu", 15: "Kitui", 16: "Machakos", 17: "Makueni", 18: "Nyandarua",
    19: "Nyeri", 20: "Kirinyaga", 21: "Murang'a", 22: "Kiambu", 23: "Turkana", 24: "West Pokot",
    25: "Samburu", 26: "Trans Nzoia", 27: "Uasin Gishu", 28: "Elgeyo-Marakwet", 29: "Nandi",
    30: "Baringo", 31: "Laikipia", 32: "Nakuru", 33: "Narok", 34: "Kajiado", 35: "Kericho",
    36: "Bomet", 37: "Kakamega", 38: "Vihiga", 39: "Bungoma", 40: "Busia", 41: "Siaya",
    42: "Kisumu", 43: "Homa Bay", 44: "Migori", 45: "Kisii", 46: "Nyamira", 47: "Nairobi",
}

# Spelling variants seen in boundary datasets -> our canonical name (normalised keys).
ALIASES = {
    "tharaka": "tharakanithi",
    "tharakanithi": "tharakanithi",
    "taitataveta": "taitataveta",
    "elgeyomarakwet": "elgeyomarakwet",
    "keiyomarakwet": "elgeyomarakwet",
    "muranga": "muranga",
    "nairobicity": "nairobi",
    "homabay": "homabay",
    "transnzoia": "transnzoia",
}


def norm(name):
    n = re.sub(r"[^a-z]", "", name.lower())
    return ALIASES.get(n, n)


def perp_dist(p, a, b):
    (x, y), (x1, y1), (x2, y2) = p, a, b
    dx, dy = x2 - x1, y2 - y1
    if dx == 0 and dy == 0:
        return ((x - x1) ** 2 + (y - y1) ** 2) ** 0.5
    t = max(0.0, min(1.0, ((x - x1) * dx + (y - y1) * dy) / (dx * dx + dy * dy)))
    px, py = x1 + t * dx, y1 + t * dy
    return ((x - px) ** 2 + (y - py) ** 2) ** 0.5


def douglas_peucker(points, tol):
    """Iterative DP on an open polyline; keeps both endpoints."""
    n = len(points)
    if n < 3:
        return list(points)
    keep = [False] * n
    keep[0] = keep[-1] = True
    stack = [(0, n - 1)]
    while stack:
        s, e = stack.pop()
        best, idx = 0.0, -1
        for i in range(s + 1, e):
            d = perp_dist(points[i], points[s], points[e])
            if d > best:
                best, idx = d, i
        if best > tol and idx > 0:
            keep[idx] = True
            stack.append((s, idx))
            stack.append((idx, e))
    return [p for p, k in zip(points, keep) if k]


def simplify_ring(ring, tol):
    pts = [(float(x), float(y)) for x, y, *_ in ring]
    if len(pts) > 1 and pts[0] == pts[-1]:
        pts = pts[:-1]
    if len(pts) < 4:
        return pts
    # Split the closed ring at the point farthest from the first so DP has a real baseline.
    far = max(range(len(pts)), key=lambda i: (pts[i][0] - pts[0][0]) ** 2 + (pts[i][1] - pts[0][1]) ** 2)
    a = douglas_peucker(pts[: far + 1], tol)
    b = douglas_peucker(pts[far:] + [pts[0]], tol)
    return a[:-1] + b[:-1]


def area(ring):
    s = 0.0
    for i in range(len(ring)):
        x1, y1 = ring[i]
        x2, y2 = ring[(i + 1) % len(ring)]
        s += x1 * y2 - x2 * y1
    return abs(s) / 2


def main(src, dst):
    with open(src, encoding="utf-8") as f:
        data = json.load(f)
    by_norm = {norm(v): k for k, v in COUNTIES.items()}
    shapes, report = {}, []
    for feat in data["features"]:
        name = feat["properties"]["shapeName"]
        code = by_norm.get(norm(name))
        if code is None:
            sys.exit(f"Unmatched feature name: {name!r}")
        if code in shapes:
            sys.exit(f"Duplicate match for {COUNTIES[code]}: {name!r}")
        if name != COUNTIES[code]:
            report.append(f"  {name!r} -> {code} {COUNTIES[code]}")
        g = feat["geometry"]
        polys = [g["coordinates"]] if g["type"] == "Polygon" else g["coordinates"]
        holes = sum(len(p) - 1 for p in polys)
        parts = [simplify_ring(p[0], TOLERANCE) for p in polys]  # outer rings only
        parts = [p for p in parts if len(p) >= 3]
        parts.sort(key=area, reverse=True)
        kept = [parts[0]] + [p for p in parts[1:] if area(p) >= MIN_PART_AREA]
        shapes[code] = kept
        if holes:
            report.append(f"  {COUNTIES[code]}: dropped {holes} interior ring(s)")
    missing = sorted(set(COUNTIES) - set(shapes))
    if missing:
        sys.exit(f"Counties without a shape: {[COUNTIES[c] for c in missing]}")

    def enc(parts):
        return ";".join(" ".join(f"{x:.3f},{y:.3f}" for x, y in p) for p in parts)

    lines = [
        "// GENERATED by tools/geo/build_county_shapes.py. Do not edit by hand.",
        "// Source: geoBoundaries gbOpen KEN ADM1 (RCMRD GeoPortal), Public Domain.",
        "// Runfola et al. (2020), PLoS ONE 15(4): e0231866. https://www.geoboundaries.org",
        f"// Simplified with Douglas-Peucker, tolerance {TOLERANCE} degrees.",
        "// Format: parts separated by ';', points 'lon,lat' separated by spaces.",
        "package ke.tahadhari.data.geo",
        "",
        "internal object CountyShapeData {",
        "    /** Encoded outline per county code. */",
        "    val encoded: Map<Int, String> by lazy {",
        "        mapOf(",
    ]
    total_pts = 0
    for code in sorted(shapes):
        total_pts += sum(len(p) for p in shapes[code])
        lines.append(f'            {code} to {code_fn(code)}(),')
    lines += ["        )", "    }", ""]
    # One function per county keeps each method tiny.
    for code in sorted(shapes):
        lines.append(f"    // {COUNTIES[code]}")
        lines.append(f'    private fun {code_fn(code)}() = "{enc(shapes[code])}"')
    lines.append("}")
    out = "\n".join(lines) + "\n"
    with open(dst, "w", encoding="utf-8", newline="\n") as f:
        f.write(out)
    print(f"Wrote {dst}: {len(shapes)} counties, {total_pts} points, {len(out.encode('utf-8'))} bytes")
    print("Name differences resolved:" if report else "No name differences.")
    print("\n".join(report))
    for code in sorted(shapes):
        if len(shapes[code]) > 1:
            print(f"  {COUNTIES[code]}: {len(shapes[code])} parts kept")


def code_fn(code):
    return f"c{code:02d}"


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("usage: build_county_shapes.py <input.geojson> <output.kt>")
    main(sys.argv[1], sys.argv[2])
