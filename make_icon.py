#!/usr/bin/env python3
"""Draws the launcher icon: three progress rings on a dark rounded tile. Stdlib only."""
import math
import pathlib
import struct
import zlib

SIZE = 192
SAMPLES = 3
BACKGROUND = (30, 32, 38)
# inner radius, outer radius, sweep in degrees, colour
RINGS = [(62, 80, 290, (124, 110, 245)), (42, 58, 235, (240, 84, 96)), (22, 38, 170, (245, 190, 60))]
TRACK = (58, 60, 68)
CORNER = 40


def tile_contains(x, y):
    dx = max(CORNER - x, x - (SIZE - CORNER), 0)
    dy = max(CORNER - y, y - (SIZE - CORNER), 0)
    return dx * dx + dy * dy <= CORNER * CORNER


def colour_at(x, y):
    if not tile_contains(x, y):
        return (0, 0, 0, 0)
    dx, dy = x - SIZE / 2, y - SIZE / 2
    radius = math.hypot(dx, dy)
    angle = math.degrees(math.atan2(dx, -dy)) % 360
    for inner, outer, sweep, colour in RINGS:
        if inner <= radius <= outer:
            return (*(colour if angle <= sweep else TRACK), 255)
    return (*BACKGROUND, 255)


def pixel(px, py):
    total = [0, 0, 0, 0]
    for sy in range(SAMPLES):
        for sx in range(SAMPLES):
            sample = colour_at(px + (sx + 0.5) / SAMPLES, py + (sy + 0.5) / SAMPLES)
            for i in range(3):
                total[i] += sample[i] * sample[3]
            total[3] += sample[3]
    if total[3] == 0:
        return bytes(4)
    return bytes([*(round(total[i] / total[3]) for i in range(3)), round(total[3] / SAMPLES ** 2)])


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))


rows = b"".join(b"\0" + b"".join(pixel(x, y) for x in range(SIZE)) for y in range(SIZE))
png = (b"\x89PNG\r\n\x1a\n"
       + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0))
       + chunk(b"IDAT", zlib.compress(rows, 9))
       + chunk(b"IEND", b""))

out = pathlib.Path(__file__).resolve().parent / "res" / "drawable-nodpi" / "ic_move.png"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_bytes(png)
print(f"wrote {out} ({len(png)} bytes)")
