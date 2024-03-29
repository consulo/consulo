/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.ide.impl.idea.execution.configurations;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.ModuleRunConfiguration;
import consulo.module.Module;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @deprecated
 */
public abstract class RuntimeConfiguration extends LocatableConfigurationBase implements Cloneable, ModuleRunConfiguration {
  protected RuntimeConfiguration(final String name, final Project project, final ConfigurationFactory factory) {
    super(project, factory, name);
  }

  @Override
  @Nonnull
  public Module[] getModules() {
    return Module.EMPTY_ARRAY;
  }

  @Override
  public RuntimeConfiguration clone() {
    return (RuntimeConfiguration)super.clone();
  }

  /**
   * @deprecated use {@link #suggestedName()} instead
   */
  @Nullable
  public String getGeneratedName() {
    return suggestedName();
  }
}
