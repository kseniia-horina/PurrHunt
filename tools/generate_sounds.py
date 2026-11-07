#!/usr/bin/env python3
"""Procedural sound synthesis for Purr Hunt.

Generates every effect and ambience loop as 16-bit mono WAV, 44.1 kHz, into app/src/main/res/raw/.
Only the Python standard library is used, and every random source is seeded, so running the script
again reproduces the same files byte for byte.

    python tools/generate_sounds.py            # all sounds
    python tools/generate_sounds.py mouse_     # only names starting with "mouse_"

To replace a synthesised sound with a recording, see docs/SOUNDS.md.
"""

import math
import os
import random
import struct
import sys
import wave

RATE = 44100
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "raw")
TWO_PI = 2.0 * math.pi


# --------------------------------------------------------------------------- primitives

def silence(seconds):
    return [0.0] * int(seconds * RATE)


def mix_into(dst, src, at=0.0, gain=1.0):
    """Adds src into dst starting at `at` seconds, growing dst if needed."""
    start = int(at * RATE)
    end = start + len(src)
    if end > len(dst):
        dst.extend([0.0] * (end - len(dst)))
    for i, v in enumerate(src):
        dst[start + i] += v * gain
    return dst


def envelope(n, attack, release, shape=2.0):
    """Attack/release envelope over n samples, times in seconds."""
    a = max(1, int(attack * RATE))
    r = max(1, int(release * RATE))
    out = []
    for i in range(n):
        if i < a:
            e = i / a
        elif i > n - r:
            e = max(0.0, (n - i) / r)
        else:
            e = 1.0
        out.append(e ** shape if shape != 1.0 else e)
    return out


def exp_decay(n, tau):
    return [math.exp(-i / (tau * RATE)) for i in range(n)]


def sweep(f0, f1, seconds, curve=1.0, vibrato=0.0, vib_rate=0.0, harmonics=((1, 1.0),)):
    """Sine sweep from f0 to f1 with optional vibrato and harmonics."""
    n = int(seconds * RATE)
    phase = 0.0
    out = []
    for i in range(n):
        t = i / n
        f = f0 + (f1 - f0) * (t ** curve)
        if vibrato:
            f *= 1.0 + vibrato * math.sin(TWO_PI * vib_rate * i / RATE)
        phase += TWO_PI * f / RATE
        out.append(sum(a * math.sin(phase * k) for k, a in harmonics))
    return out


def noise(seconds, rnd):
    return [rnd.uniform(-1.0, 1.0) for _ in range(int(seconds * RATE))]


def pink(seconds, rnd):
    """Paul Kellet's economy pink noise filter."""
    b0 = b1 = b2 = 0.0
    out = []
    for _ in range(int(seconds * RATE)):
        w = rnd.uniform(-1.0, 1.0)
        b0 = 0.99765 * b0 + w * 0.0990460
        b1 = 0.96300 * b1 + w * 0.2965164
        b2 = 0.57000 * b2 + w * 1.0526913
        out.append((b0 + b1 + b2 + w * 0.1848) * 0.2)
    return out


def brown(seconds, rnd):
    v = 0.0
    out = []
    for _ in range(int(seconds * RATE)):
        v = (v + rnd.uniform(-1.0, 1.0) * 0.02) * 0.998
        out.append(v * 3.0)
    return out


def biquad(samples, kind, freq, q=0.707):
    """RBJ cookbook biquad: 'lp', 'hp' or 'bp'."""
    w0 = TWO_PI * freq / RATE
    alpha = math.sin(w0) / (2 * q)
    cos_w0 = math.cos(w0)
    if kind == "lp":
        b0, b1, b2 = (1 - cos_w0) / 2, 1 - cos_w0, (1 - cos_w0) / 2
    elif kind == "hp":
        b0, b1, b2 = (1 + cos_w0) / 2, -(1 + cos_w0), (1 + cos_w0) / 2
    else:
        b0, b1, b2 = alpha, 0.0, -alpha
    a0, a1, a2 = 1 + alpha, -2 * cos_w0, 1 - alpha
    b0, b1, b2, a1, a2 = b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0
    x1 = x2 = y1 = y2 = 0.0
    out = []
    for x in samples:
        y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, x, y1, y
        out.append(y)
    return out


def mul(a, b):
    return [x * y for x, y in zip(a, b)]


def gain(samples, g):
    return [x * g for x in samples]


def normalize(samples, peak=0.89):
    m = max((abs(x) for x in samples), default=0.0)
    if m == 0:
        return samples
    return [x * peak / m for x in samples]


