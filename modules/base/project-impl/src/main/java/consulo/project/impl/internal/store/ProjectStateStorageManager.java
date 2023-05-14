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
package consulo.project.impl.internal.store;

import consulo.application.Application;
import consulo.component.persist.StateSplitterEx;
import consulo.component.persist.StoragePathMacros;
import consulo.component.store.impl.internal.PathMacrosService;
import consulo.component.store.impl.internal.TrackingPathMacroSubstitutor;
import consulo.component.store.impl.internal.storage.StateStorageFacade;
import consulo.component.store.impl.internal.storage.StateStorageManagerImpl;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

public class ProjectStateStorageManager extends StateStorageManagerImpl {
  protected static final String ROOT_TAG_NAME = "project";

  private final Application myApplication;

  public ProjectStateStorageManager(Project project, TrackingPathMacroSubstitutor macroSubstitutor, PathMacrosService pathMacroManager) {
    super(macroSubstitutor, ROOT_TAG_NAME, project, project::getMessageBus, ()-> pathMacroManager,StateStorageFacade.CONSULO_VFS);
    myApplication = project.getApplication();
  }

  @Nonnull
  @Override
  public StateSplitterEx createSplitter(Class<? extends StateSplitterEx> splitter) {
    return myApplication.getUnbindedInstance(splitter);
  }

  @Nonnull
  @Override
  protected String getConfigurationMacro(boolean directorySpec) {
    return StoragePathMacros.PROJECT_CONFIG_DIR;
  }
}
