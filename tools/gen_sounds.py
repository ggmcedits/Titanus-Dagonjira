#!/usr/bin/env python3
"""Synthesizes three kaiju roars (mono .ogg) into assets/tiamut/sounds/. Needs numpy, scipy, ffmpeg."""
import os, subprocess, tempfile
import numpy as np
from scipy import signal
from scipy.io import wavfile

FS = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "tiamut", "sounds")
os.makedirs(OUT, exist_ok=True)


def smooth_curve(points, n):
    """points: [(t_norm, value)] -> smooth array of length n."""
    xs, ys = zip(*points)
    t = np.linspace(0, 1, n)
    y = np.interp(t, xs, ys)
    k = int(FS * 0.02)
    return np.convolve(y, np.ones(k) / k, mode="same")


def formant(x, centers, bw, gains):
    out = np.zeros_like(x)
    for c, b, g in zip(centers, bw, gains):
        sos = signal.butter(2, [max(20, c - b), min(FS / 2 - 100, c + b)], btype="band", fs=FS, output="sos")
        out += g * signal.sosfilt(sos, x)
    return out


def roar(duration, lead, pitch_pts, vib=5.0, growl=32.0, growl_depth=0.5, breath=0.35, drive=3.0,
         env_pts=None, seed=1, f_open=(500, 1100, 2300), f_close=(350, 800, 1900), reverb=1.4):
    rng = np.random.default_rng(seed)
    n = int(duration * FS)
    nl = int(lead * FS)
    m = n - nl
    t = np.arange(m) / FS
    f = smooth_curve(pitch_pts, m) * (1 + 0.025 * np.sin(2 * np.pi * vib * t))
    f *= 1 + 0.01 * rng.standard_normal(m).cumsum() / np.sqrt(m) * 8
    phase = 2 * np.pi * np.cumsum(f) / FS
    src = np.zeros(m)
    for h in range(1, 36):
        a = 1.0 / h ** 0.85
        src += a * np.sin(h * phase + rng.uniform(0, 2 * np.pi))
        src += 0.3 * a * np.sin((h + 0.5) * phase * 0.5 + rng.uniform(0, 2 * np.pi))   # sub-harmonic roughness
    src *= 1 - growl_depth * (0.5 + 0.5 * np.sin(2 * np.pi * growl * t + 2.0 * np.sin(2 * np.pi * 3.1 * t)))
    noise = rng.standard_normal(m)
    nb = signal.sosfilt(signal.butter(2, [250, 3500], btype="band", fs=FS, output="sos"), noise)
    sig = src / np.max(np.abs(src)) + breath * nb / np.max(np.abs(nb))
    # vowel morph: open "aah" -> closed "ooo"
    a_open = formant(sig, f_open, (150, 200, 300), (1.0, 0.7, 0.35))
    a_close = formant(sig, f_close, (120, 160, 240), (1.0, 0.6, 0.25))
    mix = smooth_curve([(0, 0), (0.35, 0), (0.75, 1), (1, 1)], m)
    sig = a_open * (1 - mix) + a_close * mix
    sig += 0.8 * np.sin(phase * 0.5)                      # sub bass
    env = smooth_curve(env_pts, m)
    sig = np.tanh(drive * sig / np.max(np.abs(sig))) * env
    out = np.concatenate([np.zeros(nl), sig])
    # simple cavernous reverb
    ir_n = int(reverb * FS)
    ir = rng.standard_normal(ir_n) * np.exp(-np.arange(ir_n) / (FS * reverb / 4.5))
    ir = signal.sosfilt(signal.butter(2, 2500, btype="low", fs=FS, output="sos"), ir)
    wet = signal.fftconvolve(out, ir)[: n + ir_n // 2]
    dry = np.concatenate([out, np.zeros(len(wet) - len(out))])
    res = dry + 0.22 * wet / np.max(np.abs(wet)) * np.max(np.abs(dry))
    res = res[: n + int(0.4 * FS)]
    res *= np.linspace(1, 0, len(res)) ** 0.15 * np.clip(np.linspace(8, 0, len(res)) + 0.0, 0, 1) ** 0 
    fade = int(0.25 * FS)
    res[-fade:] *= np.linspace(1, 0, fade)
    res = res / np.max(np.abs(res)) * 0.89
    return res


def save(name, data):
    with tempfile.TemporaryDirectory() as td:
        wav = os.path.join(td, name + ".wav")
        wavfile.write(wav, FS, (data * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
                        os.path.join(OUT, name + ".ogg")], check=True)
    print(name, round(len(data) / FS, 2), "s")


# Each sound has ~0.3s of lead-in so the jaw animation (opens ~0.3-0.45s) lines up with the voice.
# roar_short  (anim 2.4s)
save("roar_short", roar(2.4, 0.3, [(0, 55), (0.15, 120), (0.5, 150), (0.8, 105), (1, 48)],
                         env_pts=[(0, 0), (0.08, 1), (0.7, 0.95), (1, 0)], seed=3, vib=6))
# roar_long   (anim 4.0s): deep rising bellow with a tremble
save("roar_long", roar(4.0, 0.4, [(0, 42), (0.2, 70), (0.45, 130), (0.7, 160), (0.9, 90), (1, 38)],
                        vib=4.0, growl=26, growl_depth=0.6, breath=0.42, drive=3.6,
                        env_pts=[(0, 0), (0.1, 0.8), (0.4, 1), (0.8, 0.9), (1, 0)], seed=11, reverb=2.0,
                        f_open=(450, 950, 2100), f_close=(300, 700, 1700)))
# roar_challenge (anim 3.2s): sharp, two-part bellow (matches the double jaw-open)
save("roar_challenge", roar(3.2, 0.45, [(0, 70), (0.1, 190), (0.3, 170), (0.42, 90), (0.5, 160), (0.8, 200), (1, 70)],
                             vib=7.5, growl=38, growl_depth=0.55, breath=0.5, drive=4.2,
                             env_pts=[(0, 0), (0.04, 1), (0.38, 0.9), (0.44, 0.15), (0.5, 1), (0.85, 0.85), (1, 0)],
                             seed=21, reverb=1.6, f_open=(600, 1300, 2600), f_close=(420, 950, 2100)))
