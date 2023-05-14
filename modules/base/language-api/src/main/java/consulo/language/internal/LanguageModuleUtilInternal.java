/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 29/04/2023
 */
public class LanguageModuleUtilInternal {
  @Nullable
  @RequiredReadAction
  public static Module findModuleForPsiElement(@Nonnull PsiElement element) {
    if (!element.isValid()) return null;

    Project project = element.getProject();
    if (project.isDefault()) return null;
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

    if (element instanceof PsiFileSystemItem && (!(element instanceof PsiFile) || element.getContext() == null)) {
      VirtualFile vFile = ((PsiFileSystemItem)element).getVirtualFile();
      if (vFile == null) {
        PsiFile containingFile = element.getContainingFile();
        vFile = containingFile == null ? null : containingFile.getOriginalFile().getVirtualFile();
        if (vFile == null) {
          return element.getUserData(ModuleUtilCore.KEY_MODULE);
        }
      }
      if (fileIndex.isInLibrarySource(vFile) || fileIndex.isInLibraryClasses(vFile)) {
        final List<OrderEntry> orderEntries = fileIndex.getOrderEntriesForFile(vFile);
        if (orderEntries.isEmpty()) {
          return null;
        }
        if (orderEntries.size() == 1) {
          return orderEntries.get(0).getOwnerModule();
        }
        Set<Module> modules = new HashSet<>();
        for (OrderEntry orderEntry : orderEntries) {
          modules.add(orderEntry.getOwnerModule());
        }
        final Module[] candidates = modules.toArray(new Module[modules.size()]);
        Arrays.sort(candidates, ModuleManager.getInstance(project).moduleDependencyComparator());
        return candidates[0];
      }
      return fileIndex.getModuleForFile(vFile);
    }
    PsiFile containingFile = element.getContainingFile();
    if (containingFile != null) {
      PsiElement context;
      while ((context = containingFile.getContext()) != null) {
        final PsiFile file = context.getContainingFile();
        if (file == null) break;
        containingFile = file;
      }

      if (containingFile.getUserData(ModuleUtilCore.KEY_MODULE) != null) {
        return containingFile.getUserData(ModuleUtilCore.KEY_MODULE);
      }

      final PsiFile originalFile = containingFile.getOriginalFile();
      if (originalFile.getUserData(ModuleUtilCore.KEY_MODULE) != null) {
        return originalFile.getUserData(ModuleUtilCore.KEY_MODULE);
      }

      final VirtualFile virtualFile = originalFile.getVirtualFile();
      if (virtualFile != null) {
        return fileIndex.getModuleForFile(virtualFile);
      }
    }

    return element.getUserData(ModuleUtilCore.KEY_MODULE);
  }
}
