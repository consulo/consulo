/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.impl.internal.compound;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.execution.configuration.RunConfiguration;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.ImageEffects;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class CompoundRunConfigurationType extends ConfigurationTypeBase {

  public static CompoundRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(CompoundRunConfigurationType.class);
  }

  public CompoundRunConfigurationType() {
    super("CompoundRunConfigurationType", LocalizeValue.localizeTODO("Compound"), LocalizeValue.localizeTODO(
      "It runs batch of run configurations at once"), ImageEffects.layered(AllIcons.Nodes.Folder, AllIcons.Nodes.RunnableMark));
    addFactory(new ConfigurationFactory(this) {
      @Override
      public RunConfiguration createTemplateConfiguration(Project project) {
        return new CompoundRunConfiguration(project, CompoundRunConfigurationType.this, "Compound Run Configuration");
      }

      @Nonnull
      @Override
      public String getId() {
        // this is hardcode string as id - never localize it
        return "Compound Run Configuration";
      }

      @Override
      public boolean isConfigurationSingletonByDefault() {
        return true;
      }

      @Override
      public boolean canConfigurationBeSingleton() {
        return false;
      }
    });
  }
}
