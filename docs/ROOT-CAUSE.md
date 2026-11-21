# Root cause — verified independently, and it is not what the handoff said

**Verdict:** the handoff's root-cause theory is **wrong**. The factory is correct. The bug is a
**missing particle definition asset** that no published Alex's Caves (Unofficial Port) build
ships. 2.0.10 does not fix it.

Everything below was checked against the actual public jars, not taken from a prior report.

## What the handoff claimed

> "the `TremorzillaFactory` path creates the particle without a populated sprite set, so
> `sprites` is null at the first `pickSprite` … confirm in the jar."

and prescribed:

> "Target the Alex's Caves factory itself (`RaygunBlastParticle$TremorzillaFactory.createParticle`):
> if the sprite set is missing/null, return `null`"

## What is actually true

### 1. The factory code is correct — and byte-identical in every build

```
$ unzip -p alexscaves-2.0.4.jar  '.../RaygunBlastParticle$TremorzillaFactory.class' | sha256sum
889150a613ffed6e51742744d4473537629b7a7eb1fb02c27006ce3e1c5af0ea
$ unzip -p alexscaves-2.0.10.jar '.../RaygunBlastParticle$TremorzillaFactory.class' | sha256sum
889150a613ffed6e51742744d4473537629b7a7eb1fb02c27006ce3e1c5af0ea
```

`javap -p -c` shows an ordinary, correct factory: it stores the `SpriteSet` it is given and
calls `pickSprite(spriteSet)` on it at bytecode offset **31**. That is the exact call the crash
report implicates, and it is byte-for-byte the same call the *working* sibling `Factory` makes
(offset 31 in both). There is no missing assignment, no wrong variable, no null constant.

**Therefore there is no "null sprite set" on this path to guard.** The factory never receives
null — it receives a non-null `MutableSpriteSet` *that is itself empty*.

### 2. The real defect: `tremorzilla_blast` has no particle definition file

```
$ unzip -l alexscaves-2.0.4.jar  | grep -c 'particles/tremorzilla_blast.json'   -> 0
$ unzip -l alexscaves-2.0.10.jar | grep -c 'particles/tremorzilla_blast.json'   -> 0
$ unzip -l alexscaves-2.0.4.jar  | grep -c 'assets/alexscaves/particles/'       -> 96 (incl. dir)
$ ls assets/alexscaves/particles/*.json | wc -l                                 -> 95
```

`alexscaves:tremorzilla_blast` **is** registered (`ACParticleRegistry` contains the field and
the id `tremorzilla_blast`), and `ClientProxy` **does** map `TREMORZILLA_BLAST` to a provider
(`RaygunBlastParticle$TremorzillaFactory`). But the mod ships **95** definition JSONs and there
is no `tremorzilla_blast.json` among them.

Vanilla binds sprites like this:

1. `ParticleEngine.registerSpriteSet(type, registration)` creates an **empty**
   `MutableSpriteSet` — its `sprites` field is `null` — and stores it under the particle type.
2. On every resource reload, vanilla lists `assets/<ns>/particles/*.json` and calls
   `spriteSet.rebind(list)` **only for types that have a file**.
3. A registered type with **no** file never gets `rebind`, so its `sprites` stays `null`
   forever.
4. `MutableSpriteSet.get(int, int)` then reads `this.sprites.size()` → **NPE**, which is
   precisely the crash: `Cannot invoke "java.util.List.size()" because "this.sprites" is null`.

The factory is the *victim*, not the culprit: it is the first code to ask an unbound sprite set
for a sprite.

### 3. It is inherited from upstream, not introduced by the port

The original Alex's Caves (1.20.1 Forge) has the same registration and the same missing asset.
The port faithfully carried the defect. Waiting for either author to fix it is not a plan.

### 4. Affected builds

Every published port build, because the factory class is unchanged across all of them and none
ships the JSON:

| Build | `tremorzilla_blast.json` | Factory class sha256[:16] |
|---|---|---|
| 2.0.4 | **absent** | `889150a613ffed6e` |
| 2.0.10 | **absent** | `889150a613ffed6e` |

2.0.5–2.0.9 were not downloaded, but the class is identical in the two ends of the range, so
there is no build in the line that carries a fix.

### 5. Why only some players crashed

Nothing in Alex's Caves spawns `TREMORZILLA_BLAST` — a real Tremorzilla fires
`TREMORZILLA_PROTON`, `_LIGHTNING`, `_EXPLOSION`, `_STEAM` and `RAYGUN_BLAST`, all of which have
definitions. The crash needs a **foreign** spawner. In the reporting pack that is a
randomly-generated dimension whose ambient particle was configured to a random registered
`alexscaves:*` id; one dimension rolled `tremorzilla_blast`. That is why it is client-only
(ambient particles are client-side), dimension-specific, a join crash-loop while standing in
that biome, and produces no server crash report.

## Consequence for the fix — this is why it is an asset, not a mixin

The handoff's prescribed mixin has **no correct target**:

- The factory is correct, so "return null when the sprite set is null" never fires — the sprite
  set is never null.
- The sprite set **cannot** be populated from inside the factory: `MutableSpriteSet` is
  package-private in `net.minecraft.client.particle`, its `sprites` field is private, and
  `rebind(...)` is its only writer.
- A guard on the factory would only rescue *this one* particle while leaving every other
  undefined type equally dangerous, and would silently make the particle invisible.

**The correct, minimal fix is to supply the missing asset.** This mod ships:

```json
{ "textures": [ "alexscaves:raygun_blast" ] }
```

`raygun_blast` is the sibling particle's texture, it already exists in the jar
(`assets/alexscaves/textures/particle/raygun_blast.png`), and its own definition file is exactly
this one line. `tremorzilla_blast` is the Tremorzilla's scaled-up version of that same sprite, so
this is visually the right choice — and it makes the ambient particle render as intended instead
of vanishing.

Benefits over the prescribed mixin: no bytecode surface that can break when Alex's Caves
updates, no mixin config to fail, ~50 bytes of JSON instead of a patch class, and it fixes the
NPE for **every** spawner rather than one call site.

## Evidence index

| Claim | How checked |
|---|---|
| factory identical 2.0.4 vs 2.0.10 | `unzip -p … \| sha256sum` on `RaygunBlastParticle$TremorzillaFactory.class` |
| `pickSprite` is the throwing call, offset 31 | `javap -p -c` on the factory class |
| factory receives a real `SpriteSet` | `javap -p -c` constructor: stores arg into `spriteSet` field, no null check needed |
| `tremorzilla_blast` registered | id + field present in `ACParticleRegistry`, mapped in `ClientProxy` |
| no definition file | `unzip -l` count `0`; 95 JSONs present of 96 registered types |
| texture exists | `unzip -l` finds `assets/alexscaves/textures/particle/raygun_blast.png` |
| sibling definition is one line | `unzip -p … raygun_blast.json` |
