#!/usr/bin/env python3
"""Generates every texture and JSON resource for the mod.

Reads the mask list straight from KanohiPower.java so the Java code stays the single source of truth.
Run from anywhere:  python3 tools/gen_assets.py
Requires Pillow.
"""
import hashlib
import json
import math
import random
import re
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
MODID = "bioniclemasks"
JAVA_LORE = ROOT / "src/main/java/com/cstestcs/bioniclemasks/lore"
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets" / MODID
DATA = RES / "data" / MODID

PATTERNS = ["flame", "wave", "swirl", "rock", "frost", "spark", "shadow", "leaf", "crystal"]
POWERS = ["reconstitute", "freeze", "weaken", "remove_poison", "enlarge", "shrink", "regenerate", "teleport"]
POWER_NAMES = {
    "reconstitute": "Reconstitutes at Random", "freeze": "Freeze", "weaken": "Weaken",
    "remove_poison": "Remove Poison", "enlarge": "Enlarge", "shrink": "Shrink",
    "regenerate": "Regenerate", "teleport": "Teleport",
}
GREAT_DISKS = {  # metru -> (power, code)
    "ta": ("enlarge", "159"), "ga": ("regenerate", "279"), "po": ("weaken", "339"),
    "ko": ("freeze", "429"), "le": ("teleport", "589"), "onu": ("reconstitute", "619"),
}
METRU_NAMES = {"ta": "Ta-Metru", "ga": "Ga-Metru", "po": "Po-Metru", "ko": "Ko-Metru", "le": "Le-Metru", "onu": "Onu-Metru"}


def read_masks():
    text = (JAVA_LORE / "KanohiPower.java").read_text()
    pattern = re.compile(
        r'^\s{4}([A-Z_]+)\("([^"]+)", "([^"]+)", Kind\.(\w+), (\d+), "([A-Z]*)", Group\.(\w+),', re.M)
    masks = []
    for m in pattern.finditer(text):
        masks.append({
            "enum": m.group(1), "id": m.group(1).lower(), "name": m.group(2), "power": m.group(3),
            "kind": m.group(4), "group": m.group(7),
        })
    assert len(masks) == 56, len(masks)
    return masks


def display_name(mask):
    unnamed = mask["group"] == "UNNAMED" or mask["id"] in ("clairvoyance", "emulation", "growth", "rahi_control", "creation")
    return ("Mask of " if unnamed else "Kanohi ") + mask["name"]


# ---------------------------------------------------------------------------------------------
# Mask silhouettes
# ---------------------------------------------------------------------------------------------

S = 8          # supersampling factor
N = 32         # final texture size
B = N * S      # working canvas size
C = B // 2     # centre

# Hand-picked looks for the famous masks; everything else is derived from a hash of its name.
STYLES = {
    "hau":      dict(face="wide",   crest="ridge",  eyes="slant",  mouth="v",      sides="flare"),
    "kaukau":   dict(face="round",  crest="dome",   eyes="visor",  mouth="tube",   sides="none"),
    "miru":     dict(face="oval",   crest="peak",   eyes="narrow", mouth="v",      sides="wings"),
    "kakama":   dict(face="long",   crest="fins",   eyes="round",  mouth="beak",   sides="none"),
    "pakari":   dict(face="wide",   crest="plate",  eyes="angry",  mouth="grille", sides="cheeks"),
    "akaku":    dict(face="oval",   crest="plate",  eyes="scope",  mouth="slit",   sides="none"),
    "huna":     dict(face="tall",   crest="horns",  eyes="round",  mouth="slit",   sides="none"),
    "rau":      dict(face="round",  crest="crown",  eyes="wide",   mouth="slit",   sides="cheeks"),
    "mahiki":   dict(face="oval",   crest="spikes", eyes="slant",  mouth="fangs",  sides="none"),
    "komau":    dict(face="shield", crest="crown",  eyes="angry",  mouth="v",      sides="none"),
    "ruru":     dict(face="round",  crest="none",   eyes="round",  mouth="beak",   sides="wings"),
    "matatu":   dict(face="tall",   crest="dome",   eyes="narrow", mouth="grille", sides="tusks"),
    "vahi":     dict(face="shield", crest="crown",  eyes="slant",  mouth="grille", sides="flare"),
    "ignika":   dict(face="round",  crest="ridge",  eyes="round",  mouth="v",      sides="cheeks"),
    "kraahkan": dict(face="shield", crest="spikes", eyes="angry",  mouth="fangs",  sides="tusks"),
    "avohkii":  dict(face="oval",   crest="crown",  eyes="slant",  mouth="v",      sides="flare"),
}
CHOICES = dict(
    face=["oval", "shield", "round", "long", "wide", "tall"],
    crest=["none", "ridge", "peak", "fins", "dome", "crown", "horns", "spikes", "plate"],
    eyes=["round", "slant", "visor", "narrow", "scope", "angry", "wide", "tri"],
    mouth=["none", "grille", "v", "jaw", "tube", "beak", "fangs", "slit"],
    sides=["none", "wings", "cheeks", "tusks", "flare"],
)


