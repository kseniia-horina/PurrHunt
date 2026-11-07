# Purr Hunt

An offline Android game for cats. Mice, birds, insects and fish move like living prey across the
screen, the cat hunts them with its paw, and the owner keeps cat profiles, a hunter's journal and
statistics.

- No ads, purchases, analytics, trackers or network access (the app does not declare `INTERNET`).
- All graphics are drawn in code, all sounds are synthesised by a script in this repository.
- Languages: Russian, Ukrainian, English.
- Optional permissions are requested only when the matching feature is switched on:
  `CAMERA` (photo on catch, camera in video), `POST_NOTIFICATIONS` (play reminders).

## Features

- **5 locations** — Basement, Garden, Meadow (with a night variant and fireflies), Pond, Mix — with
  19 species including 4 rare ones. Seasons follow the real date (snow in winter, fallen leaves in autumn).
- **Lifelike prey**: every creature runs a state machine
  `Spawn → Idle → Wander → Dart → Freeze → Hide/Peek → Flee → Caught → Exit`; mice hug walls, dart and
  freeze, hide in burrows and peek out; birds hop and flutter away; fish flick and hide under lily pads.
- **Peaceful catches**: caught prey squeaks, hops, tumbles and runs away unharmed.
- **Cat-friendly design**: muted dark backgrounds, prey contrasted by brightness, a "cat contrast" halo
  option, blue-green instead of red for the glowing dot.
- **Modes**: Hunt, Feather on a rod (the owner leads it), Glowing dot.
- **Adaptive difficulty** from accuracy over the last 30 s (−40 % … +30 %).
- **Owner-only exit**: no buttons during play; hold the top right corner for 3 s (a ring appears after
  1 s) or hold three fingers for 2 s. Back is swallowed, the screen is pinned, system bars are hidden.
- **Owner tools**: cat profiles with photos, hunter's journal (bestiary, charts, touch heatmap,
  favourite prey), stars, ranks, 22 achievements, play reminders, a debug zoo of every animation.
- **Camera (optional)**: a silent front-camera photo at the moment of a catch, screen recording with the
  camera in a corner.

## Building

Requirements: JDK 17+ (Android Studio's bundled JBR works), Android SDK with platform 36.

```bash
./gradlew assembleDebug        # debug build, includes the zoo
./gradlew assembleRelease      # R8-minified release (signed with the debug key; replace for publishing)
./gradlew testDebugUnitTest    # unit tests (engine, difficulty, progress, Room via Robolectric)
./gradlew connectedDebugAndroidTest   # Compose UI tests on a device or emulator
```

To open the debug zoo directly:
`adb shell am start -n app.purrhunt.debug/app.purrhunt.MainActivity --ez open_zoo true`.

## Structure

```
app.purrhunt
 ├─ ui/            Compose screens, navigation, theme; ui/game hosts the game (surface, pause, camera)
 ├─ game/
 │   ├─ engine/    fixed-step loop, pooled creatures, behaviour base, spawning, difficulty, toys, exit gesture
 │   ├─ creatures/ behaviour (pure Kotlin) and renderer (Compose DrawScope) per body plan
 │   ├─ scenes/    scene geometry (pure Kotlin) and procedural art per location
 │   ├─ render/    shared drawing: volume gradients, fur, eyes, shadows, sprite cache, frame renderer
 │   └─ audio/     SoundManager: SoundPool effects with stereo panning, MediaPlayer ambience
 ├─ data/          Room database, DataStore settings, repositories, reminders
 ├─ domain/        models, ranks, stars, achievements, favourite prey, night/season rules
 └─ di/            Hilt modules
```

`game/engine`, `game/creatures/*Behavior*`, `game/scenes/SceneLayout` and `domain` use no Android
classes and are covered by JVM unit tests. The random source (`GameRandom`) is injected, so simulations
are reproducible.

### How a frame works

`GameSurface` asks for a frame with `withFrameNanos`, feeds the timestamp to `GameEngine.frame`, which runs
as many fixed 1/60 s steps as needed (accumulator), then draws through `SceneRenderer`:
cached background bitmap → animated under-layer (caustics, dust) → ripples → ground creatures →
cached foreground bitmap (crates, bushes, lily pads hide what is behind them) → flying creatures → toys →
paw prints → swaying grass → dusk → exit ring.

The renderer is an interface (`FrameRenderer`) drawing into any `DrawScope`. If a device can't keep
50+ FPS with the Compose host, the same renderer can draw on a `SurfaceView` render thread: lock the
surface canvas and wrap it with `CanvasDrawScope().draw(density, layoutDirection, canvas, size) { ... }`.

Creatures, effects and paths are pooled and reused: the step and draw code don't allocate. Static fur and
body volume are pre-rendered into bitmaps (`SpriteCache`), so a frame draws a bitmap plus a few animated
parts (legs, ears, tails, wings). Debug builds log the frame rate every 5 s (`adb logcat -s PurrHunt`).

## Adding a new creature

1. **Species** — add an entry to `domain/Species.kt` with its family, medium, size and rarity flags.
   If it needs a new body plan, add a `Family` value.
2. **Where it lives** — add it to `common` or `rare` of a `Location` in `domain/Location.kt`
   (or to `SceneSpec.NIGHT_COMMON`).
3. **Behaviour parameters** — add a `BehaviorConfig` in `game/engine/BehaviorConfig.kt`
   (speeds, state durations, transition weights). Tune here without touching code.
4. **Behaviour** — reuse a family behaviour or write a new `Behavior` in `game/creatures/` (pure Kotlin),
   and register it in `Behaviors.kt`.
5. **Look** — add a palette to the family renderer or write a new `CreatureRenderer` and register it in
   `CreatureRenderers`. Draw in unit space (body ≈ 1 unit long, facing +x), cache paths and brushes, put the
   static body into a `SpriteCache`.
6. **Sounds** — add cues to `SoundCue`, synthesise them in `tools/generate_sounds.py`, map them in
   `SoundManager.resources` and `SoundManager.cuesFor`.
7. **Texts** — name and fact strings in `values`, `values-ru`, `values-uk`, mapped in `ui/components/Labels.kt`.
8. **Check** — open the debug zoo, select the species and watch every pose; run `AllScenesTest`.

## Sounds

Run `python tools/generate_sounds.py` to regenerate every sound. To replace a synthesised sound with a
recording, put a file with the same name into `app/src/main/res/raw/` — see [docs/SOUNDS.md](docs/SOUNDS.md)
for details and CC0 sources.

## Testing

- Unit tests: creature state machines in every scene, catches and hitboxes, adaptive difficulty, exit
  gestures, spawning and rare prey, ranks, stars, achievements, favourite prey, Room repositories.
- Compose UI tests: onboarding, navigation, settings persistence, owner exit gesture and swallowed back.
- Manual checklist with a real cat: [docs/MANUAL_TEST.md](docs/MANUAL_TEST.md).
