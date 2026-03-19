/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package consulo.module.content;

import consulo.module.Module;
import consulo.module.UnloadedModuleDescription;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.virtualFileSystem.VirtualFile;
import consulo.content.ContentFolderTypeProvider;

import org.jspecify.annotations.Nullable;

public abstract class DirectoryInfo {
  /**
   * @return {@code true} if the whole directory is located under project content or library roots and not excluded or ignored
   * @deprecated use {@link #isInProject(VirtualFile)} instead, this method doesn't take {@link ContentEntry#getExcludePatterns()} into account
   */
  public abstract boolean isInProject();

  /**
   * @param file a file under the directory described by this instance.
   * @return {@code true} if {@code file} is located under project content or library roots and not excluded or ignored
   */
  public abstract boolean isInProject(VirtualFile file);

  /**
   * @return {@code true} if located under ignored directory
   */
  public abstract boolean isIgnored();

  /**
   * @return {@code true} if the whole directory is located in project content, output or library root but excluded from the project
   * @deprecated use {@link #isExcluded(VirtualFile)} instead, this method doesn't take {@link ContentEntry#getExcludePatterns()} into account
   */
  public abstract boolean isExcluded();

  /**
   * Returns {@code true} if {@code file} located under this directory is excluded from the project. If {@code file} is a directory it means
   * that all of its content is recursively excluded from the project.
   *
   * @param file a file under the directory described by this instance.
   */
  public abstract boolean isExcluded(VirtualFile file);

  /**
   * Returns {@code true} if {@code file} is located under a module source root and not excluded or ignored
   */
  public abstract boolean isInModuleSource(VirtualFile file);

  /**
   * @deprecated use {@link #isInModuleSource(VirtualFile)} instead, this method doesn't take {@link ContentEntry#getExcludePatterns() exclude patterns} into account
   */
  public abstract boolean isInModuleSource();

  /**
   * @return {@code true} if {@code file} located under this directory is located in library sources.
   * @deprecated use {@link #isInLibrarySource(VirtualFile)} instead, this method doesn't take {@link SyntheticLibrary#getExcludeFileCondition()} into account
   */
  public abstract boolean isInLibrarySource();

  /**
   * @param file a file under the directory described by this instance.
   * @return {@code true} if {@code file} located under this directory is located in library sources.
   * If {@code file} is a directory it means that all of its content is recursively in not part of the libraries.
   */
  public abstract boolean isInLibrarySource(VirtualFile file);

  public abstract @Nullable VirtualFile getSourceRoot();

  public abstract @Nullable ContentFolder getContentFolder();

  public abstract @Nullable ContentFolderTypeProvider getSourceRootTypeId();

  public boolean hasLibraryClassRoot() {
    return getLibraryClassRoot() != null;
  }

  public abstract VirtualFile getLibraryClassRoot();

  public abstract @Nullable VirtualFile getContentRoot();

  public abstract @Nullable Module getModule();

  /**
   * Return name of an unloaded module to which content this file or directory belongs
   * or {@code null} if it doesn't belong to an unloaded module.
   *
   * @see UnloadedModuleDescription
   */
  public abstract @Nullable String getUnloadedModuleName();
}
