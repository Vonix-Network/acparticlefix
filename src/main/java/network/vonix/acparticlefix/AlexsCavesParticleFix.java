package network.vonix.acparticlefix;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side fix for the Alex's Caves {@code tremorzilla_blast} particle crash.
 *
 * <p><b>What actually breaks:</b> {@code alexscaves:tremorzilla_blast} is a registered
 * particle type with a registered client provider, but the mod ships no
 * {@code assets/alexscaves/particles/tremorzilla_blast.json}. Vanilla only calls
 * {@code MutableSpriteSet.rebind(...)} for particle types that have a definition file, so
 * this type's sprite set stays {@code null}. The first time anything spawns the particle,
 * {@code TextureSheetParticle.pickSprite} dereferences that null set and throws
 * {@code NullPointerException: Cannot invoke "java.util.List.size()" because "this.sprites" is null}.
 *
 * <p><b>The fix:</b> this mod ships that missing definition, binding the particle to the
 * already-existing {@code alexscaves:raygun_blast} texture that its sibling particle uses.
 * The sprite set is therefore populated before anything can spawn it, at the true source of
 * the NPE, for every spawner — not just the one that happened to crash.
 *
 * <p><b>Why an asset and not a mixin:</b> the factory code is correct and byte-identical
 * across every affected build; the sprite set cannot be populated from inside the factory
 * because {@code MutableSpriteSet} is package-private with a private field and {@code rebind}
 * as its only writer. Guarding the factory would only paper over this particle while leaving
 * any other undefined type equally dangerous. Supplying the asset is smaller, has no bytecode
 * surface to break on a mod update, and makes the particle render as intended instead of
 * silently vanishing.
 *
 * <p>Client-only by construction: this mod registers nothing (no blocks, items, entities,
 * particles or payloads), opens no network channel, and its only effect is a client resource
 * that no server can observe.
 */
@Mod(value = AlexsCavesParticleFix.MODID, dist = Dist.CLIENT)
public final class AlexsCavesParticleFix {

  public static final String MODID = "acparticlefix";

  private static final Logger LOG = LoggerFactory.getLogger("acparticlefix");

  public AlexsCavesParticleFix(IEventBus modBus, ModContainer container) {
    // Guard against ever being started on a dedicated server. The @Mod dist already
    // prevents loading there; this is belt-and-braces so a mispackaged build can never
    // take a server down.
    if (FMLEnvironment.dist != Dist.CLIENT) {
      LOG.warn("[acparticlefix] not on a client; this mod does nothing here.");
      return;
    }
    LOG.info("[acparticlefix] active: supplying the missing alexscaves:tremorzilla_blast "
        + "particle definition (client-side, no gameplay change).");
  }
}
