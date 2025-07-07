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

package consulo.execution.impl.internal.configuration;

import consulo.application.AllIcons;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author spleaner
 */
@SuppressWarnings("ExtensionImplIsNotAnnotated")
public class UnknownConfigurationType implements ConfigurationType {

  public static final UnknownConfigurationType INSTANCE = new UnknownConfigurationType();

  public static final String NAME = "Unknown";

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO(NAME);
  }

  @Nonnull
  @Override
  public LocalizeValue getConfigurationTypeDescription() {
    return LocalizeValue.localizeTODO("Configuration which cannot be loaded due to some reasons");
  }

  @Override
  public Image getIcon() {
    return AllIcons.Actions.Help;
  }

  @Override
  @Nonnull
  public String getId() {
    return NAME;
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{new ConfigurationFactory(new UnknownConfigurationType()) {
      @Override
      public RunConfiguration createTemplateConfiguration(Project project) {
        return new UnknownRunConfiguration(this, project);
      }

      @Override
      public boolean canConfigurationBeSingleton() {
        return false;
      }
    }};
  }
}
