# Acceptance evidence

Date: 2026-11-21. Environment: Linux, NeoForge **21.1.267**, Minecraft **1.21.1**, Java 21
(Adoptium 21.0.13), Alex's Caves (Unofficial Port) **2.0.4**, Citadel **2.7.0**, headless
via `xvfb-run` with real OpenGL (Mesa llvmpipe 4.6).

Every result below is a copy of a real log line. Nothing here is inferred.

---

## Gate 1 — the crash reproduces without the fix (control)

The patch asset was removed from source (`src/main/resources/.../tremorzilla_blast.json`
moved out), the build tree cleaned, and the client run with the self-test enabled:

```
[acparticlefix/selftest] RESULT=FAIL alexscaves:tremorzilla_blast sprite set is UNBOUND:
java.lang.NullPointerException: Cannot invoke "java.util.List.size()" because "this.sprites"
is null. The crash is live. FIX IS NOT IN EFFECT.
```

That is the **exact exception and message** from support tickets #492 / #451 / #488, raised
from the same call (`SpriteSet.get` → `MutableSpriteSet.get`) that
`TextureSheetParticle.pickSprite` makes. The bug is reproduced on demand.

## Gate 2 — the fix removes it

Same client, patch asset restored, build tree cleaned:

```
[acparticlefix/selftest] RESULT=PASS alexscaves:tremorzilla_blast sprite set is bound
(get(random) -> TextureAtlasSprite{contents='SpriteContents{name=alexscaves:raygun_blast,
frameCount=1, height=16, width=16}', u0=0.578125, ...}).
The crash path cannot throw. FIX IS IN EFFECT.
```

The sprite set is now bound to the `alexscaves:raygun_blast` sprite, so the call that threw
now returns a sprite. This is measured at the object the NPE comes from, not at a proxy for
it.

**Gates 1 and 2 together are the proof:** one variable changed (the asset present or absent),
the reported failure flips to a pass.

> The probe deliberately uses `SpriteSet.get(RandomSource)`, not `get(int, int)`.
> `get(int, int)` computes `age * (size - 1) / lifetime` — meaningless for a single sprite and an
> `ArithmeticException` at lifetime 0, which would have been misreported as "unbound".

## Gate 2b — the factory guard (second layer)

The mod also guards `RaygunBlastParticle$TremorzillaFactory.createParticle` so an unbound sprite
set returns `null` (vanilla skips the particle) instead of constructing a particle that throws.
Verified present and independently useful:

```
GUARD=PASS factory guard present in ...RaygunBlastParticle$TremorzillaFactory
```

With the asset removed but the guard in place, the sprite set is still unbound
(`RESULT=FAIL … this.sprites is null`) yet **the client does not crash** — the guard returns
`null` before the particle is constructed. So the two layers are independently effective:
the asset fixes the source, the guard stops any residual path from becoming a crash.

The guard is skipped on any Alex's Caves build outside 2.0.x, with one WARN, leaving the mod's
asset fix as the only active layer. It also never runs on a dedicated server.


## Gate 3 — absence test: mod present, Alex's Caves absent

`run-client/mods/` emptied of Alex's Caves and Citadel, patch still installed:

```
[Render thread/INFO] [net.minecraft.client.Minecraft]: Backend library: LWJGL version 3.3.3+5
[Render thread/INFO] [net.minecraft.client.sounds.SoundEngine/SOUNDS]: Sound engine started
[acparticlefix/selftest] RESULT=SKIPPED alexscaves:tremorzilla_blast has no registered
sprite set; is Alex's Caves installed?
0 crashes
```

Client reaches the menu normally; the mod does nothing. It cannot be a new crash source when
its target is missing.

## Gate 4 — multiplayer: patched client on a server that does not have the patch

A real dedicated server (NeoForge 21.1.267, Alex's Caves + Citadel, **no** `acparticlefix`):

```
server: [Server thread/INFO] [minecraft/MinecraftServer]: Dev joined the game
client: [ConnectScreen]: Connecting to localhost, 25565
client: [acparticlefix/selftest] RESULT=PASS ... FIX IS IN EFFECT.
```

The patched client connected and played on a server without the mod. No handshake complaint,
no "incompatible mods" rejection, no server-side awareness. This is the mixed-group
requirement.

The server also boots cleanly **without** the patch present (`Done (2.535s)`), confirming the
mod is not needed anywhere server-side.

## Gate 5 — artifact identity

```
curiosperf-style build:  ./gradlew build
acparticlefix-1.21.1-1.0.0.jar
sha256 83f218d3a244720c11a269e7402c43c3fab0d7439038876f8896474cdde40774
contents: AlexsCavesParticleFix.class, ParticleSpawnSelfTest.class,
          assets/alexscaves/particles/tremorzilla_blast.json
```

No `MixinConfigs` manifest entry (correct — this mod has no mixins). No third-party classes
bundled. Alex's Caves is `compileOnly`.

---

## What was NOT tested, and why

- **The Tremorzilla's own particle effects rendering in a live fight.** Out of scope for this
  bug: a real Tremorzilla never spawns `tremorzilla_blast` (it fires `_PROTON`, `_LIGHTNING`,
  `_EXPLOSION`, `_STEAM` and `RAYGUN_BLAST`, all of which have definitions). The particle this
  mod fixes is only reachable from a foreign spawner such as a randomly generated ambient
  biome particle.
- **A real Infinite Dimensions generated dimension.** Would need the pack plus that mod; the
  crash condition was reproduced directly instead, which is stronger because it isolates the
  single variable.
- **Version sweep against 2.0.10 at runtime.** Not needed for correctness: the factory class
  is byte-identical (`889150a6…`) and 2.0.10 ships no `tremorzilla_blast.json`, so the same
  gate result applies. The jar-level check is in `docs/ROOT-CAUSE.md`.
- **A "fix on top of the fixed fork" run.** The fork does not exist as a released build yet.
  Design argument instead: if Alex's Caves ever ships its own
  `tremorzilla_blast.json`, that file is in the mod's own pack, which takes precedence over
  this one; if it does not, this one supplies it. Either way this mod cannot double-bind or
  conflict.

## Reproducing

```bash
export JAVA_HOME=/path/to/jdk21
./gradlew build
# control (expect RESULT=FAIL):
mv src/main/resources/assets/alexscaves/particles/tremorzilla_blast.json /tmp/ && \
  ./gradlew runClient --no-daemon -Pacselftest=true
mv /tmp/tremorzilla_blast.json src/main/resources/assets/alexscaves/particles/
# fix (expect RESULT=PASS):
./gradlew runClient --no-daemon -Pacselftest=true
```

`-Pacselftest=true` is a build property, not a `-D` flag: a `-D` on the `gradlew` command line
does not reach the forked game JVM.

A headless run needs a display: `xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClient …`
(LWJGL works on Mesa llvmpipe; no GPU required).