def fade_edges(samples, seconds=0.004):
    n = len(samples)
    k = min(n // 2, int(seconds * RATE))
    for i in range(k):
        e = i / k
        samples[i] *= e
        samples[n - 1 - i] *= e
    return samples


def seamless(samples, crossfade):
    """Turns a buffer into a seamless loop: its tail is crossfaded into its head."""
    k = int(crossfade * RATE)
    body = samples[:-k]
    tail = samples[-k:]
    for i in range(k):
        e = i / k
        body[i] = body[i] * e + tail[i] * (1 - e)
    return body


def write(name, samples, peak=0.89):
    os.makedirs(OUT_DIR, exist_ok=True)
    data = normalize(samples, peak)
    path = os.path.join(OUT_DIR, name + ".wav")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1.0, min(1.0, x)) * 32767)) for x in data))
    print(f"  {name}.wav  {len(data) / RATE:5.2f} s")


# --------------------------------------------------------------------------- voices

def squeak(f0, f1, dur, vib=0.02, rough=0.0, rnd=None):
    """Rodent squeak: a fast sine sweep with a slight vibrato; `rough` adds a noisy, lower rat voice."""
    s = sweep(f0, f1, dur, curve=0.7, vibrato=vib, vib_rate=38.0, harmonics=((1, 1.0), (2, 0.18), (3, 0.05)))
    s = mul(s, envelope(len(s), 0.008, dur * 0.5, 1.5))
    if rough and rnd:
        n = biquad(noise(dur, rnd), "bp", (f0 + f1) / 2, 2.0)
        am = [1.0 + 0.6 * math.sin(TWO_PI * 70 * i / RATE) for i in range(len(s))]
        s = [a * m + b * rough * 3.0 for a, m, b in zip(s, am, n)]
        s = mul(s, envelope(len(s), 0.008, dur * 0.5, 1.5))
    return s


def chirp_note(f0, f1, dur, index=0.0, mod_ratio=2.0):
    """Short FM bird note: carrier sweep f0→f1 with modulation index."""
    n = int(dur * RATE)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / n
        f = f0 + (f1 - f0) * t
        phase += TWO_PI * f / RATE
        mod = index * math.sin(phase * mod_ratio)
        out.append(math.sin(phase + mod))
    return mul(out, envelope(n, 0.006, dur * 0.6, 1.2))


def bubble(f0, f1, dur):
    n = int(dur * RATE)
    s = sweep(f0, f1, dur, curve=0.5)
    return mul(s, [math.exp(-3.5 * i / n) * min(1.0, i / (0.003 * RATE)) for i in range(n)])


def noise_burst(rnd, dur, lo, hi, tau):
    n = noise(dur, rnd)
    n = biquad(biquad(n, "hp", lo), "lp", hi)
    return mul(n, exp_decay(len(n), tau))


# --------------------------------------------------------------------------- effects

