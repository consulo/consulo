/*
 * Copyright 2013-2021 consulo.io
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
package consulo.roots.ui.configuration;

import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 19/04/2021
 */
public interface ModulesConfigurator extends ModulesProvider {
  @Nonnull
  String getRealName(final Module module);

  boolean isModuleModelCommitted();

  @Nullable
  ModifiableRootModel getModuleEditorModelProxy(Module module);

  ModifiableModuleModel getModuleModel();

  @Nonnull
  String getCompilerOutputUrl();

  void setCompilerOutputUrl(@Nullable String compilerOutputUrl);

  void processModuleCompilerOutputChanged(String baseUrl);
}
