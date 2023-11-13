// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.application.ReadAction;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.internal.CompactVirtualFileSet;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.application.util.function.Processor;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class FileRecursiveIterator {
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final Collection<? extends VirtualFile> myRoots;

  FileRecursiveIterator(@Nonnull Project project, @Nonnull List<? extends PsiFile> roots) {
    this(project, ContainerUtil.<PsiFile, VirtualFile>map(roots, psiDir -> psiDir.getVirtualFile()));
  }

  FileRecursiveIterator(@Nonnull Module module) {
    this(module.getProject(), ContainerUtil.<PsiDirectory, VirtualFile>map(collectModuleDirectories(module), psiDir -> psiDir.getVirtualFile()));
  }

  FileRecursiveIterator(@Nonnull Project project) {
    this(project, ContainerUtil.<PsiDirectory, VirtualFile>map(collectProjectDirectories(project), psiDir -> psiDir.getVirtualFile()));
  }

  FileRecursiveIterator(@Nonnull PsiDirectory directory) {
    this(directory.getProject(), Collections.singletonList(directory.getVirtualFile()));
  }

  FileRecursiveIterator(@Nonnull Project project, @Nonnull Collection<? extends VirtualFile> roots) {
    myProject = project;
    myRoots = roots;
  }

  @Nonnull
  static List<PsiDirectory> collectProjectDirectories(@Nonnull Project project) {
    Module[] modules = ModuleManager.getInstance(project).getModules();
    List<PsiDirectory> directories = new ArrayList<>(modules.length * 3);
    for (Module module : modules) {
      directories.addAll(collectModuleDirectories(module));
    }

    return directories;
  }

  boolean processAll(@Nonnull Processor<? super PsiFile> processor) {
    CompactVirtualFileSet visited = new CompactVirtualFileSet();
    for (VirtualFile root : myRoots) {
      if (!ProjectRootManager.getInstance(myProject).getFileIndex().iterateContentUnderDirectory(root, fileOrDir -> {
        if (fileOrDir.isDirectory() || !visited.add(fileOrDir)) {
          return true;
        }
        PsiFile psiFile = ReadAction.compute(() -> myProject.isDisposed() ? null : PsiManager.getInstance(myProject).findFile(fileOrDir));
        return psiFile == null || processor.process(psiFile);
      })) return false;
    }
    return true;
  }

  @Nonnull
  static List<PsiDirectory> collectModuleDirectories(Module module) {
    VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
    return ReadAction.compute(() -> ContainerUtil.mapNotNull(contentRoots, root -> PsiManager.getInstance(module.getProject()).findDirectory(root)));
  }
}