def style_for(mask_id):
    if mask_id in STYLES:
        return STYLES[mask_id]
    h = hashlib.sha256(mask_id.encode()).digest()
    return {k: v[h[i] % len(v)] for i, (k, v) in enumerate(CHOICES.items())}


def sym_polygon(draw, pts, fill):
    """Draws a polygon and its mirror image across the vertical centre line."""
    draw.polygon(pts, fill=fill)
    draw.polygon([(B - 1 - x, y) for x, y in pts], fill=fill)


def P(v):
    """Converts a coordinate in 32px units to the supersampled canvas."""
    return int(round(v * S))


def draw_mask(mask_id):
    st = style_for(mask_id)
    shape = Image.new("L", (B, B), 0)    # 255 = solid
    detail = Image.new("L", (B, B), 0)   # 255 = dark detail line
    d = ImageDraw.Draw(shape)
    dd = ImageDraw.Draw(detail)

    face = st["face"]
    w, h, top = {"oval": (20, 24, 5), "shield": (22, 25, 4), "round": (22, 22, 6), "long": (18, 26, 4),
                 "wide": (26, 22, 6), "tall": (18, 27, 3)}[face]
    x0, x1 = 16 - w / 2, 16 + w / 2
    y0, y1 = top, top + h
    if face == "shield":
        d.polygon([(P(x0), P(y0 + 3)), (P(x0 + 3), P(y0)), (P(x1 - 3), P(y0)), (P(x1), P(y0 + 3)),
                   (P(x1 - 1), P(y0 + h * 0.65)), (P(16), P(y1)), (P(x0 + 1), P(y0 + h * 0.65))], fill=255)
    elif face == "long":
        d.ellipse([P(x0), P(y0), P(x1), P(y1 - 4)], fill=255)
        d.polygon([(P(x0 + 2), P(y1 - 9)), (P(x1 - 2), P(y1 - 9)), (P(16), P(y1))], fill=255)
    else:
        d.ellipse([P(x0), P(y0), P(x1), P(y1)], fill=255)

    # Crests and brows on top of the mask.
    crest = st["crest"]
    if crest == "ridge":
        d.rectangle([P(x0 + 1), P(y0 + 1), P(x1 - 1), P(y0 + 5)], fill=255)
        dd.line([P(x0 + 2), P(y0 + 5), P(x1 - 2), P(y0 + 5)], fill=255, width=S)
    elif crest == "peak":
        d.polygon([(P(12), P(y0 + 2)), (P(16), P(y0 - 3)), (P(20), P(y0 + 2))], fill=255)
    elif crest == "fins":
        sym_polygon(d, [(P(9), P(y0 + 4)), (P(7), P(y0 - 2)), (P(13), P(y0 + 2))], 255)
    elif crest == "dome":
        d.ellipse([P(x0 + 2), P(y0 - 2), P(x1 - 2), P(y0 + 8)], fill=255)
        dd.arc([P(x0 + 3), P(y0 - 1), P(x1 - 3), P(y0 + 9)], 200, 340, fill=255, width=S)
    elif crest == "crown":
        for cx in (11, 16, 21):
            d.polygon([(P(cx - 2), P(y0 + 2)), (P(cx), P(y0 - 3)), (P(cx + 2), P(y0 + 2))], fill=255)
    elif crest == "horns":
        sym_polygon(d, [(P(10), P(y0 + 3)), (P(6), P(y0 - 3)), (P(8), P(y0 + 5))], 255)
    elif crest == "spikes":
        for cx in (8, 12, 16, 20, 24):
            d.polygon([(P(cx - 1.5), P(y0 + 3)), (P(cx), P(y0 - 1)), (P(cx + 1.5), P(y0 + 3))], fill=255)
    elif crest == "plate":
        d.rectangle([P(11), P(y0 - 1), P(21), P(y0 + 4)], fill=255)
        dd.rectangle([P(13), P(y0 + 1), P(19), P(y0 + 2)], fill=255)

    # Side pieces.
    sides = st["sides"]
    if sides == "wings":
        sym_polygon(d, [(P(x0 + 1), P(y0 + 6)), (P(1), P(y0 + 3)), (P(3), P(y0 + 11)), (P(x0 + 1), P(y0 + 12))], 255)
    elif sides == "cheeks":
        sym_polygon(d, [(P(x0), P(y0 + 11)), (P(x0 - 2), P(y0 + 15)), (P(x0 + 2), P(y0 + 18))], 255)
    elif sides == "tusks":
        sym_polygon(d, [(P(x0 + 2), P(y1 - 6)), (P(x0 - 1), P(y1 + 1)), (P(x0 + 4), P(y1 - 3))], 255)
    elif sides == "flare":
        sym_polygon(d, [(P(x0 + 1), P(y0 + 3)), (P(x0 - 2), P(y0 + 1)), (P(x0), P(y0 + 9))], 255)

    # Mouth area (additions first, then cut-outs).
    mouth = st["mouth"]
    ey = y0 + h * 0.42
    if mouth == "v":
        d.polygon([(P(12), P(y1 - 4)), (P(20), P(y1 - 4)), (P(16), P(y1 + 1))], fill=255)
        dd.line([P(13), P(y1 - 6), P(16), P(y1 - 2), P(19), P(y1 - 6)], fill=255, width=S)
    elif mouth == "tube":
        d.ellipse([P(13), P(y1 - 5), P(19), P(y1 + 1)], fill=255)
        dd.ellipse([P(14.5), P(y1 - 3.5), P(17.5), P(y1 - 0.5)], fill=255)
    elif mouth == "beak":
        d.polygon([(P(13), P(y1 - 7)), (P(19), P(y1 - 7)), (P(16), P(y1 + 2))], fill=255)
        dd.line([P(16), P(y1 - 7), P(16), P(y1)], fill=255, width=S)
    elif mouth == "jaw":
        d.rectangle([P(x0 + 3), P(y1 - 6), P(x1 - 3), P(y1)], fill=255)
        dd.line([P(x0 + 4), P(y1 - 3), P(x1 - 4), P(y1 - 3)], fill=255, width=S)

    # Eyes (cut out).
    eyes = st["eyes"]
    lx, rx = 16 - w * 0.22, 16 + w * 0.22
    if eyes == "round":
        for ex in (lx, rx):
            d.ellipse([P(ex - 2.2), P(ey - 2), P(ex + 2.2), P(ey + 2)], fill=0)
    elif eyes == "slant":
        sym_polygon(d, [(P(lx - 3), P(ey - 2)), (P(lx + 3), P(ey)), (P(lx + 2), P(ey + 2)), (P(lx - 2.5), P(ey + 1))], 0)
    elif eyes == "visor":
        d.rectangle([P(x0 + 3), P(ey - 1.5), P(x1 - 3), P(ey + 1.5)], fill=0)
    elif eyes == "narrow":
        for ex in (lx, rx):
            d.rectangle([P(ex - 3), P(ey - 1), P(ex + 3), P(ey + 1)], fill=0)
    elif eyes == "scope":
        d.ellipse([P(lx - 2), P(ey - 2), P(lx + 2), P(ey + 2)], fill=0)
        d.ellipse([P(rx - 3.2), P(ey - 3.2), P(rx + 3.2), P(ey + 3.2)], fill=0)
        dd.ellipse([P(rx - 4.2), P(ey - 4.2), P(rx + 4.2), P(ey + 4.2)], outline=255, width=S)
    elif eyes == "angry":
        sym_polygon(d, [(P(lx - 3), P(ey - 1)), (P(lx + 3), P(ey + 1)), (P(lx + 2), P(ey + 2.5)), (P(lx - 2.5), P(ey + 2))], 0)
        sym_polygon(dd, [(P(lx - 3.5), P(ey - 2.5)), (P(lx + 3.5), P(ey - 0.5)), (P(lx + 3.5), P(ey + 0.3)), (P(lx - 3.5), P(ey - 1.7))], 255)
    elif eyes == "wide":
        for ex in (lx, rx):
            d.ellipse([P(ex - 3), P(ey - 2.4), P(ex + 3), P(ey + 2.4)], fill=0)
    elif eyes == "tri":
        sym_polygon(d, [(P(lx - 3), P(ey - 2)), (P(lx + 3), P(ey - 2)), (P(lx), P(ey + 2.5))], 0)

    if mouth == "grille":
        for gx in (13, 15, 17, 19):
            d.rectangle([P(gx), P(y1 - 7), P(gx + 0.9), P(y1 - 2)], fill=0)
    elif mouth == "fangs":
        d.rectangle([P(12.5), P(y1 - 6), P(19.5), P(y1 - 4)], fill=0)
        for fx in (13.5, 18.5):
            d.polygon([(P(fx - 1), P(y1 - 6)), (P(fx + 1), P(y1 - 6)), (P(fx), P(y1 - 3))], fill=255)
    elif mouth == "slit":
        d.rectangle([P(14), P(y1 - 6), P(18), P(y1 - 5)], fill=0)

    # Nose ridge between the eyes, common to almost every Kanohi.
    if eyes not in ("visor",):
        dd.line([P(16), P(ey - 3), P(16), P(ey + 3)], fill=255, width=S)

    shape = shape.resize((N, N), Image.BOX).point(lambda v: 255 if v >= 128 else 0)
    detail = detail.resize((N, N), Image.BOX).point(lambda v: 255 if v >= 100 else 0)
    return shape, detail


