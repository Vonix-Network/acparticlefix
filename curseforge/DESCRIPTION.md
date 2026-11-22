# Alex's Caves: Tremorzilla Crash Fix

**A small client-side patch for [Alex's Caves (Unofficial Port)](https://www.curseforge.com/minecraft/mc-mods/alexs-caves-unofficial-port) that stops one of its particles from crashing your game.**

One player on a server can get permanently locked out of their world by this bug: every time they
join, the client crashes, and the only workaround is for staff to move their character somewhere
else. This mod fixes that on the player's own client — the server does not need it, does not know
about it, and does not have to change anything.

---

## The crash

```
Exception while adding particle
java.lang.NullPointerException: Cannot invoke "java.util.List.size()" because "this.sprites" is null
    at net.minecraft.client.particle.ParticleEngine$MutableSpriteSet.get(...)
    at net.minecraft.client.particle.TextureSheetParticle.pickSprite(...)
    at com.github.alexmodguy.alexscaves.client.particle.RaygunBlastParticle$TremorzillaFactory.createParticle(...)
```

It happens when you join a world where something spawns the particle `alexscaves:tremorzilla_blast`.
It repeats on every rejoin while that condition holds. **The server is fine** — this is purely a
client-side crash, which is exactly why a client-side patch can fix it completely.

## Why it happens

Alex's Caves (Unofficial Port) registers `tremorzilla_blast` as a particle and registers a client
provider for it, but ships **no particle definition file** for it.

Minecraft only gives a particle its sprites when such a file exists, so this particle's sprite set
stays empty — and the first code that asks it for a sprite throws.

The mod's own code is correct. The factory in that stack trace is the **victim, not the cause**: it is
byte-identical across every 2.0.x build, and it does exactly what its healthy sibling particles do.

Nothing in Alex's Caves itself spawns this particle either. A real Tremorzilla fires the healthy
`_PROTON`, `_LIGHTNING`, `_EXPLOSION`, `_STEAM` and `RAYGUN_BLAST` types. The crash needs a **foreign
spawner** — such as a randomly generated dimension whose ambient particle happened to roll this one.
That is why the bug is rare, very specific, and reproducible only for a given seed: it is not that the
particle is broken for everyone, it is that one world rolled it.

## The fix

Two layers, in this order of importance.

**1. The missing asset — the root cause.** This mod ships the definition Alex's Caves omits:

```json
{ "textures": [ "alexscaves:raygun_blast" ] }
```

`raygun_blast` is the texture the sibling particle already uses and it already exists in the mod —
`tremorzilla_blast` is the Tremorzilla's scaled-up version of that same sprite. So this is both the
smallest possible change and the visually correct one. The sprite set is filled before anything can
spawn the particle, so the crash is fixed at its source, **for every spawner**, not just the one that
crashed you.

**2. A factory guard — a backstop.** That one particle factory is additionally guarded: if its sprite
set cannot produce a sprite, it returns "nothing to draw" instead of throwing, and Minecraft skips that
single particle. This is what saves your client if a resource pack overrides the definition, another
mod replaces it, or the asset is stripped from a repack. You get one missing particle instead of a
crash.

The guard applies only on Alex's Caves 2.0.x. On any other build it is skipped with a single warning,
leaving the asset fix as the only active layer.

## What this mod is not

- **Not a rewrite of Alex's Caves.** No code is replaced and nothing is repackaged. It is one asset
  file, one small guard on a single factory method, and a no-op mod entrypoint.
- **Not a gameplay change.** Blocks, items, entities, damage, drops, worldgen — all untouched. Same
  visuals when the particle works.
- **Not something a server needs.** It registers nothing, opens no network channel, and sends nothing.
  A server without it accepts a patched client with no complaint, and players with and without the
  patch see identical gameplay — the only difference is that you don't crash.

## Compatibility

- **Alex's Caves (Unofficial Port) 2.0.x** — works with any build in that line, because the defect and
  its fix are identical across them.
- If a future Alex's Caves build ships its own definition for this particle, that file lives in the
  mod's own resource pack and takes precedence — this mod quietly becomes a no-op rather than
  conflicting.
- If Alex's Caves is not installed at all, this mod does nothing and your game starts normally. It
  cannot become a new crash source.
- Safe alongside performance and shader mods; it does not touch rendering beyond refusing to draw one
  already-broken particle.

## Requirements

- **Minecraft 1.21.1**, **NeoForge 21.1.x**
- **[Alex's Caves (Unofficial Port)](https://www.curseforge.com/minecraft/mc-mods/alexs-caves-unofficial-port) 2.0.x** — optional at load time, but this is what the mod fixes
- **Citadel** — required by Alex's Caves, install as usual
- **Client-side only.** Do not install on a server; it is pointless there and will simply do nothing.

## A note on the two Alex's Caves projects

There are two, and it matters which one you are running:

- **[Alex's Caves](https://www.curseforge.com/minecraft/mc-mods/alexs-caves)** by **AlexModGuy** — the
  original mod, for **Minecraft 1.20.1 Forge**.
- **[Alex's Caves (Unofficial Port)](https://www.curseforge.com/minecraft/mc-mods/alexs-caves-unofficial-port)**
  by **Raguto** — the community port of that mod to **Minecraft 1.21.1 NeoForge**. This is what this
  patch is for.

The defect this patch works around is inherited from the original mod and was carried faithfully
through the port, so the missing asset is present in both lineages. This patch targets the 1.21.1
NeoForge port because that is the build affected players are running.

## Credits

**Alex's Caves** was created by **AlexModGuy** —
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/alexs-caves) ·
[GitHub](https://github.com/AlexModGuy/AlexsCaves).

The **1.21.1 NeoForge port** is by **Raguto** —
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/alexs-caves-unofficial-port) ·
[GitHub](https://github.com/Raguto/AlexsCaves-1.21.1).

All credit for the mod, its art, its content and its code belongs to them and their contributors.
This patch ships **no Alex's Caves code or assets** — it adds a resource file for a particle the mod
defines but never described, and references a texture that is loaded from your own installed Alex's
Caves jar at runtime.

**This is an unofficial community patch.** It is not affiliated with, endorsed by, or supported by
AlexModGuy or Raguto. Please do not report problems caused by this patch to them.

## Licence

**LGPL-3.0-or-later.** See the included `LICENSE`.
