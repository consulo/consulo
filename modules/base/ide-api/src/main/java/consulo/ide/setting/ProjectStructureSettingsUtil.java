/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.setting;

import consulo.content.bundle.SdkModel;
import consulo.ide.setting.bundle.SettingsSdksModel;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.project.Project;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2020-01-20
 * <p>
 * Marker for {@link ShowSettingsUtil} with return current {@link SdkModel} for dialog, or for SdkTable
 */
public interface ProjectStructureSettingsUtil {
  
  SettingsSdksModel getSdksModel();

  default @Nullable ModulesConfigurator getModulesModel(Project project) {
    return null;
  }

  default @Nullable LibrariesConfigurator getLibrariesModel(Project project) {
    return null;
  }
}
