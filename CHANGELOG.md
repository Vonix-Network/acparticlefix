# Changelog

All notable changes to Alex's Caves Particle Crash Fix.

## [1.21.1-1.0.0] — 2026-11-21

### Added

- Client-side fix for the `alexscaves:tremorzilla_blast` particle crash
  (`NullPointerException: Cannot invoke "java.util.List.size()" because "this.sprites" is
  null`). The mod supplies the particle definition that Alex's Caves (Unofficial Port) omits,
  binding the particle to the existing `raygun_blast` texture. The crash is fixed at its
  source, for every spawner.

### Notes

- Client-only. Registers nothing, opens no network channel, changes no gameplay. Safe to use on
  a server that does not have it, alongside players who do not have it.
- Inert when Alex's Caves is absent, and a no-op if a future Alex's Caves build ships its own
  definition for this particle.
- Verified against NeoForge 21.1.267 / MC 1.21.1 with Alex's Caves (Unofficial Port) 2.0.4.
  Affects every 2.0.x build: the particle factory is byte-identical across them and none ships
  the definition.
