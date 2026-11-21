package network.vonix.acparticlefix;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in self-test that proves whether this particle's sprite set is bound.
 *
 * <p>Enabled only by {@code -Dacparticlefix.selftest=true} (see the {@code acselftest} Gradle
 * property); inert in normal play.
 *
 * <p>It needs no world. It goes straight to the object the crash comes from: the {@code
 * SpriteSet} that {@code ParticleEngine} holds for {@code alexscaves:tremorzilla_blast}, and
 * asks it for a sprite — the same call {@code TextureSheetParticle.pickSprite} makes. That call
 * is where {@code NullPointerException: Cannot invoke "java.util.List.size()" because
 * "this.sprites" is null} is thrown.
 *
 * <ul>
 *   <li>{@code RESULT=FAIL …} with the NPE → the sprite set is unbound, i.e. the crash is live
 *       (this is what the unpatched mod does);</li>
 *   <li>{@code RESULT=PASS …} → the sprite set is bound, so that call cannot throw, i.e. the
 *       crash is fixed.</li>
 * </ul>
 */
@EventBusSubscriber(modid = AlexsCavesParticleFix.MODID, value = Dist.CLIENT)
public final class ParticleSpawnSelfTest {

  private static final String ENABLED_PROPERTY = "acparticlefix.selftest";

  private static final Logger LOG = LoggerFactory.getLogger("acparticlefix/selftest");

  private static final ResourceLocation TARGET =
      ResourceLocation.fromNamespaceAndPath("alexscaves", "tremorzilla_blast");

  private static Boolean enabled = null;
  private static int ticksWaited = 0;
  private static boolean done = false;

  private ParticleSpawnSelfTest() {
  }

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    if (done) {
      return;
    }
    if (enabled == null) {
      enabled = Boolean.getBoolean(ENABLED_PROPERTY);
      if (!enabled) {
        done = true;
        return;
      }
    }

    // Let the initial resource reload finish; sprite sets are populated during it.
    if (ticksWaited++ < 40) {
      return;
    }
    done = true;

    try {
      runCheck();
    } catch (Throwable t) {
      LOG.error("[acparticlefix/selftest] RESULT=ERROR while inspecting the particle engine", t);
    }
  }

  /**
   * Confirms the defence-in-depth guard is really present on the loaded factory class, rather
   * than assuming the mixin applied. An absent guard must not be reported as working.
   */
  private static void verifyGuardApplied() {
    final String target =
        "com.github.alexmodguy.alexscaves.client.particle.RaygunBlastParticle$TremorzillaFactory";
    try {
      Class<?> factory = Class.forName(target);
      for (var m : factory.getDeclaredMethods()) {
        if (m.getName().contains("acparticlefix$skipUnboundSpriteSet")) {
          LOG.info("[acparticlefix/selftest] GUARD=PASS factory guard present in {}", target);
          return;
        }
      }
      LOG.warn("[acparticlefix/selftest] GUARD=ABSENT {} loaded but carries no guard; "
          + "the asset fix is still active, but the backstop is not.", target);
    } catch (ClassNotFoundException e) {
      LOG.error("[acparticlefix/selftest] GUARD=SKIPPED could not load {}", target);
    }
  }

  @SuppressWarnings("unchecked")
  private static void runCheck() throws Exception {
    Minecraft mc = Minecraft.getInstance();
    ParticleEngine engine = mc.particleEngine;
    if (engine == null) {
      LOG.error("[acparticlefix/selftest] RESULT=SKIPPED particle engine not constructed yet");
      return;
    }

    // Force-load the guard's target so the mixin is actually applied and can be inspected.
    // Alex's Caves registers its particle providers lazily, so this class may never have been
    // loaded at this point — in which case an absent guard proves nothing either way.
    verifyGuardApplied();

    // ParticleEngine.spriteSets is the map of particle id -> MutableSpriteSet. Look it up by
    // its declared generic type rather than by name, so an unobfuscated name change cannot
    // silently make this test pick the providers map instead.
    Field spriteSetsField = null;
    for (Field f : ParticleEngine.class.getDeclaredFields()) {
      if (!Map.class.isAssignableFrom(f.getType())) {
        continue;
      }
      String sig = f.getGenericType().getTypeName();
      if (sig.contains("MutableSpriteSet")) {
        spriteSetsField = f;
        break;
      }
    }
    if (spriteSetsField == null) {
      LOG.error("[acparticlefix/selftest] RESULT=SKIPPED could not locate ParticleEngine.spriteSets");
      return;
    }
    spriteSetsField.setAccessible(true);
    Map<ResourceLocation, SpriteSet> spriteSets =
        (Map<ResourceLocation, SpriteSet>) spriteSetsField.get(engine);

    SpriteSet sprites = spriteSets.get(TARGET);
    if (sprites == null) {
      LOG.error("[acparticlefix/selftest] RESULT=SKIPPED {} has no registered sprite set; "
          + "is Alex's Caves installed?", TARGET);
      return;
    }

    // Probe with get(RandomSource), not get(int,int): the latter computes
    // age * (size-1) / lifetime, which is meaningless for a single sprite and throws
    // ArithmeticException at lifetime 0 — which would have been misreported as "unbound".
    // get(RandomSource) only dereferences the sprite list, so an NPE from it means unbound.
    try {
      Object sprite = sprites.get(net.minecraft.util.RandomSource.create());
      LOG.info("[acparticlefix/selftest] RESULT=PASS {} sprite set is bound (get(random) -> {}). "
          + "The crash path cannot throw. FIX IS IN EFFECT.", TARGET, sprite);
    } catch (Throwable t) {
      LOG.error("[acparticlefix/selftest] RESULT=FAIL {} sprite set is UNBOUND: {}: {}. "
              + "The crash is live. FIX IS NOT IN EFFECT.",
          TARGET, t.getClass().getName(), t.getMessage());
    }
  }
}
