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

  @SuppressWarnings("unchecked")
  private static void runCheck() throws Exception {
    Minecraft mc = Minecraft.getInstance();
    ParticleEngine engine = mc.particleEngine;
    if (engine == null) {
      LOG.error("[acparticlefix/selftest] RESULT=SKIPPED particle engine not constructed yet");
      return;
    }

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

    // Exactly what TextureSheetParticle.pickSprite ends up calling.
    try {
      Object sprite = sprites.get(0, 1);
      LOG.info("[acparticlefix/selftest] RESULT=PASS {} sprite set is bound (get(0,1) -> {}). "
          + "The crash path cannot throw. FIX IS IN EFFECT.", TARGET, sprite);
    } catch (Throwable t) {
      LOG.error("[acparticlefix/selftest] RESULT=FAIL {} sprite set is UNBOUND: {}: {}. "
              + "The crash is live. FIX IS NOT IN EFFECT.",
          TARGET, t.getClass().getName(), t.getMessage());
    }
  }
}
