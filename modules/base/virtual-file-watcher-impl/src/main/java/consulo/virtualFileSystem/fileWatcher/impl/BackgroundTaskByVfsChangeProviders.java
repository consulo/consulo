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
package consulo.virtualFileSystem.fileWatcher.impl;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeProvider;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 01.05.14
 */
public class BackgroundTaskByVfsChangeProviders {
  @Nonnull
  public static List<BackgroundTaskByVfsChangeProvider> getProviders(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
    List<BackgroundTaskByVfsChangeProvider> providers = new ArrayList<>();
    project.getApplication().getExtensionPoint(BackgroundTaskByVfsChangeProvider.class).forEachExtensionSafe(provider -> {
      if (provider.validate(project, virtualFile)) {
        providers.add(provider);
      }
    });
    return providers.isEmpty() ? Collections.<BackgroundTaskByVfsChangeProvider>emptyList() : providers;
  }
}
