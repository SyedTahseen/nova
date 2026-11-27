#!/usr/bin/env python3
"""
Generate app/src/main/assets/world.bin: country outlines for the in-app map.

Source: Natural Earth 1:50m "Admin 0 - Countries" (public domain, naturalearthdata.com).
Uses only the Python standard library.

    python3 tools/generate_world_map.py                      # downloads the data
    python3 tools/generate_world_map.py --input countries.geojson

File format (little-endian):
    4 bytes   magic "NVM1"
    uint32    ring count
    per ring: uint32 point count, then point count x (int16 lon*100, int16 lat*100)
Every ring is one country outline (outer ring only), drawn filled + stroked, so shared
borders show up as lines between countries.
"""
import argparse
import json
import pathlib
import struct
import sys
import urllib.request

URL = ("https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/"
       "geojson/ne_50m_admin_0_countries.geojson")
DEFAULT_OUT = (pathlib.Path(__file__).resolve().parent.parent
               / "app" / "src" / "main" / "assets" / "world.bin")

TOLERANCE_DEG = 0.04   # Douglas-Peucker tolerance; ~0.2 dp on screen at our zoom
MIN_EXTENT_DEG = 0.12  # drop specks smaller than this
SKIP = {"Antarctica"}


def simplify(pts, tol):
    """Iterative Douglas-Peucker."""
    n = len(pts)
    if n < 5:
        return pts
    keep = [False] * n
    keep[0] = keep[-1] = True
    t2 = tol * tol
    stack = [(0, n - 1)]
    while stack:
        a, b = stack.pop()
        ax, ay = pts[a]
        bx, by = pts[b]
        dx, dy = bx - ax, by - ay
        d2 = dx * dx + dy * dy
        best, idx = -1.0, -1
        for i in range(a + 1, b):
            px, py = pts[i]
            if d2 == 0:
                dist2 = (px - ax) ** 2 + (py - ay) ** 2
            else:
                t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / d2))
                cx, cy = ax + t * dx, ay + t * dy
                dist2 = (px - cx) ** 2 + (py - cy) ** 2
            if dist2 > best:
                best, idx = dist2, i
        if best > t2:
            keep[idx] = True
            stack.append((a, idx))
            stack.append((idx, b))
    return [p for p, k in zip(pts, keep) if k]


def outer_rings(geometry):
    if not geometry:
        return
    kind = geometry.get("type")
    coords = geometry.get("coordinates", [])
    if kind == "Polygon":
        if coords:
            yield coords[0]
    elif kind == "MultiPolygon":
        for poly in coords:
            if poly:
                yield poly[0]


def build(geojson):
    rings = []
    for feature in geojson.get("features", []):
        props = feature.get("properties") or {}
        name = props.get("ADMIN") or props.get("NAME") or ""
        if name in SKIP:
            continue
        for ring in outer_rings(feature.get("geometry")):
            pts = [(float(p[0]), float(p[1])) for p in ring]
            if len(pts) > 1 and pts[0] == pts[-1]:
                pts = pts[:-1]
            xs = [p[0] for p in pts]
            ys = [p[1] for p in pts]
            if not pts or (max(xs) - min(xs) < MIN_EXTENT_DEG and max(ys) - min(ys) < MIN_EXTENT_DEG):
                continue
            pts = simplify(pts, TOLERANCE_DEG)
            if len(pts) >= 3:
                rings.append(pts)
    return rings


def encode(rings):
    out = bytearray(b"NVM1")
    out += struct.pack("<I", len(rings))
    for ring in rings:
        out += struct.pack("<I", len(ring))
        for lon, lat in ring:
            out += struct.pack("<hh", round(lon * 100), round(lat * 100))
    return bytes(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", help="local Natural Earth countries GeoJSON (otherwise downloaded)")
    ap.add_argument("--output", default=str(DEFAULT_OUT))
    args = ap.parse_args()

    if args.input:
        data = pathlib.Path(args.input).read_bytes()
    else:
        print("Downloading", URL, file=sys.stderr)
        req = urllib.request.Request(URL, headers={"User-Agent": "novavpn-map-generator"})
        with urllib.request.urlopen(req, timeout=120) as resp:
            data = resp.read()

    rings = build(json.loads(data))
    if not rings:
        sys.exit("No polygons found - is this a Natural Earth countries GeoJSON?")

    blob = encode(rings)
    out = pathlib.Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(blob)
    pts = sum(len(r) for r in rings)
    print(f"Wrote {out} ({len(blob) / 1024:.0f} KB, {len(rings)} outlines, {pts} points)")


if __name__ == "__main__":
    main()
