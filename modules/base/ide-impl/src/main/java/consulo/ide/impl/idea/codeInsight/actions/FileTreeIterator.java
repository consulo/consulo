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
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import jakarta.annotation.Nonnull;

import java.util.*;

public class FileTreeIterator {
  private Queue<PsiFile> myCurrentFiles = new LinkedList<>();
  private Queue<PsiDirectory> myCurrentDirectories = new LinkedList<>();

  public FileTreeIterator(@Nonnull List<PsiFile> files) {
    myCurrentFiles.addAll(files);
  }

  @RequiredReadAction
  public FileTreeIterator(@Nonnull Module module) {
    myCurrentDirectories.addAll(collectModuleDirectories(module));
    expandDirectoriesUntilFilesNotEmpty();
  }

  public FileTreeIterator(@Nonnull Project project) {
    myCurrentDirectories.addAll(collectProjectDirectories(project));
    expandDirectoriesUntilFilesNotEmpty();
  }

  @Nonnull
  @RequiredReadAction
  public static List<PsiDirectory> collectProjectDirectories(@Nonnull Project project) {
    List<PsiDirectory> directories = new ArrayList<>();

    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      directories.addAll(collectModuleDirectories(module));
    }

    return directories;
  }

  public FileTreeIterator(@Nonnull PsiDirectory directory) {
    myCurrentDirectories.add(directory);
    expandDirectoriesUntilFilesNotEmpty();
  }

  public FileTreeIterator(@Nonnull FileTreeIterator fileTreeIterator) {
    myCurrentFiles = new LinkedList<>(fileTreeIterator.myCurrentFiles);
    myCurrentDirectories = new LinkedList<>(fileTreeIterator.myCurrentDirectories);
  }

  @Nonnull
  public PsiFile next() {
    if (myCurrentFiles.isEmpty()) {
      throw new NoSuchElementException();
    }
    PsiFile current = myCurrentFiles.poll();
    expandDirectoriesUntilFilesNotEmpty();
    return current;
  }

  public boolean hasNext() {
    return !myCurrentFiles.isEmpty();
  }

  @RequiredReadAction
  private void expandDirectoriesUntilFilesNotEmpty() {
    while (myCurrentFiles.isEmpty() && !myCurrentDirectories.isEmpty()) {
      PsiDirectory dir = myCurrentDirectories.poll();
      expandDirectory(dir);
    }
  }

  @RequiredReadAction
  private void expandDirectory(@Nonnull PsiDirectory dir) {
    Collections.addAll(myCurrentFiles, dir.getFiles());
    Collections.addAll(myCurrentDirectories, dir.getSubdirectories());
  }

  @Nonnull
  @RequiredReadAction
  public static List<PsiDirectory> collectModuleDirectories(Module module) {
    List<PsiDirectory> dirs = new ArrayList<>();

    VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
    for (VirtualFile root : contentRoots) {
      PsiDirectory dir = PsiManager.getInstance(module.getProject()).findDirectory(root);
      if (dir != null) {
        dirs.add(dir);
      }
    }

    return dirs;
  }
}
