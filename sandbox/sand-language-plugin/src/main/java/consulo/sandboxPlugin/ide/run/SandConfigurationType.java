/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.localize.LocalizeValue;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.module.extension.SandModuleExtension;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04.06.14
 */
@ExtensionImpl
public class SandConfigurationType extends ConfigurationTypeBase {
  public SandConfigurationType() {
    super("#SandConfigurationType", LocalizeValue.localizeTODO("Sand Test"), PlatformIconGroup.nodesStatic());
    addFactory(new ConfigurationFactory(this) {
      @Override
      public RunConfiguration createTemplateConfiguration(Project project) {
        return new SandConfiguration(project, this, "Unnamed");
      }

      @Override
      public boolean isApplicable(@Nonnull Project project) {
        return ModuleExtensionHelper.getInstance(project).hasModuleExtension(SandModuleExtension.class);
      }
    });
  }
}
