/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.roots;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Provides information about files contained in a project.
 *
 * @see ProjectRootManager#getFileIndex()
 */
public interface ProjectFileIndex extends FileIndex {
  @Deprecated
  class SERVICE {
    private SERVICE() {
    }

    public static ProjectFileIndex getInstance(Project project) {
      return ServiceManager.getService(project, ProjectFileIndex.class);
    }
  }

  @Nonnull
  static ProjectFileIndex getInstance(Project project) {
    return ServiceManager.getService(project, ProjectFileIndex.class);
  }

  /**
   * Returns module to which the specified file belongs.
   *
   * @param file the file for which the module is requested.
   * @return the module instance or null if the file does not belong to content of any module.
   */
  @Nullable
  Module getModuleForFile(@Nonnull VirtualFile file);

  /**
   * Returns module to which the specified file belongs.
   *
   * @param file           the file for which the module is requested.
   * @param honorExclusion if {@code false} the containing module will be returned even if the file is located under a folder marked as excluded
   * @return the module instance or null if the file does not belong to content of any module.
   */
  @Nullable
  Module getModuleForFile(@Nonnull VirtualFile file, boolean honorExclusion);

  /**
   * Return content folder if virtual file equal to content folder directory (don't check parent directories)
   */
  @Nullable
  ContentFolder getContentFolder(@Nonnull VirtualFile file);

  /**
   * Returns the order entries which contain the specified file (either in CLASSES or SOURCES).
   *
   * @param file the file for which the order entries are requested.
   * @return the array of order entries containing the file.
   */
  @Nonnull
  List<OrderEntry> getOrderEntriesForFile(@Nonnull VirtualFile file);

  /**
   * Returns a classpath entry to which the specified file or directory belongs.
   *
   * @param file the file or directory for which the information is requested.
   * @return the file for the classpath entry, or null if the file is not a compiled
   * class file or directory belonging to a library.
   */
  @Nullable
  VirtualFile getClassRootForFile(@Nonnull VirtualFile file);

  /**
   * Returns the module source root or library source root to which the specified file
   * or directory belongs.
   *
   * @param file the file or directory for which the information is requested.
   * @return the file for the source root, or null if the file is not located under any
   * of the source roots for the module.
   */
  @Nullable
  VirtualFile getSourceRootForFile(@Nonnull VirtualFile file);

  /**
   * Returns the module content root to which the specified file or directory belongs.
   *
   * @param file the file or directory for which the information is requested.
   * @return the file for the content root, or null if the file does not belong to this project.
   */
  @Nullable
  VirtualFile getContentRootForFile(@Nonnull VirtualFile file);

  /**
   * Returns the module content root to which the specified file or directory belongs.
   *
   * @param file           the file or directory for which the information is requested.
   * @param honorExclusion if {@code false} the containing content root will be returned even if the file is located under a folder marked as excluded
   * @return the file for the content root, or null if the file does not belong to this project.
   */
  @Nullable
  VirtualFile getContentRootForFile(@Nonnull VirtualFile file, final boolean honorExclusion);

  /**
   * Returns the name of the package corresponding to the specified directory.
   *
   * @param dir the directory for which the package name is requested.
   * @return the package name, or null if the directory does not correspond to any package.
   */
  @Nullable
  String getPackageNameByDirectory(@Nonnull VirtualFile dir); //Q: move to FileIndex?

  /**
   * Returns true if <code>file</code> is a file which belongs to the classes (not sources) of some library.
   *
   * @param file the file to check.
   * @return true if the file belongs to library classes, false otherwise.
   */
  boolean isLibraryClassFile(@Nonnull VirtualFile file);

  /**
   * Returns true if <code>fileOrDir</code> is a file or directory from the content source or library sources.
   *
   * @param fileOrDir the file or directory to check.
   * @return true if the file or directory belongs to project or library sources, false otherwise.
   */
  boolean isInSource(@Nonnull VirtualFile fileOrDir);

  /**
   * Returns true if <code>fileOrDir</code> is a file or directory from the resources.
   *
   * @param fileOrDir the file or directory to check.
   * @return true if the file or directory belongs to resources, false otherwise.
   */
  boolean isInResource(@Nonnull VirtualFile fileOrDir);

  /**
   * Returns true if <code>fileOrDir</code> is a file or directory from the test resources.
   *
   * @param fileOrDir the file or directory to check.
   * @return true if the file or directory belongs to resources, false otherwise.
   */
  boolean isInTestResource(@Nonnull VirtualFile fileOrDir);

  /**
   * Returns true if <code>fileOrDir</code> is a file or directory from library classes.
   *
   * @param fileOrDir the file or directory to check.
   * @return true if the file belongs to library classes, false otherwise.
   */
  boolean isInLibraryClasses(@Nonnull VirtualFile fileOrDir);

  /**
   * Returns true if <code>fileOrDir</code> is a file or directory from library source.
   *
   * @param fileOrDir the file or directory to check.
   * @return true if the file belongs to library sources, false otherwise.
   */
  boolean isInLibrarySource(@Nonnull VirtualFile fileOrDir);

  default boolean isInLibrary(@Nonnull VirtualFile fileOrDir) {
    return isInLibrarySource(fileOrDir) || isInLibraryClasses(fileOrDir);
  }

  /**
   * @deprecated name of this method may be confusing. If you want to check if the file is excluded or ignored use {@link #isExcluded(com.intellij.openapi.vfs.VirtualFile)}.
   * If you want to check if the file is ignored use {@link com.intellij.openapi.fileTypes.FileTypeRegistry#isFileIgnored(com.intellij.openapi.vfs.VirtualFile)}.
   * If you want to check if the file or one of its parents is ignored use {@link #isUnderIgnored(com.intellij.openapi.vfs.VirtualFile)}.
   */
  @Deprecated
  boolean isIgnored(@Nonnull VirtualFile file);

  /**
   * Checks if the specified file or directory is located under project roots but the file itself or one of its parent directories is
   * either excluded from the project or ignored by {@link com.intellij.openapi.fileTypes.FileTypeRegistry#isFileIgnored(com.intellij.openapi.vfs.VirtualFile)}).
   *
   * @param file the file to check.
   * @return true if <code>file</code> is excluded or ignored, false otherwise.
   */
  boolean isExcluded(@Nonnull VirtualFile file);

  /**
   * Checks if the specified file or directory is located under project roots but the file itself or one of its parent directories is ignored
   * by {@link com.intellij.openapi.fileTypes.FileTypeRegistry#isFileIgnored(com.intellij.openapi.vfs.VirtualFile)}).
   *
   * @param file the file to check.
   * @return true if <code>file</code> is ignored, false otherwise.
   */
  boolean isUnderIgnored(@Nonnull VirtualFile file);
}
