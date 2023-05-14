/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.options.impl;

import consulo.configurable.Configurable;
import consulo.ide.impl.base.BaseShowSettingsUtil;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 14/05/2023
 */
public interface ConfigurablePreselectStrategy {
  @Nonnull
  static ConfigurablePreselectStrategy lastStored(@Nonnull Project project) {
    return new DefaultConfigurablePreselectStrategy(project);
  }

  @Nonnull
  static ConfigurablePreselectStrategy preOrNotSelected(@Nullable Configurable configurable) {
    if (configurable == null) {
      return notSelected();
    }

    Objects.requireNonNull(configurable);
    return new ConfigurablePreselectStrategy() {
      @Override
      public Configurable get(@Nonnull Configurable[] configurables) {
        return configurable;
      }

      @Override
      public void save(@Nonnull Configurable configurable) {

      }
    };
  }

  @Nonnull
  static ConfigurablePreselectStrategy notSelected() {
    return new ConfigurablePreselectStrategy() {
      @Override
      public Configurable get(@Nonnull Configurable[] configurables) {
        return BaseShowSettingsUtil.SKIP_SELECTION_CONFIGURATION;
      }

      @Override
      public void save(@Nonnull Configurable configurable) {

      }
    };
  }

  @Nullable
  Configurable get(@Nonnull Configurable[] configurables);

  void save(@Nonnull Configurable configurable);
}
