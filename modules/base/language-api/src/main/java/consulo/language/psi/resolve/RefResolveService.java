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
package consulo.language.psi.resolve;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.disposer.Disposable;

import org.jspecify.annotations.Nullable;
import java.util.Collection;

public abstract class RefResolveService {
  /**
   * if true then PsiElement.getUseScope() returns scope restricted to only relevant files which are stored in {@link RefResolveService}
   */
  public static final boolean ENABLED = /*ApplicationManager.getApplication().isUnitTestMode() ||*/ Boolean.getBoolean("ref.back");

  public static RefResolveService getInstance(Project project) {
    return project.getInstance(RefResolveService.class);
  }

  /**
   *
   * @param file
   * @return null means the service has not resolved all files and is not ready yet
   */
  @Nullable
  public abstract int[] getBackwardIds(VirtualFileWithId file);

  /**
   * @return subset of scope containing only files which reference the virtualFile
   */
  
  public abstract GlobalSearchScope restrictByBackwardIds(VirtualFile virtualFile, GlobalSearchScope scope);

  /**
   * @return add files to the resolve queue. until all files from there are resolved, the service is in incomplete state and returns null from getBackwardIds()
   */
  public abstract boolean queue(Collection<VirtualFile> files, Object reason);

  public abstract boolean isUpToDate();

  public abstract int getQueueSize();
  public abstract void addListener(Disposable parent, Listener listener);
  public abstract static class Listener {
    public void fileResolved(VirtualFile virtualFile) {}
    public void allFilesResolved() {}
  }
}
