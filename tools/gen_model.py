#!/usr/bin/env python3
"""
Generates the Tiamut GeckoLib assets:
  - geo/tiamut.geo.json
  - textures/entity/tiamut.png + tiamut_glowmask.png
  - animations/tiamut.animation.json

Re-run after editing to regenerate:  python3 tools/gen_model.py
You can also open the .geo.json / .animation.json in Blockbench (GeckoLib plugin) and edit by hand.
"""
import json, math, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "tiamut")
TEX = 512

# ----------------------------------------------------------------------------------------------
# MODEL DEFINITION (units are 1/16 block; model faces -Z, +Y up)
# ----------------------------------------------------------------------------------------------
bones = {}      # name -> dict(parent, pivot, cubes[])
order = []


def bone(name, parent, pivot):
    bones[name] = {"name": name, "parent": parent, "pivot": pivot, "cubes": []}
    order.append(name)


def cube(b, origin, size, mat="skin"):
    bones[b]["cubes"].append({"origin": origin, "size": [int(s) for s in size], "mat": mat})


def spine(b, cx, ybase, zc, h, length, sweep=3, mat="spine", width=3):
    """Tapered, backward-swept dorsal spine built from three stacked boxes."""
    seg = max(1, h // 3)
    for i in range(3):
        ln = max(2, int(length * (1.0 - 0.3 * i)))
        z = zc - length / 2 + sweep * i
        cube(b, [cx - width / 2, ybase + seg * i, z], [width, seg, ln], mat)


# --- skeleton -----------------------------------------------------------------------------------
bone("root", None, [0, 28, 0])
bone("body", "root", [0, 28, 0])
cube("body", [-16, 12, -30], [32, 32, 64], "skin")
for z, h, ln in [(-22, 14, 12), (-6, 18, 14), (10, 16, 14), (26, 12, 12)]:
    spine("body", 0, 44, z, h, ln)

# neck (3 segments) + head
bone("neck1", "body", [0, 32, -28])
cube("neck1", [-11, 21, -54], [22, 22, 26])
spine("neck1", 0, 43, -41, 10, 10)
bone("neck2", "neck1", [0, 32, -54])
cube("neck2", [-9, 22, -80], [18, 20, 26])
spine("neck2", 0, 42, -67, 9, 10)
bone("neck3", "neck2", [0, 32, -80])
cube("neck3", [-7, 23, -106], [14, 18, 26])
spine("neck3", 0, 41, -93, 8, 9)

bone("head", "neck3", [0, 34, -106])
cube("head", [-9, 25, -126], [18, 18, 20])             # cranium
cube("head", [-6, 30, -148], [12, 9, 22])              # upper snout
cube("head", [-4, 38, -150], [8, 3, 8])                # nose bump
for z in (-146, -140, -134, -128):                     # upper teeth
    cube("head", [-6, 26, z], [2, 4, 2], "tooth")
    cube("head", [4, 26, z], [2, 4, 2], "tooth")
cube("head", [-5, 26, -149], [2, 5, 2], "tooth")       # front fangs
cube("head", [3, 26, -149], [2, 5, 2], "tooth")
cube("head", [9, 34, -122], [1, 4, 6], "eye")
cube("head", [-10, 34, -122], [1, 4, 6], "eye")
cube("head", [8, 36, -114], [4, 5, 14], "horn")       # swept horns
cube("head", [8, 38, -100], [3, 3, 12], "horn")
cube("head", [-12, 36, -114], [4, 5, 14], "horn")
cube("head", [-11, 38, -100], [3, 3, 12], "horn")
for z, h, ln in [(-122, 14, 8), (-114, 12, 8), (-107, 9, 8)]:   # head crest
    spine("head", 0, 43, z, h, ln, sweep=3, mat="horn")

bone("jaw", "head", [0, 29, -124])
cube("jaw", [-5, 24, -146], [10, 5, 22])
for z in (-144, -138, -132):
    cube("jaw", [-5, 29, z], [2, 3, 2], "tooth")
    cube("jaw", [3, 29, z], [2, 3, 2], "tooth")

# tail (4 segments + fluke)
bone("tail1", "body", [0, 27, 34])
cube("tail1", [-13, 14, 34], [26, 26, 36])
spine("tail1", 0, 40, 52, 14, 14)
bone("tail2", "tail1", [0, 27, 70])
cube("tail2", [-10, 16, 70], [20, 22, 38])
spine("tail2", 0, 38, 88, 12, 14)
cube("tail2", [-1, 8, 84], [2, 8, 16], "fin")                  # small ventral fin
bone("tail3", "tail2", [0, 27, 108])
cube("tail3", [-7, 18, 108], [14, 18, 38])
spine("tail3", 0, 36, 126, 10, 12)
bone("tail4", "tail3", [0, 27, 146])
cube("tail4", [-5, 20, 146], [10, 14, 36])
spine("tail4", 0, 34, 164, 8, 10)
bone("tail_fin", "tail4", [0, 27, 182])
cube("tail_fin", [-3, 22, 178], [6, 10, 12], "skin")
cube("tail_fin", [-1, 27, 184], [2, 22, 26], "fin")           # upper lobe
cube("tail_fin", [-1, 5, 184], [2, 22, 26], "fin")            # lower lobe
cube("tail_fin", [-1, 30, 210], [2, 12, 8], "fin")
cube("tail_fin", [-1, 6, 210], [2, 12, 8], "fin")

# flippers (front pair large, rear pair smaller)
for side, sx in (("l", 1), ("r", -1)):
    f = f"front_flipper_{side}"
    bone(f, "body", [14 * sx, 20, -14])
    cube(f, [14 if sx > 0 else -36, 18, -24], [22, 4, 20], "fin")
    ft = f + "_tip"
    bone(ft, f, [36 * sx, 20, -14])
    cube(ft, [36 if sx > 0 else -56, 19, -21], [20, 3, 14], "fin")
    b = f"back_flipper_{side}"
    bone(b, "body", [14 * sx, 16, 22])
    cube(b, [14 if sx > 0 else -32, 14, 15], [18, 4, 16], "fin")
    bt = b + "_tip"
    bone(bt, b, [32 * sx, 16, 23])
    cube(bt, [32 if sx > 0 else -46, 15, 17], [14, 3, 12], "fin")

# --- UV packing ---------------------------------------------------------------------------------
allcubes = [c for b in order for c in bones[b]["cubes"]]
for c in allcubes:
    w, h, d = c["size"]
    c["tw"], c["th"] = 2 * d + 2 * w, d + h
rows = sorted(allcubes, key=lambda c: -c["th"])
x = y = rowh = 0
for c in rows:
    if x + c["tw"] > TEX:
        x, y, rowh = 0, y + rowh + 1, 0
    c["uv"] = [x, y]
    x += c["tw"] + 1
    rowh = max(rowh, c["th"])
assert y + rowh <= TEX, "texture too small"

# --- write geo ----------------------------------------------------------------------------------
geo_bones = []
for name in order:
    b = bones[name]
    entry = {"name": name, "pivot": b["pivot"]}
    if b["parent"]:
        entry["parent"] = b["parent"]
    if b["cubes"]:
        entry["cubes"] = [{"origin": c["origin"], "size": c["size"], "uv": c["uv"]} for c in b["cubes"]]
    geo_bones.append(entry)

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {
            "identifier": "geometry.tiamut",
            "texture_width": TEX, "texture_height": TEX,
            "visible_bounds_width": 30, "visible_bounds_height": 8,
            "visible_bounds_offset": [0, 1.5, 0],
        },
        "bones": geo_bones,
    }],
}
os.makedirs(os.path.join(ROOT, "geo"), exist_ok=True)
with open(os.path.join(ROOT, "geo", "tiamut.geo.json"), "w") as f:
    json.dump(geo, f, indent=1)

