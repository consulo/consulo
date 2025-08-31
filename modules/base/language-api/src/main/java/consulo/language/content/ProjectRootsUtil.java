/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.content;

import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.content.TestSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiCodeFragment;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author anna
 * @since 2008-01-17
 */
public class ProjectRootsUtil {
  public static boolean isSourceRoot(PsiDirectory psiDirectory) {
    return isSourceRoot(psiDirectory.getVirtualFile(), psiDirectory.getProject());
  }

  public static boolean isSourceRoot(VirtualFile directoryFile, Project project) {
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    return directoryFile.equals(fileIndex.getSourceRootForFile(directoryFile));
  }

  public static boolean isInSource(PsiDirectory directory) {
    return isInSource(directory.getVirtualFile(), directory.getProject());
  }

  public static boolean isInSource(VirtualFile directoryFile, Project project) {
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    return projectFileIndex.isInSourceContent(directoryFile);
  }

  public static boolean isInTestSource(PsiDirectory psiDirectory) {
    return isInTestSource(psiDirectory.getVirtualFile(), psiDirectory.getProject());
  }

  public static boolean isInTestSource(VirtualFile directoryFile, Project project) {
    return TestSourcesFilter.isTestSources(directoryFile, project);
  }

  public static boolean isInTestResource(PsiDirectory psiDirectory) {
    return isInTestResource(psiDirectory.getVirtualFile(), psiDirectory.getProject());
  }

  public static boolean isInTestResource(VirtualFile directoryFile, Project project) {
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    return projectFileIndex.isInTestResource(directoryFile);
  }

  public static boolean isSourceOrTestRoot(@Nonnull VirtualFile virtualFile, Project project) {
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    return projectFileIndex.isInSource(virtualFile);
  }

  @Nullable
  public static ContentFolder getModuleSourceRoot(@Nonnull VirtualFile root, @Nonnull Project project) {
    return findContentFolderForDirectory(root, project);
  }

  public static boolean isModuleSourceRoot(@Nonnull VirtualFile virtualFile, @Nonnull Project project) {
    return getModuleSourceRoot(virtualFile, project) != null;
  }

  @Nullable
  public static ContentFolder findContentFolderForDirectory(@Nonnull VirtualFile virtualFile, Project project) {
    return findContentFolderForDirectory(ProjectRootManager.getInstance(project).getFileIndex(), virtualFile);
  }

  @Nullable
  public static ContentFolder findContentFolderForDirectory(@Nonnull ProjectFileIndex projectFileIndex, @Nonnull VirtualFile virtualFile) {
    Module module = projectFileIndex.getModuleForFile(virtualFile);
    if (module == null) {
      return null;
    }

    ContentFolder contentFolder = projectFileIndex.getContentFolder(virtualFile);
    if (contentFolder == null) {
      return null;
    }
    return virtualFile.equals(contentFolder.getFile()) ? contentFolder : null;
  }

  public static boolean isLibraryRoot(VirtualFile directoryFile, Project project) {
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    if (projectFileIndex.isInLibraryClasses(directoryFile)) {
      VirtualFile parent = directoryFile.getParent();
      return parent == null || !projectFileIndex.isInLibraryClasses(parent);
    }
    return false;
  }

  public static boolean isModuleContentRoot(PsiDirectory directory) {
    return isModuleContentRoot(directory.getVirtualFile(), directory.getProject());
  }

  public static boolean isModuleContentRoot(@Nonnull VirtualFile directoryFile, Project project) {
    return isModuleContentRoot(ProjectRootManager.getInstance(project).getFileIndex(), directoryFile);
  }

  public static boolean isModuleContentRoot(@Nonnull ProjectFileIndex projectFileIndex, @Nonnull VirtualFile directoryFile) {
    VirtualFile contentRootForFile = projectFileIndex.getContentRootForFile(directoryFile);
    return directoryFile.equals(contentRootForFile);
  }

  public static boolean isProjectHome(PsiDirectory psiDirectory) {
    return psiDirectory.getVirtualFile().equals(psiDirectory.getProject().getBaseDir());
  }

  public static boolean isOutsideSourceRoot(@Nullable PsiFile psiFile) {
    if (psiFile == null) return false;
    if (psiFile instanceof PsiCodeFragment) return false;
    VirtualFile file = psiFile.getVirtualFile();
    if (file == null) return false;
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(psiFile.getProject()).getFileIndex();
    return !projectFileIndex.isInSource(file) && !projectFileIndex.isInLibraryClasses(file);
  }
}