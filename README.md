# Alex's Caves: Tremorzilla Crash Fix

A tiny **client-only** mod that stops a crash caused by one [Alex's Caves (Unofficial
Port)](https://www.curseforge.com/minecraft/mc-mods/alexs-caves-unofficial-port) particle, so a
player can fix it on their own client without the server changing anything.

Minecraft **1.21.1** · NeoForge · Alex's Caves (Unofficial Port) **2.0.x** · **client-side only**

---

## The crash

```
Exception while adding particle
java.lang.NullPointerException: Cannot invoke "java.util.List.size()" because "this.sprites" is null
    at net.minecraft.client.particle.ParticleEngine$MutableSpriteSet.get(...)
    at net.minecraft.client.particle.TextureSheetParticle.pickSprite(...)
    at com.github.alexmodguy.alexscaves.client.particle.RaygunBlastParticle$TremorzillaFactory.createParticle(...)
```

It hits a player who joins a world where something spawns the particle
`alexscaves:tremorzilla_blast`, and it re-fires on every join while that condition holds. The
server never crashes — it is purely client-side.

## Why it happens

Alex's Caves (Unofficial Port) **registers** `tremorzilla_blast` as a particle and registers a
client provider for it, but ships **no particle definition file** for it. Vanilla gives a
particle its sprites only when such a file exists, so this particle's sprite set stays empty,
and the first code to ask it for a sprite throws.

The mod's own factory code is fine and is byte-identical in every 2.0.x build; it is the
victim, not the cause. Nothing in Alex's Caves itself spawns this particle — a real Tremorzilla
uses the healthy `_PROTON`, `_LIGHTNING`, `_EXPLOSION`, `_STEAM` and `RAYGUN_BLAST` types — so
the crash needs a foreign spawner, such as a randomly generated dimension whose ambient
particle happens to be this one. Full derivation in [`docs/ROOT-CAUSE.md`](docs/ROOT-CAUSE.md).

## The fix

Two layers, in this order of importance.

**1. The missing asset (root-cause fix).** This mod ships the definition Alex's Caves omits:

```json
{ "textures": [ "alexscaves:raygun_blast" ] }
```

`raygun_blast` is the sibling particle's texture, it already exists in the mod, and
`tremorzilla_blast` is the Tremorzilla's scaled-up version of that same sprite — so this is
both the minimal and the visually correct choice. The sprite set is populated before anything
can spawn the particle, so the crash is fixed at its source **for every spawner**.

**2. A factory guard (backstop).** `TremorzillaFactory.createParticle` is additionally guarded:
if the sprite set cannot produce a sprite, it returns `null` before constructing anything, and
vanilla skips that one particle instead of crashing. This is what saves the client if another
resource pack overrides the definition, a later mod replaces it, or the asset is stripped from a
repack. It applies only on Alex's Caves 2.0.x; on any other build it is skipped with one WARN.

## What this mod is not

- **Not a rewrite of Alex's Caves.** No code is replaced and nothing is repackaged. One asset
  file, one small guard, one no-op mod entrypoint.
- **Not a gameplay change.** Nothing about blocks, items, entities, damage or drops.
- **Not something the server needs.** Registers nothing, opens no network channel, sends
  nothing, and the guard never runs server-side. A server without it accepts a patched client
  with no complaint, and patched and unpatched players see identical gameplay.

## Compatibility

- Works with any 2.0.x Alex's Caves (Unofficial Port) build, because the defect and its fix are
  identical across them.
- If a future build ships its own `tremorzilla_blast.json`, that file lives in the mod's own
  resource pack, which takes precedence — this mod becomes a harmless no-op rather than
  conflicting.
- If Alex's Caves is not installed at all, this mod does nothing and the game starts normally.

## Verified

- The crash reproduces on demand with the asset absent, and disappears with it present — same
  client, one variable changed. Exact reported NPE in the failing case.
- Patched client joins a dedicated server **without** the mod and plays normally.
- Client starts normally with the mod installed and Alex's Caves absent.
- Evidence: [`docs/ACCEPTANCE.md`](docs/ACCEPTANCE.md)

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.x
- Alex's Caves (Unofficial Port) 2.0.x — optional at load time, but that is what this fixes
- Alex's Caves requires Citadel; install that as usual

## Credits

Alex's Caves by **AlexModGuy**; the unofficial 1.21.1 port by **Raguto**. This mod ships no
third-party code — only a resource file for a particle the mod defines but never described.

## Licence

LGPL-3.0-or-later. No Alex's Caves code or assets are redistributed; the texture referenced is
loaded from the installed Alex's Caves jar at runtime.
