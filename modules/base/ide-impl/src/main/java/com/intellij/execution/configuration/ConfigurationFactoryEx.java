/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.execution.configuration;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class ConfigurationFactoryEx extends ConfigurationFactory {
  protected ConfigurationFactoryEx(@Nonnull ConfigurationType type) {
    super(type);
  }

  public void onNewConfigurationCreated(@Nonnull RunConfiguration configuration) {
  }

  public void onConfigurationCopied(@Nonnull RunConfiguration configuration) {
  }
}
