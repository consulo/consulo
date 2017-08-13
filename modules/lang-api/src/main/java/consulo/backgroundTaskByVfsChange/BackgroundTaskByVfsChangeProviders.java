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
package consulo.backgroundTaskByVfsChange;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import consulo.backgroundTaskByVfsChange.BackgroundTaskByVfsChangeProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 01.05.14
 */
public class BackgroundTaskByVfsChangeProviders {
  @NotNull
  public static List<BackgroundTaskByVfsChangeProvider> getProviders(@NotNull Project project, @NotNull VirtualFile virtualFile) {
    List<BackgroundTaskByVfsChangeProvider> providers = new SmartList<BackgroundTaskByVfsChangeProvider>();
    for (BackgroundTaskByVfsChangeProvider provider : BackgroundTaskByVfsChangeProvider.EP_NAME.getExtensions()) {
      if (provider.validate(project, virtualFile)) {
        providers.add(provider);
      }
    }
    return providers.isEmpty() ? Collections.<BackgroundTaskByVfsChangeProvider>emptyList() : providers;
  }
}