def make_effects(only):
    rnd = random.Random(2024)

    def out(name, samples, peak=0.89):
        if name.startswith(only):
            write(name, fade_edges(samples), peak)

    # Mouse: fast 3–8 kHz sweeps, 60–150 ms, four variants.
    out("mouse_squeak_1", squeak(3500, 7600, 0.09))
    out("mouse_squeak_2", squeak(4300, 6200, 0.13))
    double = squeak(5000, 8000, 0.065)
    mix_into(double, squeak(5200, 7400, 0.06), at=0.085)
    out("mouse_squeak_3", double)
    out("mouse_squeak_4", squeak(3200, 6800, 0.15, vib=0.04))

    for i, count in enumerate((7, 10), start=1):
        s = silence(0.32)
        for k in range(count):
            mix_into(s, noise_burst(rnd, 0.018, 1800, 6500, 0.005), at=k * 0.03 + rnd.uniform(0, 0.012), gain=rnd.uniform(0.5, 1.0))
        out(f"mouse_rustle_{i}", s, 0.6)

    caught = silence(0.45)
    for k, (a, b) in enumerate(((5200, 8200), (5600, 8600), (6000, 7400))):
        mix_into(caught, squeak(a, b, 0.11, vib=0.06), at=k * 0.12)
    out("mouse_caught", caught)

    # Rat: lower and rougher.
    out("rat_squeak_1", squeak(1600, 3400, 0.16, rough=0.35, rnd=rnd))
    out("rat_squeak_2", squeak(2200, 2900, 0.2, vib=0.05, rough=0.45, rnd=rnd))
    rat = silence(0.5)
    mix_into(rat, squeak(2400, 4200, 0.22, vib=0.08, rough=0.5, rnd=rnd))
    mix_into(rat, squeak(2800, 3600, 0.2, vib=0.08, rough=0.5, rnd=rnd), at=0.24)
    out("rat_caught", rat)

    # Birds: each species has its own pattern.
    for i, notes in enumerate((
        ((4800, 3600, 0.06), (5000, 3800, 0.055), (4600, 3500, 0.07)),
        ((4400, 3300, 0.07), (4900, 4100, 0.05)),
    ), start=1):
        s = silence(0.4)
        t = 0.0
        for f0, f1, d in notes:
            mix_into(s, chirp_note(f0, f1, d, index=0.8, mod_ratio=3.0), at=t)
            t += d + 0.04
        out(f"sparrow_chirp_{i}", s)

    for i, reps in enumerate((3, 2), start=1):
        s = silence(0.7)
        t = 0.0
        for _ in range(reps):
            mix_into(s, chirp_note(5600, 5300, 0.09, index=0.3), at=t)
            mix_into(s, chirp_note(3900, 3700, 0.1, index=0.3), at=t + 0.11)
            t += 0.24
        out(f"tit_trill_{i}", s)

    bull = chirp_note(1850, 1500, 0.26, index=0.1)
    mix_into(bull, chirp_note(1500, 1420, 0.18, index=0.1), at=0.3)
    out("bullfinch_call", bull, 0.7)

    flap = silence(0.35)
    for k in range(6):
        mix_into(flap, noise_burst(rnd, 0.04, 200, 2500, 0.012), at=k * 0.05, gain=1.0 - k * 0.12)
    out("wing_flap", flap, 0.7)

    # Fly buzz: 200 Hz saw with AM; integer cycles in 1 s so the loop is seamless.
    n = RATE
    buzz = []
    for i in range(n):
        t = i / RATE
        f_phase = (200 * t + 0.003 * math.sin(TWO_PI * 6 * t)) % 1.0
        saw = 2 * f_phase - 1
        am = 0.75 + 0.25 * math.sin(TWO_PI * 12 * t)
        buzz.append(saw * am)
    out_buzz = biquad(buzz, "lp", 1800)
    if "fly_buzz".startswith(only):
        write("fly_buzz", out_buzz, 0.6)

    # Cricket: pulse trains at 4.6 kHz.
    cricket = silence(0.6)
    for group in range(2):
        for k in range(4):
            p = sweep(4600, 4550, 0.02)
            mix_into(cricket, mul(p, envelope(len(p), 0.002, 0.008, 1.0)), at=group * 0.28 + k * 0.035)
    out("cricket_chirp", cricket, 0.7)

    out("insect_rustle", mul(biquad(noise(0.22, rnd), "hp", 3500), envelope(int(0.22 * RATE), 0.05, 0.12)), 0.35)

    hum = []
    for i in range(RATE):
        t = i / RATE
        ph = (110 * t) % 1.0
        hum.append((2 * ph - 1) * (0.7 + 0.3 * math.sin(TWO_PI * 30 * t)))
    if "beetle_hum".startswith(only):
        write("beetle_hum", biquad(hum, "lp", 700), 0.55)

    out("water_bloop", bubble(320, 950, 0.09), 0.8)
    splash = mul(biquad(noise(0.45, rnd), "lp", 3500), exp_decay(int(0.45 * RATE), 0.09))
    for k in range(4):
        mix_into(splash, bubble(rnd.uniform(400, 700), rnd.uniform(900, 1500), 0.06), at=0.05 + k * 0.07, gain=0.5)
    out("water_splash", splash)
    bubbles = silence(0.5)
    for k in range(7):
        mix_into(bubbles, bubble(rnd.uniform(700, 1100), rnd.uniform(1500, 2400), rnd.uniform(0.03, 0.05)), at=rnd.uniform(0, 0.42), gain=rnd.uniform(0.4, 1.0))
    out("bubbles", bubbles, 0.6)

    tw = [0.6 * math.sin(TWO_PI * 2200 * i / RATE) + 0.3 * math.sin(TWO_PI * 3300 * i / RATE) for i in range(int(0.4 * RATE))]
    out("firefly_twinkle", mul(tw, exp_decay(len(tw), 0.09)), 0.35)

    sw = noise(0.28, rnd)
    swish = []
    chunk = 512
    for k in range(0, len(sw), chunk):
        f = 500 + 2600 * (k / len(sw))
        swish.extend(biquad(sw[k:k + chunk], "bp", f, 1.2))
    out("feather_swish", mul(swish, envelope(len(swish), 0.05, 0.15)), 0.6)

    chime = [0.6 * math.sin(TWO_PI * 1600 * i / RATE) + 0.4 * math.sin(TWO_PI * 2400 * i / RATE) for i in range(int(0.18 * RATE))]
    out("dot_chime", mul(chime, exp_decay(len(chime), 0.05)), 0.4)


# --------------------------------------------------------------------------- ambience loops

LOOP = 20.0
XFADE = 2.0


