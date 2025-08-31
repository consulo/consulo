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

package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.application.ApplicationManager;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.project.Project;
import consulo.application.util.function.Computable;
import consulo.project.ui.view.internal.AbstractUrl;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiManager;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nullable;

/**
 * @author cdr
 */
public class DirectoryUrl extends AbstractUrl {
  @NonNls private static final String ELEMENT_TYPE = "directory";

  public DirectoryUrl(String url, String moduleName) {
    super(url, moduleName,ELEMENT_TYPE);
  }
  public static DirectoryUrl create(PsiDirectory directory) {
    Project project = directory.getProject();
    VirtualFile virtualFile = directory.getVirtualFile();
    Module module = ModuleUtil.findModuleForFile(virtualFile, project);
    return new DirectoryUrl(virtualFile.getUrl(), module != null ? module.getName() : null);
  }

  @Override
  public Object[] createPath(final Project project) {
    if (moduleName != null) {
      Module module = ApplicationManager.getApplication().runReadAction(new Computable<Module>() {
        @Nullable
        @Override
        public Module compute() {
          return ModuleManager.getInstance(project).findModuleByName(moduleName);
        }
      });
      if (module == null) return null;
    }
    VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
    final VirtualFile file = virtualFileManager.findFileByUrl(url);
    if (file == null) return null;
    PsiDirectory directory = ApplicationManager.getApplication().runReadAction(new Computable<PsiDirectory>() {
      @Nullable
      @Override
      public PsiDirectory compute() {
        return PsiManager.getInstance(project).findDirectory(file);
      }
    });
    if (directory == null) return null;
    return new Object[]{directory};
  }

  @Override
  protected AbstractUrl createUrl(String moduleName, String url) {
      return new DirectoryUrl(url, moduleName);
  }

  @Override
  public AbstractUrl createUrlByElement(Object element) {
    if (element instanceof PsiDirectory) {
      return create((PsiDirectory)element);
    }
    return null;
  }
}
