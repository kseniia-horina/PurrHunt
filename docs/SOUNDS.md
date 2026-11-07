# Sounds

All sounds in Purr Hunt are synthesised by `tools/generate_sounds.py` (Python 3 standard library only).
The output is 16-bit mono WAV at 44.1 kHz in `app/src/main/res/raw/`. Every random source in the script
is seeded, so re-running it reproduces the same files.

```bash
python tools/generate_sounds.py          # regenerate everything
python tools/generate_sounds.py mouse_   # only files whose name starts with "mouse_"
```

## Files

| File | What | Used for |
|---|---|---|
| `mouse_squeak_1..4` | 3–8 kHz sine sweeps, 60–150 ms | mice, idle and fright |
| `mouse_rustle_1..2` | filtered noise impulses | running feet (volume follows speed and distance to the centre) |
| `mouse_caught` | three frightened squeaks | catching a mouse |
| `rat_squeak_1..2`, `rat_caught` | lower, rough squeaks with noise | rat |
| `sparrow_chirp_1..2`, `tit_trill_1..2`, `bullfinch_call` | FM bird notes, one pattern per species | birds |
| `wing_flap` | noise bursts | birds taking off |
| `fly_buzz` | 200 Hz saw with AM, seamless 1 s loop | flying fly, volume follows speed |
| `cricket_chirp` | 4.6 kHz pulse trains | cricket |
| `insect_rustle` | quiet high noise | butterflies, ladybird, beetle |
| `beetle_hum` | 110 Hz saw, seamless 1 s loop | flying beetle |
| `water_bloop`, `water_splash`, `bubbles` | resonant bubble sweeps and noise | fish, tadpoles, water striders |
| `firefly_twinkle` | soft bell | fireflies |
| `feather_swish`, `dot_chime` | swept noise / soft chime | toy modes |
| `amb_basement`, `amb_garden`, `amb_meadow`, `amb_pond`, `amb_night` | 20 s seamless ambience loops | background per location |

Effects play through `SoundPool` (at most 8 streams) with equal-power stereo panning by the creature's
X position; ambience loops play through `MediaPlayer`.

## Replacing a sound with a recording

1. Prepare a mono file, 44.1 kHz, ideally 16-bit WAV or OGG Vorbis. Keep effects short (under 1–2 s):
   SoundPool decodes them fully into memory. Ambience loops should start and end at the same level so the
   loop point is inaudible.
2. Name it exactly like the file it replaces (for example `mouse_squeak_2.wav` or `mouse_squeak_2.ogg`)
   and put it into `app/src/main/res/raw/`, deleting the synthesised one: Android resources may not have
   two files with the same name and different extensions.
3. Rebuild. Nothing else needs to change; the code refers to sounds by resource name
   (see `SoundManager.resources`).
4. If you regenerate synthesised sounds later, run the script with a prefix so your recordings are not
   overwritten, or remove the corresponding lines from the script.

## Where to find CC0 recordings

Use only recordings with a licence that allows redistribution in an app. CC0 (public domain) is the
simplest: no attribution is required.

- [Freesound](https://freesound.org) — filter by licence “Creative Commons 0”.
- [BigSoundBank](https://bigsoundbank.com) — all sounds are royalty free / CC0.
- [Pixabay Sound Effects](https://pixabay.com/sound-effects/) — Pixabay licence (free use, check the terms).
- [OpenGameArt](https://opengameart.org) — filter by CC0.
- [Sonniss GDC bundles](https://sonniss.com/gameaudiogdc) — royalty free, check the bundle licence.

Keep a note of the source URL and licence of each replaced file (for example in this document).
