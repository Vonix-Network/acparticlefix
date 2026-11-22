# CurseForge release prep — Alex's Caves: Tremorzilla Crash Fix

**Status:** copy and metadata ready. **Nothing published.** Awaiting owner approval.
**Date:** 2026-11-21
**Repo:** `Vonix-Network/acparticlefix` (local checkout: `~/DEV/alexscaves-particle-fix`)

---

## Files in this directory

| File | Purpose |
|---|---|
| `DESCRIPTION.md` | Full project description — paste into the CurseForge description editor |
| `SUMMARY.txt` | The project **Summary** field (147 chars, limit 150) |
| `RELEASE-PREP.md` | This document — naming rationale, field values, pre-flight checks |

## Project name — decision needed

CurseForge **Project Name** is separate from the jar's display name; both are set below.

**Recommended: `Alex's Caves: Tremorzilla Crash Fix`** ← applied to the jar

Why this shape:

- **Leads with the upstream mod name.** Players search "Alex's Caves". A patch that does not name it
  in the first three words is invisible to exactly the audience that needs it. This was the owner's
  correction, and it was right — the previous name, *Alex's Caves Particle Crash Fix*, buried the
  actionable word ("particle crash" → what do I do?) behind a generic one.
- **Says what it fixes, not that it "fixes particles."** "Tremorzilla" is a specific, searchable noun
  that players who hit the bug will recognise from the crash log they were sent.
- **Avoids a purity trap.** `Alex's Caves Crash Fixes` would be accurate today and wrong the moment a
  second, unrelated fix lands. It also implies a bundle, which oversells a 11 KB patch.

### On "Alex's Caves CE"

Considered, not recommended. **CE** ("Community Edition") is a real convention elsewhere, but:

- It is unused across every Vonix repo — checked; no existing "CE" branding to stay consistent with.
- It reads as a **competing fork or continuation** of Alex's Caves. This is not that. It is an 11 KB
  patch that ships one JSON and one guard, and it does not replace the mod.
- It hides the purpose. "CE" does not tell a searching player anything about a crash, and CurseForge
  moderation guidance explicitly says to avoid metadata-ish and loadout-ish tokens in names.

If a "CE" family is wanted later for a *fork* (e.g. the Reforged drop-in replacement), that is the
place for it — not for a patch. Happy to use it if you want it anyway; it is one line.

## Fields to enter

**Project Name**
```
Alex's Caves: Tremorzilla Crash Fix
```

**Summary** (147 / 150 chars — `SUMMARY.txt`)
```
Client-side fix for an Alex's Caves (Unofficial Port) crash that locks players out of their world on 1.21.1. No server install, no gameplay change.
```

**Description** — paste `DESCRIPTION.md`.

**Category:** `Bug Fixes` (primary). Secondary if offered: `Utility`.

**Game version:** `1.21.1` · **Mod loader:** `NeoForge` · **Environment:** **Client** only.

**Relations (CurseForge "Relations" tab):**

| Relation | Project | Why |
|---|---|---|
| Required dependency → none | — | Do not hard-depend; the mod is inert without Alex's Caves and must stay loadable |
| Optional dependency → **Alex's Caves (Unofficial Port)** | `alexs-caves-unofficial-port` | What it patches |
| Incompatible → none | — | Nothing conflicts by design |

Deliberately **not** a required dependency on Alex's Caves: a hard dependency would block startup for
anyone who installs the patch without the mod, which contradicts the "cannot become a new crash
source" property that Gate 3 verifies.

**Licence:** LGPL-3.0-or-later (matches `LICENSE` and `gradle.properties`).

**Links:**
- Source: `https://github.com/Vonix-Network/acparticlefix`
- Issues: `https://github.com/Vonix-Network/acparticlefix/issues` *(fixed this pass — see below)*
- Upstream mod (AlexModGuy): `https://www.curseforge.com/minecraft/mc-mods/alexs-caves`
- Upstream port (Raguto): `https://www.curseforge.com/minecraft/mc-mods/alexs-caves-unofficial-port`

## Changes made this pass

1. **`issueTrackerURL` fixed.** It pointed at `https://github.com/Vonix-Network/alexscaves-particle-fix/issues`,
   which **404s** — that repo does not exist (the checkout is merely *named* that locally; its remote
   is `acparticlefix`). Now points at the real repo. Verified: `gh api …/acparticlefix/issues` returns
   `0`, not 404. Shipping the old value would have sent every bug report to a dead page.
2. **Display name leads with the upstream mod** (`gradle.properties` `mod_name`).
3. **`docs/ACCEPTANCE.md` Gate 5 corrected.** It recorded `sha256 83f218d3…` and asserted "this mod
   has no mixins". Both were true only of the pre-guard asset-only build; the shipped jar has four
   mixin classes and a different hash. Corrected to the published artifact
   (`39cd6fd3…`, 11,328 bytes) with a note explaining the stale values.
4. **Description and README now name both upstream projects** and state plainly that this is an
   unofficial community patch, not affiliated with or supported by AlexModGuy or Raguto.

### Rebuild note

Items 1–2 change jar contents, so the artifact hash moved:
- previously published `v1.21.1-1.0.0`: `39cd6fd3da6322a024af3f38b56e51f5ef1721433835d06be8a699fba454381d`
- rebuilt (name + issue URL fixed): `e1f794c5ff3dde3d1b5683771178c750a7790c43aabd6829206dc5df4e7c4d72`

Behaviour is identical — the mixin and asset are untouched. The CurseForge upload must use the
rebuilt jar so the metadata matches the listing, and the release's `SHA256SUMS` must be regenerated.

## Pre-flight before upload

- [ ] Owner approves the project name
- [ ] Owner approves publication
- [ ] Re-upload the rebuilt jar (hash above), regenerate `SHA256SUMS`
- [ ] Paste `SUMMARY.txt` into the Summary field and `DESCRIPTION.md` into Description
- [ ] Set Environment = **Client**, loader = NeoForge, game version = 1.21.1
- [ ] Add optional dependency on `alexs-caves-unofficial-port`
- [ ] Confirm the description renders (tables/lists are plain markdown, safe in the editor)

## Not done, deliberately

- **No pack-owner message drafted yet.** It depends on the final name and the CurseForge URL, neither
  of which exist until publication.
- **No KB/wiki entry.** Owner rule: KB changes only on explicit request.
- **No release notes rewritten.** The existing `v1.21.1-1.0.0` release body is accurate; only the
  attached jar and `SHA256SUMS` need replacing.
