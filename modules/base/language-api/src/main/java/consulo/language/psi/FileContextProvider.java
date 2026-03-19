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

package consulo.language.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

import org.jspecify.annotations.Nullable;
import java.util.Collection;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.PROJECT)
public abstract class FileContextProvider {
  public static @Nullable FileContextProvider getProvider(PsiFile file) {
    for (FileContextProvider provider : file.getProject().getExtensionList(FileContextProvider.class)) {
      if (provider.isAvailable(file)) {
        return provider;
      }
    }
    return null;
  }

  protected abstract boolean isAvailable(PsiFile file);

  
  public abstract Collection<PsiFileSystemItem> getContextFolders(PsiFile file);

  public abstract @Nullable PsiFile getContextFile(PsiFile file);
}