def shade(shape, detail, size=N):
    """Turns a silhouette into a light grayscale texture that tints well."""
    px = shape.load()
    dt = detail.load()
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    o = out.load()
    for y in range(size):
        for x in range(size):
            if not px[x, y]:
                continue
            # distance to the nearest transparent pixel, capped at 3
            dist = 3
            for r in range(1, 4):
                found = False
                for dy in range(-r, r + 1):
                    for dx in range(-r, r + 1):
                        nx, ny = x + dx, y + dy
                        if not (0 <= nx < size and 0 <= ny < size) or not px[nx, ny]:
                            found = True
                            break
                    if found:
                        break
                if found:
                    dist = r - 1
                    break
            v = 168 + dist * 22
            # light from the top-left
            v += int(18 * (1 - (x + y) / (2 * size)))
            if dt[x, y]:
                v = 110
            v = max(60, min(255, v))
            o[x, y] = (v, v, v, 255)
    return out


def top_region(shape, fraction=0.30):
    bbox = shape.getbbox()
    if not bbox:
        return 0, 0
    top, bottom = bbox[1], bbox[3]
    return top, top + max(1, round((bottom - top) * fraction))


def pattern_value(pattern, x, y, rng_seed):
    rnd = random.Random(rng_seed * 1000 + y * 37 + x)
    if pattern == "flame":
        v = 0.55 + 0.45 * math.sin(x * 1.3 + math.sin(y * 0.9) * 2) * 0.5 + 0.3 * (1 - y / 12) + rnd.random() * 0.15
    elif pattern == "wave":
        v = 0.6 + 0.4 * math.sin(x * 0.8 + y * 1.6)
    elif pattern == "swirl":
        a = math.atan2(y - 4, x - 16)
        r = math.hypot(x - 16, y - 4)
        v = 0.6 + 0.4 * math.sin(a * 3 + r * 0.9)
    elif pattern == "rock":
        v = 0.55 + 0.35 * ((x // 3 * 7 + y // 2 * 13) % 5) / 4 + rnd.random() * 0.1
    elif pattern == "frost":
        v = 0.95 if (x + y) % 5 == 0 or (x - y) % 7 == 0 else 0.7 + rnd.random() * 0.1
    elif pattern == "spark":
        v = 1.0 if (x * 3 + (y % 4) * 2) % 9 in (0, 1) else 0.6 + rnd.random() * 0.15
    elif pattern == "shadow":
        v = 0.35 + 0.25 * rnd.random() + 0.15 * math.sin(x * 0.7 + y)
    elif pattern == "leaf":
        v = 0.9 if abs((x - 16) * 0.6 - (y % 5) + 2) < 0.8 or x == 16 else 0.6 + rnd.random() * 0.1
    else:  # crystal facets
        cx, cy = x // 4, y // 3
        facet = random.Random(rng_seed * 7 + cx * 31 + cy * 17).random()
        edge = x % 4 == 0 or y % 3 == 0
        v = 0.98 if edge else 0.55 + facet * 0.4
    return max(0.0, min(1.0, v))


def overlay(shape, detail, pattern, seed):
    out = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    top, cut = top_region(shape)
    px, dt, o = shape.load(), detail.load(), out.load()
    for y in range(top, cut):
        for x in range(N):
            if not px[x, y]:
                continue
            v = int(110 + 145 * pattern_value(pattern, x, y - top, seed))
            if dt[x, y]:
                v = int(v * 0.6)
            # darker rim keeps the silhouette readable
            edge = any(not (0 <= x + dx < N and 0 <= y + dy < N) or not px[x + dx, y + dy]
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                v = int(v * 0.75)
            o[x, y] = (v, v, v, 255)
    return out


def worn_texture(shape, detail):
    """Equipment texture (128x64, 2x the 64x32 armor layout): the mask covers the front of the head."""
    small_shape = shape.resize((16, 16), Image.BOX).point(lambda v: 255 if v >= 110 else 0)
    small_detail = detail.resize((16, 16), Image.BOX).point(lambda v: 255 if v >= 90 else 0)
    face = shade(small_shape, small_detail, 16)
    tex = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    tex.paste(face, (16, 16), face)
    return tex


# ---------------------------------------------------------------------------------------------
# Kanoka disks, blocks and GUIs
# ---------------------------------------------------------------------------------------------

def disk_base(rim=None):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    o = img.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r > 7.4 or r < 1.4:
                continue
            if r > 6.3:
                v = 150 if (int(math.degrees(math.atan2(y - 7.5, x - 7.5))) // 30) % 2 else 205
            elif r < 2.4:
                v = 140
            else:
                v = 225 - int(r * 6) + (12 if x + y < 14 else 0)
            if rim:
                if r > 6.3:
                    o[x, y] = rim + (255,)
                else:
                    o[x, y] = (v, v, v, 255)
            else:
                o[x, y] = (v, v, v, 255)
    return img


SYMBOLS = {
    "reconstitute": ((180, 60, 255), ["....#...", "...#.#..", ".....#..", "....#...", "...#....", "........", "...#....", "........"]),
    "freeze": ((150, 230, 255), ["...#....", ".#.#.#..", "..###...", "#######.", "..###...", ".#.#.#..", "...#....", "........"]),
    "weaken": ((120, 160, 90), ["....#...", "...#....", "..##....", "...##...", "....#...", "...#....", "..#.....", "........"]),
    "remove_poison": ((70, 220, 90), ["...#....", "...#....", "#######.", "...#....", "...#....", "...#....", "........", "........"]),
    "enlarge": ((255, 150, 40), ["...#....", "..###...", ".#.#.#..", "...#....", "...#....", ".#####..", "........", "........"]),
    "shrink": ((250, 230, 60), ["#.....#.", ".#...#..", "..#.#...", "...#....", "..#.#...", ".#...#..", "#.....#.", "........"]),
    "regenerate": ((240, 60, 70), [".##.##..", "#######.", "#######.", ".#####..", "..###...", "...#....", "........", "........"]),
    "teleport": ((230, 90, 230), ["..###...", ".#...#..", "#..#..#.", "#.#.#.#.", "#..#..#.", ".#...#..", "..###...", "........"]),
    "time": ((255, 220, 120), ["..###...", ".#.#.#..", "#..#..#.", "#..##.#.", "#.....#.", ".#...#..", "..###...", "........"]),
}


def symbol(power):
    color, rows = SYMBOLS[power]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    o = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "#":
                o[4 + x, 4 + y] = color + (255,)
    return img


def noise_tile(base, var, seed, accent=None):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    o = img.load()
    for y in range(16):
        for x in range(16):
            k = rnd.randint(-var, var)
            o[x, y] = tuple(max(0, min(255, c + k)) for c in base) + (255,)
            if x in (0, 15) or y in (0, 15):
                o[x, y] = tuple(max(0, c - 35) for c in base) + (255,)
    if accent:
        accent(img)
    return img


def gui(slots, big, extra=None, height=184):
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    w, h = 176, height
    d.rectangle([0, 0, w - 1, h - 1], fill=(198, 198, 198, 255))
    d.line([0, 0, w - 2, 0], fill=(255, 255, 255, 255), width=2)
    d.line([0, 0, 0, h - 2], fill=(255, 255, 255, 255), width=2)
    d.line([1, h - 1, w - 1, h - 1], fill=(85, 85, 85, 255), width=2)
    d.line([w - 1, 1, w - 1, h - 1], fill=(85, 85, 85, 255), width=2)

    def slot(x, y, size=18):
        d.rectangle([x - 1, y - 1, x + size - 2, y + size - 2], fill=(139, 139, 139, 255))
        d.line([x - 1, y - 1, x + size - 2, y - 1], fill=(55, 55, 55, 255))
        d.line([x - 1, y - 1, x - 1, y + size - 2], fill=(55, 55, 55, 255))
        d.line([x, y + size - 2, x + size - 2, y + size - 2], fill=(255, 255, 255, 255))
        d.line([x + size - 2, y, x + size - 2, y + size - 2], fill=(255, 255, 255, 255))

    for (x, y) in slots:
        slot(x, y)
    for (x, y) in big:
        slot(x - 4, y - 4, 26)
    inv_y = height - 82
    for r in range(3):
        for c in range(9):
            slot(8 + c * 18, inv_y + r * 18)
    for c in range(9):
        slot(8 + c * 18, inv_y + 58)
    if extra:
        extra(d)
    return img


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


# ---------------------------------------------------------------------------------------------
# JSON resources
# ---------------------------------------------------------------------------------------------

def item_model_simple(name, layers):
    write_json(ASSETS / "models/item" / f"{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {f"layer{i}": tex for i, tex in enumerate(layers)},
    })


def main():
    masks = read_masks()
    for sub in ("textures", "models", "items", "equipment", "blockstates", "lang"):
        shutil.rmtree(ASSETS / sub, ignore_errors=True)
    for sub in ("recipe", "loot_table", "tags"):
        shutil.rmtree(DATA / sub, ignore_errors=True)

    lang = {
        "itemGroup.bioniclemasks.kanohi": "Kanohi",
        "itemGroup.bioniclemasks.kanoka": "Kanoka",
        "key.categories.bioniclemasks.masks": "Kanohi Masks",
        "key.bioniclemasks.use_mask": "Use mask power",
        "key.bioniclemasks.cycle_mask": "Cycle Golden Kanohi power",
        "block.bioniclemasks.mask_forge": "Kanohi Mask Forge",
        "block.bioniclemasks.kanoka_forge": "Kanoka Disk Forge",
        "container.bioniclemasks.mask_forge": "Kanohi Mask Forge",
        "container.bioniclemasks.kanoka_forge": "Kanoka Disk Forge",
        "gui.bioniclemasks.forge": "Forge",
        "item.bioniclemasks.kanohi.named": "%1$s %2$s",
        "item.bioniclemasks.kanohi.nuva": "%1$s Nuva",
        "form.bioniclemasks.powerless": "Powerless",
        "form.bioniclemasks.noble": "Noble",
        "form.bioniclemasks.great": "Great",
        "form.bioniclemasks.nuva": "Nuva",
        "form.bioniclemasks.golden": "Golden",
        "form.bioniclemasks.legendary": "Legendary",
        "key.category.bioniclemasks.masks": "Kanohi Masks",
        "item.bioniclemasks.disk_of_time": "Disk of Time",
        "effect.bioniclemasks.enlarged": "Enlarged",
        "effect.bioniclemasks.shrunken": "Shrunken",
        "effect.bioniclemasks.reconstituted": "Reconstituted",
        "effect.bioniclemasks.crushing_gravity": "Crushing Gravity",
        "effect.bioniclemasks.light_gravity": "Light Gravity",
        "effect.bioniclemasks.time_slowed": "Time Slowed",
        "effect.bioniclemasks.time_echo": "Time Echo",
        "effect.bioniclemasks.mind_controlled": "Mind Controlled",
        "effect.bioniclemasks.silenced": "Silenced",
        "effect.bioniclemasks.visions": "Lost in Visions",
        "effect.bioniclemasks.fused": "Fused",
    }

    # --- Kanohi ---------------------------------------------------------------------------
    for i, mask in enumerate(masks):
        mid = mask["id"]
        shape, detail = draw_mask(mid)
        save(shade(shape, detail), ASSETS / "textures/item/kanohi" / f"{mid}.png")
        for j, pat in enumerate(PATTERNS):
            save(overlay(shape, detail, pat, i * 31 + j), ASSETS / "textures/item/kanohi/overlay" / f"{mid}_{pat}.png")
            item_model_simple(f"kanohi/overlay/{mid}_{pat}", [f"{MODID}:item/kanohi/overlay/{mid}_{pat}"])
        save(worn_texture(shape, detail), ASSETS / "textures/entity/equipment/humanoid" / f"kanohi_{mid}.png")
        item_model_simple(f"kanohi/{mid}", [f"{MODID}:item/kanohi/{mid}"])

        write_json(ASSETS / "equipment" / f"kanohi_{mid}.json", {
            "layers": {"humanoid": [{"texture": f"{MODID}:kanohi_{mid}", "dyeable": {"color_when_undyed": -1}}]}
        })
        write_json(ASSETS / "items" / f"kanohi_{mid}.json", {
            "model": {
                "type": "minecraft:composite",
                "models": [
                    {"type": "minecraft:model", "model": f"{MODID}:item/kanohi/{mid}",
                     "tints": [{"type": "minecraft:dye", "default": -1}]},
                    {"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": 0,
                     "cases": [{"when": pat, "model": {
                         "type": "minecraft:model", "model": f"{MODID}:item/kanohi/overlay/{mid}_{pat}",
                         "tints": [{"type": "minecraft:custom_model_data", "index": 0, "default": -1}]}}
                         for pat in PATTERNS],
                     "fallback": {"type": "minecraft:empty"}},
                ],
            }
        })
        lang[f"item.{MODID}.kanohi_{mid}"] = display_name(mask)

    # --- Kanoka -----------------------------------------------------------------------------
    save(disk_base(), ASSETS / "textures/item/kanoka/disk.png")
    save(disk_base(rim=(232, 190, 60)), ASSETS / "textures/item/kanoka/great_disk.png")
    save(disk_base(rim=(255, 215, 90)), ASSETS / "textures/item/kanoka/disk_of_time.png")
    for p in POWERS + ["time"]:
        save(symbol(p), ASSETS / "textures/item/kanoka/symbol" / f"{p}.png")
    for p in POWERS:
        item_model_simple(f"kanoka_{p}", [f"{MODID}:item/kanoka/disk", f"{MODID}:item/kanoka/symbol/{p}"])
        write_json(ASSETS / "items" / f"kanoka_{p}.json", {"model": {
            "type": "minecraft:model", "model": f"{MODID}:item/kanoka_{p}",
            "tints": [{"type": "minecraft:custom_model_data", "index": 0, "default": -1},
                      {"type": "minecraft:constant", "value": -1}]}})
        lang[f"item.{MODID}.kanoka_{p}"] = f"Kanoka Disk ({POWER_NAMES[p]})"
    for metru, (p, code) in GREAT_DISKS.items():
        name = f"great_disk_{metru}"
        item_model_simple(name, [f"{MODID}:item/kanoka/great_disk", f"{MODID}:item/kanoka/symbol/{p}"])
        write_json(ASSETS / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:item/{name}"}})
        lang[f"item.{MODID}.{name}"] = f"Great Disk of {METRU_NAMES[metru]}"
    item_model_simple("disk_of_time", [f"{MODID}:item/kanoka/disk_of_time", f"{MODID}:item/kanoka/symbol/time"])
    write_json(ASSETS / "items/disk_of_time.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:item/disk_of_time"}})

    # --- Blocks -----------------------------------------------------------------------------
    def forge_front(img):
        d = ImageDraw.Draw(img)
        d.rectangle([3, 8, 12, 13], fill=(40, 20, 15, 255))
        for x in range(4, 12):
            d.point((x, 11 + (x % 2)), fill=(255, 120 + x * 10, 30, 255))
            d.point((x, 10), fill=(255, 80, 20, 255))

    def press_front(img):
        d = ImageDraw.Draw(img)
        d.ellipse([3, 3, 12, 12], outline=(60, 60, 64, 255))
        d.ellipse([6, 6, 9, 9], fill=(70, 70, 76, 255))

    blocks = {
        "mask_forge": {
            "top": noise_tile((70, 62, 60), 10, 1, lambda im: ImageDraw.Draw(im).rectangle([4, 4, 11, 11], fill=(200, 80, 20, 255))),
            "side": noise_tile((88, 80, 78), 12, 2),
            "front": noise_tile((88, 80, 78), 12, 3, forge_front),
        },
        "kanoka_forge": {
            "top": noise_tile((150, 150, 155), 10, 4, press_front),
            "side": noise_tile((120, 110, 100), 10, 5),
            "front": noise_tile((120, 110, 100), 10, 6, press_front),
        },
    }
    for name, faces in blocks.items():
        for face, img in faces.items():
            save(img, ASSETS / "textures/block" / f"{name}_{face}.png")
        write_json(ASSETS / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/orientable",
            "textures": {"top": f"{MODID}:block/{name}_top", "side": f"{MODID}:block/{name}_side",
                         "front": f"{MODID}:block/{name}_front"}})
        write_json(ASSETS / "blockstates" / f"{name}.json", {"variants": {
            "facing=north": {"model": f"{MODID}:block/{name}"},
            "facing=east": {"model": f"{MODID}:block/{name}", "y": 90},
            "facing=south": {"model": f"{MODID}:block/{name}", "y": 180},
            "facing=west": {"model": f"{MODID}:block/{name}", "y": 270},
        }})
        write_json(ASSETS / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:block/{name}"}})
        write_json(DATA / "loot_table/blocks" / f"{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MODID}:{name}"}]}],
        })

    # --- GUIs (slot positions must match the menus) -------------------------------------------
    def arrow(d, x, y):
        d.polygon([(x, y + 3), (x + 14, y + 3), (x + 14, y), (x + 21, y + 7), (x + 14, y + 14), (x + 14, y + 11), (x, y + 11)],
                  fill=(139, 139, 139, 255))

    def flame(d):
        d.polygon([(18, 50), (22, 40), (26, 50), (24, 58), (20, 58)], fill=(139, 139, 139, 255))

    mask_slots = [(44 + c * 18, 20 + r * 18) for r in range(2) for c in range(3)] + [(62, 64), (14, 64)]
    save(gui(mask_slots, [(134, 38)], lambda d: (arrow(d, 104, 38), flame(d))), ASSETS / "textures/gui/mask_forge.png")
    kanoka_slots = [(26, 28), (26, 52), (56, 28), (56, 52)]
    save(gui(kanoka_slots, [(124, 40)], lambda d: arrow(d, 88, 40)), ASSETS / "textures/gui/kanoka_forge.png")

    # --- Recipes for the two forges -----------------------------------------------------------
    write_json(DATA / "recipe/mask_forge.json", {
        "type": "minecraft:crafting_shaped",
        "pattern": ["BIB", "BFB", "BLB"],
        "key": {"B": "minecraft:bricks", "I": "minecraft:iron_ingot", "F": "minecraft:blast_furnace", "L": "minecraft:lava_bucket"},
        "result": {"id": f"{MODID}:mask_forge"},
    })
    write_json(DATA / "recipe/kanoka_forge.json", {
        "type": "minecraft:crafting_shaped",
        "pattern": ["III", "SAS", "SSS"],
        "key": {"I": "minecraft:iron_ingot", "S": "minecraft:stone_bricks", "A": "minecraft:anvil"},
        "result": {"id": f"{MODID}:kanoka_forge"},
    })

    # --- Tags --------------------------------------------------------------------------------
    kanohi_items = [f"{MODID}:kanohi_{m['id']}" for m in masks]
    write_json(RES / "data/minecraft/tags/item/dyeable.json", {"values": kanohi_items})
    write_json(DATA / "tags/item/kanohi.json", {"values": kanohi_items})
    write_json(DATA / "tags/item/kanoka.json", {"values": [f"{MODID}:kanoka_{p}" for p in POWERS]
                                                 + [f"{MODID}:great_disk_{m}" for m in GREAT_DISKS] + [f"{MODID}:disk_of_time"]})
    write_json(DATA / "tags/item/forge_fuel.json", {"values": [
        "minecraft:coal", "minecraft:charcoal", "minecraft:coal_block", "minecraft:blaze_rod",
        "minecraft:blaze_powder", "minecraft:lava_bucket", "minecraft:fire_charge"]})
    write_json(RES / "data/minecraft/tags/block/mineable/pickaxe.json",
               {"values": [f"{MODID}:mask_forge", f"{MODID}:kanoka_forge"]})

    write_json(ASSETS / "lang/en_us.json", dict(sorted(lang.items())))
    print(f"Generated assets for {len(masks)} masks.")


if __name__ == "__main__":
    main()