def wind(rnd, seconds, cutoff, depth=0.5):
    w = biquad(pink(seconds, rnd), "lp", cutoff)
    slow = [1.0 - depth + depth * (0.5 + 0.5 * math.sin(TWO_PI * (0.07 * i / RATE) + 1.3 * math.sin(TWO_PI * 0.023 * i / RATE))) for i in range(len(w))]
    return mul(w, slow)


def make_ambience(only):
    total = LOOP + XFADE

    def out(name, samples, peak):
        if name.startswith(only):
            write(name, seamless(samples, XFADE), peak)

    if "amb_basement".startswith(only):
        rnd = random.Random(11)
        base = gain(biquad(brown(total, rnd), "lp", 160), 0.6)
        hum = [0.05 * math.sin(TWO_PI * 50 * i / RATE) + 0.02 * math.sin(TWO_PI * 100 * i / RATE) for i in range(len(base))]
        base = [a + b for a, b in zip(base, hum)]
        t = 0.5
        while t < total - 0.5:
            mix_into(base, bubble(rnd.uniform(900, 1300), rnd.uniform(1800, 2600), 0.05), at=t, gain=rnd.uniform(0.15, 0.35))
            t += rnd.uniform(1.2, 3.5)
        out("amb_basement", base, 0.35)

    if "amb_garden".startswith(only):
        rnd = random.Random(12)
        a = gain(wind(rnd, total, 900), 1.0)
        leaves = mul(biquad(noise(total, rnd), "hp", 2500), [0.08 * (0.5 + 0.5 * math.sin(TWO_PI * 0.11 * i / RATE)) ** 3 for i in range(len(a))])
        a = [x + y for x, y in zip(a, leaves)]
        t = 1.0
        while t < total - 1:
            mix_into(a, chirp_note(rnd.uniform(3500, 5000), rnd.uniform(2800, 4000), 0.07, index=0.6), at=t, gain=0.05)
            t += rnd.uniform(2.5, 6.0)
        out("amb_garden", a, 0.35)

    if "amb_meadow".startswith(only):
        rnd = random.Random(13)
        a = wind(rnd, total, 700, 0.6)
        hopper = biquad(noise(total, rnd), "bp", 7000, 3.0)
        pulse = [0.12 * (1.0 if (i // int(RATE * 0.012)) % 3 else 0.0) * (0.5 + 0.5 * math.sin(TWO_PI * 0.15 * i / RATE)) for i in range(len(hopper))]
        a = [x + y * p for x, y, p in zip(a, hopper, pulse)]
        out("amb_meadow", a, 0.33)

    if "amb_pond".startswith(only):
        rnd = random.Random(14)
        water = biquad(pink(total, rnd), "lp", 500)
        lap = [0.6 + 0.4 * math.sin(TWO_PI * 0.4 * i / RATE) * math.sin(TWO_PI * 0.13 * i / RATE) for i in range(len(water))]
        a = mul(water, lap)
        t = 0.8
        while t < total - 1:
            mix_into(a, bubble(rnd.uniform(250, 450), rnd.uniform(700, 1100), 0.08), at=t, gain=rnd.uniform(0.1, 0.25))
            t += rnd.uniform(1.0, 3.0)
        t = 2.0
        while t < total - 1:
            croak = silence(0.35)
            for k in range(rnd.randint(2, 4)):
                c = sweep(300, 260, 0.06, harmonics=((1, 1.0), (2, 0.6), (3, 0.4), (5, 0.2)))
                mix_into(croak, mul(c, envelope(len(c), 0.005, 0.03)), at=k * 0.08)
            mix_into(a, biquad(croak, "lp", 1200), at=t, gain=0.12)
            t += rnd.uniform(4.0, 8.0)
        out("amb_pond", a, 0.35)

    if "amb_night".startswith(only):
        rnd = random.Random(15)
        a = gain(wind(rnd, total, 400, 0.5), 0.5)
        for voice in range(4):
            f = rnd.uniform(4200, 5200)
            period = rnd.uniform(0.5, 0.9)
            t = rnd.uniform(0, period)
            while t < total - 0.3:
                ch = silence(0.12)
                for k in range(3):
                    p = sweep(f, f - 40, 0.018)
                    mix_into(ch, mul(p, envelope(len(p), 0.002, 0.008, 1.0)), at=k * 0.03)
                mix_into(a, ch, at=t, gain=rnd.uniform(0.05, 0.12))
                t += period * rnd.uniform(0.9, 1.1)
        out("amb_night", a, 0.33)


if __name__ == "__main__":
    prefix = sys.argv[1] if len(sys.argv) > 1 else ""
    print("Generating sounds into", os.path.normpath(OUT_DIR))
    make_effects(prefix)
    make_ambience(prefix)
