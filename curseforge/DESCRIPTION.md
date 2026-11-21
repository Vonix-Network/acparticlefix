**A tiny client-only mod that stops one Alex's Caves particle from crashing your game.**

### The crash

```
Exception while adding particle
java.lang.NullPointerException: Cannot invoke "java.util.List.size()" because "this.sprites" is null
```

It hits you when you join a world where something spawns the particle `alexscaves:tremorzilla_blast`,
and it repeats every time you rejoin while you are standing there. The server is fine — this is
purely a client-side crash.

### Why it happens

Alex's Caves (Unofficial Port) registers `tremorzilla_blast` as a particle and gives it a client
provider, but ships **no particle definition file** for it. Minecraft only gives a particle its
sprites when that file exists, so this one's sprite set stays empty, and the first code that asks
it for a sprite throws. The mod's own code is correct — it is the victim, not the cause.

Nothing in Alex's Caves itself spawns this particle: a real Tremorzilla uses the healthy
`_PROTON`, `_LIGHTNING`, `_EXPLOSION`, `_STEAM` and `RAYGUN_BLAST` types. The crash needs a
foreign spawner, such as a randomly generated dimension whose ambient particle happened to roll
this one — which is why it is rare, extremely specific, and reproducible for a given seed.

### The fix

This mod supplies the missing definition, pointing the particle at the `raygun_blast` texture
its sibling already uses. That is both the smallest possible change and the visually correct
one: `tremorzilla_blast` is the scaled-up version of that same sprite. The sprite set is filled
before anything can spawn the particle, so the crash is fixed at its source, for **every**
spawner — not just the one that crashed you.

As a backstop, the mod also makes that one particle factory return "nothing to draw" instead of
throwing if its sprites are ever missing again — for example if another resource pack overrides
the definition. You get one missing particle instead of a crash.

### What it is not

- Not a rewrite of Alex's Caves. Nothing is repackaged; it is one asset file plus a small guard
  on a single factory method.
- Not a gameplay change. Blocks, items, entities, damage, drops: untouched.
- Not something a server needs. It registers nothing, opens no network connection, and sends
  nothing. A server without it accepts you with no complaint, and players with and without the
  patch see identical gameplay — the only difference is that you don't crash.

### Compatibility

- Works with any 2.0.x build of the port, because the defect and the fix are identical across
  them.
- If a future build ships its own definition for this particle, that file takes precedence and
  this mod quietly does nothing.
- If Alex's Caves is not installed, this mod does nothing and your game starts normally.

### Requirements

Minecraft 1.21.1 · NeoForge 21.1.x · Alex's Caves (Unofficial Port) 2.0.x · Citadel (as Alex's
Caves requires) · **client-side only**

### Credits

Alex's Caves by **AlexModGuy**; the 1.21.1 unofficial port by **Raguto**. This mod redistributes
no Alex's Caves code or assets — it references a texture that is loaded from your installed
Alex's Caves jar at runtime.
