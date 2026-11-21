package network.vonix.acparticlefix.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the Tremorzilla factory guard only on the Alex's Caves builds it was written against,
 * and never lets the guard become a new crash source.
 *
 * <p>The asset fix in this mod works on any 2.0.x build regardless. The guard is narrower: it
 * reaches into a specific class, so on an unknown build it is skipped with one WARN and Alex's
 * Caves is left exactly as it was.
 *
 * <p>Only {@code alexscaves} 2.0.x is accepted. If Alex's Caves is absent there is no class to
 * mix into, and Mixin simply has nothing to do.
 */
public final class AcParticleFixMixinPlugin implements IMixinConfigPlugin {

  /** Alex's Caves (Unofficial Port) versions whose factory shape the guard was written for. */
  private static final String SUPPORTED_PREFIX = "2.0.";

  private static final Logger LOG = LoggerFactory.getLogger("acparticlefix/mixin");

  private static volatile boolean active = false;
  private static volatile boolean warned = false;

  /** True when the guard was allowed to apply. */
  public static boolean guardActive() {
    return active;
  }

  @Override
  public void onLoad(String mixinPackage) {
    // The guard patches a client-only particle factory. On a dedicated server there is nothing
    // to patch and nothing to warn about, so stay silent rather than claiming to be "enabled".
    if (net.neoforged.fml.loading.FMLEnvironment.dist
        != net.neoforged.api.distmarker.Dist.CLIENT) {
      return;
    }
    String version = detectAlexsCavesVersion();
    if (version == null) {
      LOG.info("[acparticlefix] Alex's Caves not present; the factory guard has nothing to patch.");
      return;
    }
    if (!version.startsWith(SUPPORTED_PREFIX)) {
      warnOnce("Alex's Caves " + version + " is not a 2.0.x build; the factory guard is "
          + "skipped and Alex's Caves is left unchanged. The particle definition this mod "
          + "supplies still applies if it is needed.");
      return;
    }
    active = true;
    LOG.info("[acparticlefix] Alex's Caves {} accepted; factory guard enabled as a backstop.",
        version);
  }

  @Override
  public String getRefMapperConfig() {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
    if (!active) {
      if (!warned && detectAlexsCavesVersion() != null) {
        warnOnce("factory guard not applied (unrecognised Alex's Caves build); "
            + "Alex's Caves is left unchanged.");
      }
      return false;
    }
    return true;
  }

  private static String detectAlexsCavesVersion() {
    try {
      var modFile = FMLLoader.getLoadingModList().getModFileById("alexscaves");
      return modFile == null ? null : modFile.versionString();
    } catch (Throwable t) {
      // Loading list not ready this early: do not guess.
      return null;
    }
  }

  private static void warnOnce(String message) {
    if (!warned) {
      warned = true;
      LOG.warn("[acparticlefix] {}", message);
    }
  }

  @Override
  public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
  }

  @Override
  public List<String> getMixins() {
    return null;
  }

  @Override
  public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                       IMixinInfo mixinInfo) {
  }

  @Override
  public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                        IMixinInfo mixinInfo) {
  }
}
