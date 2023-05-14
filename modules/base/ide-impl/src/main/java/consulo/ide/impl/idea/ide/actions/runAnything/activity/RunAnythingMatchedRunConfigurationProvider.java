// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.activity;

import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.dataContext.DataContext;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * Implement this class if a particular run configuration should be created for matching input string.
 */
public abstract class RunAnythingMatchedRunConfigurationProvider extends RunAnythingRunConfigurationProvider {
  /**
   * Actual run configuration creation by {@code commandLine}
   *
   * @param dataContext
   * @param pattern
   * @return created run configuration
   */
  @Nonnull
  public abstract RunnerAndConfigurationSettings createConfiguration(@Nonnull DataContext dataContext, @Nonnull String pattern);

  /**
   * Returns current provider associated run configuration factory
   */
  @Nonnull
  public abstract ConfigurationFactory getConfigurationFactory();

  @Override
  public Image getHelpIcon() {
    return getConfigurationFactory().getIcon();
  }
}