# --- texture ------------------------------------------------------------------------------------
rnd = random.Random(7)
img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
px, gx = img.load(), glow.load()


def h2(a, b, s=0):
    n = (a * 374761393 + b * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(col, k):
    return tuple(max(0, min(255, int(c * k))) for c in col)


# palette: abyssal teal-blue back, slate flanks, pale belly
BACK = (16, 44, 56)
FLANK = (36, 82, 98)
BELLY = (192, 202, 188)
STRIPE = (8, 22, 30)


def paint_face(c, face, rx, ry, rw, rh):
    mat = c["mat"]
    for j in range(rh):
        for i in range(rw):
            X, Y = rx + i, ry + j
            t = j / max(1, rh - 1)
            if mat == "skin":
                if face == "up":
                    col = BACK
                elif face == "down":
                    col = BELLY
                else:
                    if t < 0.45:
                        col = lerp(BACK, FLANK, t / 0.45)
                    elif t < 0.75:
                        col = FLANK
                    else:
                        col = lerp(FLANK, BELLY, (t - 0.75) / 0.25)
                    band = h2(i // 7, 3, 11)
                    if t < 0.62 and band > 0.55:
                        col = lerp(col, STRIPE, 0.55 * (band - 0.55) / 0.45 + 0.15)
                cell = ((X + ((Y // 5) % 2) * 2) // 4, Y // 5)
                k = 0.88 + 0.24 * h2(cell[0], cell[1], 5)
                if (X + ((Y // 5) % 2) * 2) % 4 == 0 or Y % 5 == 0:
                    k *= 0.86
                col = shade(col, k)
            elif mat == "spine":
                col = lerp((10, 20, 28), (34, 96, 112), t if face not in ("up", "down") else 0.5)
                col = shade(col, 0.9 + 0.2 * h2(X, Y, 2))
                if face in ("north", "south", "east", "west") and rw > 2 and abs(i - rw // 2) <= 0 and 0.2 < t < 0.9:
                    gx[X, Y] = (70, 235, 255, 210)
            elif mat == "fin":
                base = lerp((20, 58, 72), (88, 132, 138), 1.0 if face == "down" else 0.0 if face == "up" else t)
                rib = 0.8 if (i % 6 == 0 and face in ("up", "down")) else 1.0
                col = shade(base, rib * (0.92 + 0.16 * h2(X, Y, 9)))
            elif mat == "horn":
                col = lerp((34, 40, 42), (176, 170, 150), t * 0.6 if face not in ("up", "down") else 0.25)
                col = shade(col, 0.9 + 0.2 * h2(X // 2, Y // 2, 4))
            elif mat == "tooth":
                col = shade((232, 226, 206), 0.9 + 0.12 * h2(X, Y, 6))
            elif mat == "eye":
                col = (255, 196, 34)
                gx[X, Y] = (255, 214, 70, 255)
                if face in ("east", "west", "north", "south") and rw > 1 and rh > 2 and abs(j - rh // 2) == 0 and abs(i - rw // 2) <= 1:
                    col = (20, 8, 4)   # slit pupil
                    gx[X, Y] = (0, 0, 0, 0)
            else:
                col = (128, 128, 128)
            px[X, Y] = (col[0], col[1], col[2], 255)


for c in allcubes:
    w, h, d = c["size"]
    u, v = c["uv"]
    faces = {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + 2 * d + w, v + d, w, h),
    }
    for face, (rx, ry, rw, rh) in faces.items():
        paint_face(c, face, rx, ry, rw, rh)

os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
img.save(os.path.join(ROOT, "textures", "entity", "tiamut.png"))
glow.save(os.path.join(ROOT, "textures", "entity", "tiamut_glowmask.png"))

# --------------------------------------------------------------------------------------------------
# ANIMATIONS
# Rotation conventions follow Blockbench/Bedrock: +X on a bone tilts its front DOWN.
# --------------------------------------------------------------------------------------------------
STEP = 0.05


def key(t):
    return f"{round(float(t), 4)}"


def smooth(x):
    x = max(0.0, min(1.0, x))
    return x * x * (3 - 2 * x)


def env(t, a, b, c, d):
    """0 before a, smooth rise to 1 at b, hold until c, smooth fall to 0 at d."""
    if t < a or t > d:
        return 0.0
    if t < b:
        return smooth((t - a) / (b - a))
    if t <= c:
        return 1.0
    return 1.0 - smooth((t - c) / (d - c))


class Anim:
    def __init__(self, length, loop):
        self.length, self.loop = length, loop
        self.tracks = {}

    def add(self, bone_name, channel, fn):
        """fn(t) -> [x,y,z]; sampled over the whole animation."""
        assert bone_name in bones, bone_name
        n = int(round(self.length / STEP))
        frames = {}
        for i in range(n + 1):
            t = min(i * STEP, self.length)
            frames[key(t)] = [round(v, 3) for v in fn(t)]
        if self.loop:
            frames[key(self.length)] = frames[key(0)]
        self.tracks.setdefault(bone_name, {})[channel] = frames

    def json(self):
        return {"loop": self.loop, "animation_length": self.length, "bones": self.tracks}


def wave(T, amp, phase=0.0):
    return lambda t: amp * math.sin(2 * math.pi * t / T - phase)


anims = {}
TAIL = ["tail1", "tail2", "tail3", "tail4", "tail_fin"]
NECK = ["neck1", "neck2", "neck3", "head"]


def swim_cycle(name, T, tail_amp, lag, neck_amp, neck_lag, flip_amp, body_sway, bob, lean=0.0, fin_pitch=0.0):
    a = Anim(T, True)
    w = 2 * math.pi / T
    for i, bn in enumerate(TAIL):
        a.add(bn, "rotation", lambda t, i=i: [fin_pitch * math.sin(w * t - lag * i - 1.2) if bn == "tail_fin" else 0,
                                              tail_amp[i] * math.sin(w * t - lag * i), 0])
    for i, bn in enumerate(NECK):
        a.add(bn, "rotation", lambda t, i=i: [lean * (0.4 if i == 3 else 0.25),
                                              neck_amp[i] * math.sin(w * t + math.pi + neck_lag * i), 0])
    a.add("body", "rotation", lambda t: [0, body_sway * math.sin(w * t - 0.2), body_sway * 0.6 * math.sin(w * t - 1.0)])
    a.add("body", "position", lambda t: [0, bob * math.sin(2 * w * t), 0])
    for side, s in (("l", 1), ("r", -1)):
        a.add(f"front_flipper_{side}", "rotation", lambda t, s=s: [0, 0, s * flip_amp * math.sin(w * t)])
        a.add(f"front_flipper_{side}_tip", "rotation", lambda t, s=s: [0, 0, s * flip_amp * 0.6 * math.sin(w * t - 0.9)])
        a.add(f"back_flipper_{side}", "rotation", lambda t, s=s: [0, 0, s * flip_amp * 0.5 * math.sin(w * t - 1.6)])
        a.add(f"back_flipper_{side}_tip", "rotation", lambda t, s=s: [0, 0, s * flip_amp * 0.4 * math.sin(w * t - 2.4)])
    a.add("jaw", "rotation", lambda t: [2.0 + 1.5 * math.sin(w * t * 0.5), 0, 0])
    anims["animation.tiamut." + name] = a.json()


# calm hovering / breathing
swim_cycle("idle", 5.0, [3, 5, 7, 9, 13], 0.5, [1.5, 2.5, 3.0, 3.5], 0.4, 7, 0.8, 0.5, lean=0, fin_pitch=3)
# cruising
swim_cycle("swim", 2.4, [6, 9, 12, 16, 24], 0.65, [3, 4, 5, 6], 0.5, 18, 2.0, 0.6, lean=2, fin_pitch=8)
# fast swim: rapid, large amplitude, streamlined, neck stretched forward, flippers tucked to a tiny flutter
swim_cycle("swim_fast", 0.85, [8, 13, 18, 24, 36], 0.85, [2, 3, 3, 4], 0.6, 8, 2.5, 0.9, lean=6, fin_pitch=14)

# beached: slow heavy flop
b = Anim(1.8, True)
w = 2 * math.pi / 1.8
for i, bn in enumerate(TAIL):
    b.add(bn, "rotation", lambda t, i=i: [0, [10, 14, 18, 22, 28][i] * math.sin(w * t - 0.7 * i), 0])
b.add("body", "rotation", lambda t: [0, 0, 4 * math.sin(w * t)])
for i, bn in enumerate(NECK):
    b.add(bn, "rotation", lambda t, i=i: [5 + 2 * i, 6 * math.sin(w * t + 0.6 * i), 0])
b.add("jaw", "rotation", lambda t: [10 + 4 * math.sin(w * t * 2), 0, 0])
for side, s in (("l", 1), ("r", -1)):
    b.add(f"front_flipper_{side}", "rotation", lambda t, s=s: [0, 0, s * 22 * math.sin(w * t)])
anims["animation.tiamut.beached"] = b.json()

# bite
T = 0.9
a = Anim(T, False)


def pull(t): return env(t, 0.0, 0.15, 0.25, 0.35)
def lunge(t): return env(t, 0.25, 0.4, 0.45, 0.7)
def jawo(t): return env(t, 0.1, 0.28, 0.4, 0.52)


a.add("neck1", "rotation", lambda t: [-8 * pull(t) + 14 * lunge(t), 0, 0])
a.add("neck2", "rotation", lambda t: [-6 * pull(t) + 10 * lunge(t), 0, 0])
a.add("neck3", "rotation", lambda t: [-5 * pull(t) + 8 * lunge(t), 0, 0])
a.add("head", "rotation", lambda t: [-10 * pull(t) + 8 * lunge(t), 0, 0])
a.add("jaw", "rotation", lambda t: [42 * jawo(t), 0, 0])
a.add("root", "rotation", lambda t: [2.5 * lunge(t), 0, 0])
anims["animation.tiamut.bite"] = a.json()

# roar_short ------------------------------------------------------------------------------------
T = 2.4
a = Anim(T, False)
E = lambda t: env(t, 0, 0.45, 1.9, 2.4)
J = lambda t: env(t, 0.1, 0.45, 1.9, 2.3)
a.add("neck1", "rotation", lambda t: [-10 * E(t), 0, 0])
a.add("neck2", "rotation", lambda t: [-14 * E(t), 0, 0])
a.add("neck3", "rotation", lambda t: [-12 * E(t), 0, 0])
a.add("head", "rotation", lambda t: [-10 * E(t), 4 * math.sin(2 * math.pi * 3 * t) * E(t), 0])
a.add("jaw", "rotation", lambda t: [40 * J(t) + 3 * math.sin(2 * math.pi * 14 * t) * J(t), 0, 0])
a.add("root", "rotation", lambda t: [-3 * E(t), 0, 0])
a.add("body", "scale", lambda t: [1 + 0.04 * E(t)] * 3)
a.add("front_flipper_l", "rotation", lambda t: [0, 0, 25 * E(t)])
a.add("front_flipper_r", "rotation", lambda t: [0, 0, -25 * E(t)])
anims["animation.tiamut.roar_short"] = a.json()

# roar_long (reared-up bellow) ------------------------------------------------------------------
T = 4.0
a = Anim(T, False)
E = lambda t: env(t, 0, 0.8, 3.2, 4.0)
J = lambda t: env(t, 0.2, 0.8, 3.3, 3.9)
a.add("neck1", "rotation", lambda t: [-16 * E(t), 12 * math.sin(2 * math.pi * 0.5 * t) * E(t), 0])
a.add("neck2", "rotation", lambda t: [-20 * E(t), 14 * math.sin(2 * math.pi * 0.5 * t - 0.5) * E(t), 0])
a.add("neck3", "rotation", lambda t: [-16 * E(t), 10 * math.sin(2 * math.pi * 0.5 * t - 1.0) * E(t), 0])
a.add("head", "rotation", lambda t: [-14 * E(t), 6 * math.sin(2 * math.pi * 2 * t) * E(t), 0])
a.add("jaw", "rotation", lambda t: [46 * J(t) + 4 * math.sin(2 * math.pi * 16 * t) * J(t), 0, 0])
a.add("root", "rotation", lambda t: [-9 * E(t), 0, 0])
a.add("body", "scale", lambda t: [1 + 0.05 * E(t)] * 3)
a.add("front_flipper_l", "rotation", lambda t: [0, 0, 35 * E(t)])
a.add("front_flipper_r", "rotation", lambda t: [0, 0, -35 * E(t)])
a.add("front_flipper_l_tip", "rotation", lambda t: [0, 0, 20 * E(t)])
a.add("front_flipper_r_tip", "rotation", lambda t: [0, 0, -20 * E(t)])
anims["animation.tiamut.roar_long"] = a.json()

# roar_challenge (bow, snap up, double bellow, head shake) ---------------------------------------
T = 3.2
a = Anim(T, False)
bow = lambda t: env(t, 0, 0.2, 0.3, 0.5)
up = lambda t: env(t, 0.35, 0.7, 2.6, 3.2)
jw = lambda t: max(env(t, 0.45, 0.7, 1.3, 1.5), env(t, 1.6, 1.8, 2.7, 3.0))
sh = lambda t: env(t, 1.7, 2.0, 2.6, 3.0)
a.add("neck1", "rotation", lambda t: [12 * bow(t) - 14 * up(t), 0, 0])
a.add("neck2", "rotation", lambda t: [10 * bow(t) - 18 * up(t), 0, 0])
a.add("neck3", "rotation", lambda t: [8 * bow(t) - 14 * up(t), 8 * math.sin(2 * math.pi * 2.5 * t) * sh(t), 0])
a.add("head", "rotation", lambda t: [6 * bow(t) - 12 * up(t), 12 * math.sin(2 * math.pi * 2.5 * t) * sh(t), 0])
a.add("jaw", "rotation", lambda t: [44 * jw(t) + 3 * math.sin(2 * math.pi * 15 * t) * jw(t), 0, 0])
a.add("root", "rotation", lambda t: [-8 * up(t), 0, 0])
a.add("body", "scale", lambda t: [1 + 0.05 * up(t)] * 3)
a.add("front_flipper_l", "rotation", lambda t: [0, 0, 30 * up(t)])
a.add("front_flipper_r", "rotation", lambda t: [0, 0, -30 * up(t)])
anims["animation.tiamut.roar_challenge"] = a.json()

os.makedirs(os.path.join(ROOT, "animations"), exist_ok=True)
with open(os.path.join(ROOT, "animations", "tiamut.animation.json"), "w") as f:
    json.dump({"format_version": "1.8.0", "animations": anims}, f, separators=(",", ":"))

print("cubes:", len(allcubes), "bones:", len(order), "anims:", list(anims))
