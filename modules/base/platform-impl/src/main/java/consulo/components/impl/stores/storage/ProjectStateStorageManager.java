/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.components.impl.stores.storage;

import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;

public class ProjectStateStorageManager extends StateStorageManagerImpl {
  @NonNls
  protected static final String ROOT_TAG_NAME = "project";

  public ProjectStateStorageManager(Project project, TrackingPathMacroSubstitutor macroSubstitutor) {
    super(macroSubstitutor, ROOT_TAG_NAME, project, project::getMessageBus, StateStorageFacade.CONSULO_VFS);
  }

  @Nonnull
  @Override
  protected String getConfigurationMacro(boolean directorySpec) {
    return StoragePathMacros.PROJECT_CONFIG_DIR;
  }
}
