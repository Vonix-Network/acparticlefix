package network.vonix.acparticlefix.mixin;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Defence in depth: never let the Tremorzilla blast factory construct a particle whose sprite
 * set cannot produce a sprite.
 *
 * <p>The primary fix in this mod is the missing {@code assets/alexscaves/particles/
 * tremorzilla_blast.json}, which binds the sprite set before anything can spawn the particle and
 * therefore fixes the crash at its source for every spawner. This mixin is the second line of
 * defence for the cases the asset cannot cover:
 *
 * <ul>
 *   <li>another resource pack overrides the definition with one that binds no sprites;</li>
 *   <li>a mod loads after this one and replaces the definition;</li>
 *   <li>this mod's asset is stripped from a repack.</li>
 * </ul>
 *
 * <p>Vanilla tolerates a {@code null} return from a particle provider
 * ({@code ParticleEngine.makeParticle} simply skips the particle), so returning {@code null}
 * degrades to "this one particle is missing" instead of a client crash. The guard runs
 * <b>before</b> the particle is constructed, so nothing is allocated and nothing else changes.
 *
 * <p>Applies only to the pinned Alex's Caves build (see {@code AcParticleFixMixinPlugin}); on
 * any other build it is skipped and the mod falls back to the asset fix alone.
 */
@Mixin(targets = "com.github.alexmodguy.alexscaves.client.particle.RaygunBlastParticle$TremorzillaFactory")
public abstract class TremorzillaFactoryMixin {

  @Shadow
  private SpriteSet spriteSet;

  @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, remap = false)
  private void acparticlefix$skipUnboundSpriteSet(CallbackInfoReturnable<Object> cir) {
    if (!AcParticleFixSprites.isBound(this.spriteSet)) {
      cir.setReturnValue(null);
    }
  }

  /** Sprite-boundness probe used by both the guard and the self-test. */
  public static final class AcParticleFixSprites {

    private AcParticleFixSprites() {
    }

    /**
     * @return true when the set can produce a sprite. Deliberately probes with
     *     {@code get(RandomSource)}: {@code get(int, int)} computes
     *     {@code age * (size - 1) / lifetime}, which is meaningless for a single sprite and
     *     divides by zero when lifetime is 0.
     */
    public static boolean isBound(SpriteSet set) {
      if (set == null) {
        return false;
      }
      try {
        return set.get(RandomSource.create()) != null;
      } catch (RuntimeException e) {
        return false;
      }
    }
  }
}
