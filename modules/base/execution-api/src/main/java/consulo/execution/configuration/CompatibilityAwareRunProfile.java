package consulo.execution.configuration;

import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfile;
import javax.annotation.Nonnull;

public interface CompatibilityAwareRunProfile extends RunProfile {
  /**
   * Checks whether the run configuration is compatible with the configuration passed as a parameter
   * and may still run if the configuration passed as a parameter starts as well.
   * If configuration A mustBeStoppedToRun configuration B it does not necessarily means the opposite
   * so configuration B may be able to start with configuration A running.
   *
   * @param configuration the run configuration to check a compatibility to run with the current configuration.
   * @return true if the configuration can still run along side with the configuration passed as parameter, false otherwise.
   */
  boolean mustBeStoppedToRun(@Nonnull RunConfiguration configuration);
}
