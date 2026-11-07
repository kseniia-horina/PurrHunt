# Manual test checklist

Run on a real mid-range phone and, if possible, a tablet. Use a release build for performance checks.
Mark each item ✅ / ❌ and write down the device, Android version and app version.

## 1. First launch and owner screens

- [ ] Splash with the paw logo, then a 3-slide onboarding (exit gesture, screen protector, short sessions + treat).
- [ ] Onboarding is not shown again after restart.
- [ ] Menu shows the cat profile (avatar, name, rank, stars); with no cat it offers to add one.
- [ ] Cat profiles: add with a photo from the Photo Picker (no permission prompt), choose a colour, edit, delete with confirmation; switching the current cat switches statistics.
- [ ] Every screen is reachable and Back returns correctly; nothing overlaps on a small phone and in landscape.
- [ ] Language: Russian, Ukrainian, English and System switch instantly; no untranslated text.
- [ ] TalkBack: every button, switch, slider, avatar, bestiary card and chart is announced meaningfully.

## 2. Game, protection against leaving

- [ ] First game: screen pinning explanation, then the system pin prompt; after accepting, Home/Recents don't leave the game.
- [ ] No buttons on the game screen; system bars are hidden and come back only by swiping, without leaving.
- [ ] Back does nothing during play.
- [ ] Holding the top right corner: nothing visible for 1 s, then a ring fills, pause after 3 s.
- [ ] Three fingers held for 2 s open the pause.
- [ ] A short tap in the corner does not pause; paws all over the screen for a minute never pause.
- [ ] Pause: continue, change location, change mode, finish (results), exit to menu all work; pinning ends on leaving.
- [ ] Turning the screen off / an incoming call: the game is paused and silent when you return.
- [ ] Orientation stays fixed during a hunt; menus work in both orientations.

## 3. Every location and creature

For each location (Basement, Garden, Meadow, Night meadow, Pond, Mix):

- [ ] Background looks right, props hide creatures behind them, season details match the date (switch "Seasons" off to compare).
- [ ] Creatures move with darts, stops and turns, never in straight uniform lines; they hide and peek out.
- [ ] A touch next to a creature scares it away; a touch on it catches it: squeak/sound, hop, tumble, runs away unharmed.
- [ ] A replacement appears 1–3 s later from a burrow, an edge or the water; the count matches the setting (1–5).
- [ ] Sounds come from the side where the creature is (headphones); turning stereo off centres them.
- [ ] Debug zoo: every species shows every pose (idle, walk, dart, freeze, special actions, caught).
- [ ] Rare prey appears occasionally (white mouse, goldfish, swallowtail, giant firefly) and is marked in the results.

## 4. Modes and session

- [ ] Feather: the owner's first held finger leads it with a lag and swing; touches on the feather count as catches.
- [ ] Glowing dot: blue-green, dashes, stops, jitters and disappears behind edges; touching it counts.
- [ ] Session timer (set 5 min): prey goes to sleep, dusk falls, sounds fade, results appear.
- [ ] Results: catches per species with pictures, misses, accuracy, duration, stars, new achievements, treat reminder (switchable).
- [ ] Adaptive difficulty: missing a lot for ~30 s makes prey visibly slower; catching everything makes it faster.

## 5. Statistics

- [ ] Journal: bestiary silhouettes turn into pictures with facts once caught; charts by day/week; heatmap for the last hunt and all time; favourite prey with a play button.
- [ ] Achievements progress and unlock; ranks change at 50/200/500/1000 catches.
- [ ] "Unlock locations with stars" locks and unlocks cards.
- [ ] Reminders: notification permission asked only when switched on; a notification arrives at the chosen time and day.
- [ ] Reset statistics asks for confirmation and keeps the cat profiles.

## 6. Camera (optional)

- [ ] Photo on catch: camera permission asked only when switched on; photos appear in Best shots, no more than one per 10 s; share and delete work.
- [ ] Video: the system recording consent appears before pinning; the front camera preview shows in the bottom left corner; after the hunt "Video saved to Movies/PurrHunt" and the video plays in the gallery.
- [ ] Denying any permission just disables the feature.

## 7. With a real cat

Play at least three 10-minute sessions on different days. Note for each:

- [ ] Reaction to each species (stares, stalks, paws, ignores) — which locations work best.
- [ ] Does the cat follow the sounds (looks behind the phone, at the side of the sound)?
- [ ] Is the prey visible enough on each background? Try "Cat contrast mode".
- [ ] Is the speed right? Note the speed level and whether adaptive difficulty helps.
- [ ] Accidental exits: did the cat ever pause or leave the game? How (paw on the corner, three paws, home gesture)?
- [ ] Claws vs. screen: any scratches with/without a protector.
- [ ] Ending with a real treat — does the cat stay calm and satisfied?

## 8. Performance and battery

- [ ] Frame rate stays at 60 FPS (debug: `adb logcat -s PurrHunt`, or the GPU profiling bars) with 5 creatures on the Pond and Meadow.
- [ ] 15 minutes of play: battery drop ___ %, device temperature (warm / hot), no stutter at the end.
- [ ] Memory stays flat over 15 minutes (Android Studio profiler).
