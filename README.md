# Dagonjira - Ben's Titans addon (Forge 1.20.1)

An addon mod (not a patched copy of the jar) that adds **Dagonjira**, a variant of Dagonzilla.
It subclasses Ben's Titans' `DagonzillaEntity`, so all AI, animations, tail parts and sounds are inherited.

## Build
1. Put your Citadel jar (from your mods folder) in `libs/` and name it `citadel-local.jar`.
   `libs/benstitans-local.jar` is already the jar you uploaded.
2. JDK 17, then: `./gradlew build` (generate the wrapper with `gradle wrapper` if needed).
3. Output: `build/libs/dagonjira-forge-1.20.1-1.0.0.jar`. Install with Ben's Titans + Citadel.

## What differs from Dagonzilla
| | Dagonzilla | Dagonjira |
|---|---|---|
| Height | 11.7 | 10.2 (-1.5), width scaled by the same factor |
| Health | 500 | 400 |
| Attack / damage dealt | 30 | 24, and all damage x0.8 (incl. beam) |
| Armor | 7.5 | 6 |
| Speed | 0.36 | 0.32 |
- Dorsal fins: 2019 thorny fins repainted as 2014-style swept triangular plates (same UV slots, so the model is unchanged).
- Thermonuclear skin, glows, charge emission and beam: fire orange to yellow.
- Beam: own renderer - white-hot core, turbulent flame sheets, spiralling flame tongues, fireballs at mouth and impact, plus flame/smoke particles.

## Commands (/dagonjira, op level 2; acts on nearest Dagonjira within 128 blocks, or add a selector)
spawn, speak, roar, roar2, beam, pulse, stop, scratch left|right, tail left|right, stomp left|right, bite, trample,
thermonuclear <0-20>, charge <0-600>, target <entity>, calm, heal, info

## Known limitations
- Written against decompiled structure only; **not compiled or run** (no Forge toolchain was available). Expect to fix small compile/API mismatches.
- The nuclear pulse shockwave keeps Ben's Titans' texture/particles (changing its texture string triggers pink neon-dust particles).
- Tail hit-parts keep Dagonzilla's sizes.
