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

package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataProvider;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.dataContext.GetDataRule;
import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.util.dataholder.Key;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.usage.Usage;
import consulo.ide.impl.idea.usages.UsageDataUtil;
import consulo.usage.UsageTarget;
import consulo.usage.UsageView;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashSet;

@ExtensionImpl
public class VirtualFileArrayRule implements GetDataRule<VirtualFile[]> {
  @Nonnull
  @Override
  public Key<VirtualFile[]> getKey() {
    return VirtualFile.KEY_OF_ARRAY;
  }

  @Override
  public VirtualFile[] getData(@Nonnull final DataProvider dataProvider) {
    // Try to detect multiselection.

    Project project = dataProvider.getDataUnchecked(PlatformDataKeys.PROJECT_CONTEXT);
    if (project != null && !project.isDisposed()) {
      return ProjectRootManager.getInstance(project).getContentRoots();
    }

    Module[] selectedModules = dataProvider.getDataUnchecked(LangDataKeys.MODULE_CONTEXT_ARRAY);
    if (selectedModules != null && selectedModules.length > 0) {
      return getFilesFromModules(selectedModules);
    }

    Module selectedModule = dataProvider.getDataUnchecked(LangDataKeys.MODULE_CONTEXT);
    if (selectedModule != null && !selectedModule.isDisposed()) {
      return ModuleRootManager.getInstance(selectedModule).getContentRoots();
    }

    PsiElement[] psiElements = dataProvider.getDataUnchecked(PsiElement.KEY_OF_ARRAY);
    if (psiElements != null && psiElements.length != 0) {
      return getFilesFromPsiElements(psiElements);
    }

    // VirtualFile -> VirtualFile[]
    VirtualFile vFile = dataProvider.getDataUnchecked(PlatformDataKeys.VIRTUAL_FILE);
    if (vFile != null) {
      return new VirtualFile[]{vFile};
    }

    //

    PsiFile psiFile = dataProvider.getDataUnchecked(PsiFile.KEY);
    if (psiFile != null && psiFile.getVirtualFile() != null) {
      return new VirtualFile[]{psiFile.getVirtualFile()};
    }

    PsiElement elem = dataProvider.getDataUnchecked(PsiElement.KEY);
    if (elem != null) {
      return getFilesFromPsiElement(elem);
    }

    Usage[] usages = dataProvider.getDataUnchecked(UsageView.USAGES_KEY);
    UsageTarget[] usageTargets = dataProvider.getDataUnchecked(UsageView.USAGE_TARGETS_KEY);
    if (usages != null || usageTargets != null) {
      return UsageDataUtil.provideVirtualFileArray(usages, usageTargets);
    }

    return null;
  }


  private static VirtualFile[] getFilesFromPsiElement(PsiElement elem) {
    if (elem instanceof PsiFile) {
      VirtualFile virtualFile = ((PsiFile)elem).getVirtualFile();
      return virtualFile != null ? new VirtualFile[]{virtualFile} : null;
    }
    else if (elem instanceof PsiDirectory) {
      return new VirtualFile[]{((PsiDirectory)elem).getVirtualFile()};
    }
    else {
      PsiFile file = elem.getContainingFile();
      return file != null && file.getVirtualFile() != null ? new VirtualFile[]{file.getVirtualFile()} : null;
    }
  }

  private static VirtualFile[] getFilesFromPsiElements(PsiElement[] psiElements) {
    HashSet<VirtualFile> files = new HashSet<>();
    for (PsiElement elem : psiElements) {
      if (elem instanceof PsiDirectory) {
        files.add(((PsiDirectory)elem).getVirtualFile());
      }
      else if (elem instanceof PsiFile) {
        VirtualFile virtualFile = ((PsiFile)elem).getVirtualFile();
        if (virtualFile != null) {
          files.add(virtualFile);
        }
      }
      else if (elem instanceof PsiDirectoryContainer) {
        PsiDirectory[] dirs = ((PsiDirectoryContainer)elem).getDirectories();
        for (PsiDirectory dir : dirs) {
          files.add(dir.getVirtualFile());
        }
      }
      else {
        PsiFile file = elem.getContainingFile();
        if (file != null) {
          VirtualFile virtualFile = file.getVirtualFile();
          if (virtualFile != null) {
            files.add(virtualFile);
          }
        }
      }
    }
    VirtualFile[] result = VfsUtil.toVirtualFileArray(files);
    files.clear();
    return result;
  }

  private static VirtualFile[] getFilesFromModules(Module[] selectedModules) {
    ArrayList<VirtualFile> result = new ArrayList<>();
    for (Module selectedModule : selectedModules) {
      ContainerUtil.addAll(result, ModuleRootManager.getInstance(selectedModule).getContentRoots());
    }
    return VfsUtil.toVirtualFileArray(result);
  }
}
