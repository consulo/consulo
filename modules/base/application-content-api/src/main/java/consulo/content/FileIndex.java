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
package consulo.content;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Provides information about files contained in a project or module. Should be used from a read action.
 *
 * @see ProjectRootManager#getFileIndex()
 * @see ModuleRootManager#getFileIndex()
 */
public interface FileIndex {
  /**
   * Processes all files and directories under content roots skipping excluded and ignored files and directories.
   *
   * @return false if files processing was stopped ({@link ContentIterator#processFile(VirtualFile)} returned false)
   */
  boolean iterateContent(@Nonnull ContentIterator processor);

  /**
   * Same as {@link #iterateContent(ContentIterator)} but allows to pass {@code filter} to
   * provide filtering in condition for directories.
   * <p>
   * If {@code filter} returns false on a directory, the directory won't be processed, but iteration will go on.
   * <p>
   * {@code null} filter means that all directories should be processed.
   *
   * @return false if files processing was stopped ({@link ContentIterator#processFile(VirtualFile)} returned false)
   */
  boolean iterateContent(@Nonnull ContentIterator processor, @Nullable VirtualFileFilter filter);

  /**
   * Processes all files and directories in the content under directory {@code dir} (including the directory itself) skipping excluded
   * and ignored files and directories. Does nothing if {@code dir} is not in the content.
   *
   * @return false if files processing was stopped ({@link ContentIterator#processFile(VirtualFile)} returned false)
   */
  boolean iterateContentUnderDirectory(@Nonnull VirtualFile dir, @Nonnull ContentIterator processor);

  /**
   * Same as {@link #iterateContentUnderDirectory(VirtualFile, ContentIterator)} but allows to pass additional {@code customFilter} to
   * the iterator, in case you need to skip some file system branches using your own logic. If {@code customFilter} returns false on
   * a directory, it won't be processed, but iteration will go on.
   * <p>
   * {@code null} filter means that all directories should be processed.
   */
  boolean iterateContentUnderDirectory(@Nonnull VirtualFile dir, @Nonnull ContentIterator processor, @Nullable VirtualFileFilter customFilter);

  /**
   * Returns {@code true} if {@code fileOrDir} is a file or directory under a content root of this project or module and not excluded or
   * ignored.
   */
  boolean isInContent(@Nonnull VirtualFile fileOrDir);

  /**
   * Returns {@code true} if {@code file} is a file located under a sources, tests or resources root and not excluded or ignored.
   * <p>
   * Note that sometimes a file can belong to the content and be a source file but not belong to sources of the content.
   * This happens if sources of some library are located under the content (so they belong to the project content but not as sources).
   */
  boolean isContentSourceFile(@Nonnull VirtualFile file);

  /**
   * Returns {@code true} if {@code fileOrDir} is a file or directory located under a sources, tests or resources root and not excluded or ignored.
   */
  boolean isInSourceContent(@Nonnull VirtualFile fileOrDir);

  /**
   * Returns true if {@code fileOrDir} is a file or directory located under a test sources or resources root and not excluded or ignored.
   * <p>
   * Use this method when you really need to check whether the file is under test roots according to project configuration.
   * <p>
   * If you want to determine whether file should be considered as test (e.g. for implementing SearchScope)
   * you'd better use {@link TestSourcesFilter#isTestSources(VirtualFile, Project)} instead
   * which includes {@link ProjectFileIndex#isInTestSourceContent(VirtualFile)} invocation.
   *
   * @see TestSourcesFilter#isTestSources(VirtualFile, Project)
   */
  boolean isInTestSourceContent(@Nonnull VirtualFile fileOrDir);

  @Nullable
  ContentFolderTypeProvider getContentFolderTypeForFile(@Nonnull VirtualFile file);

  default boolean isUnderContentFolderType(@Nonnull VirtualFile virtualFile, @Nonnull ContentFolderTypeProvider contentFolderTypeProvider) {
    ContentFolderTypeProvider contentFolderTypeForFile = getContentFolderTypeForFile(virtualFile);
    return contentFolderTypeProvider == contentFolderTypeForFile;
  }

  default boolean isUnderContentFolderType(@Nonnull VirtualFile virtualFile, @Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    ContentFolderTypeProvider contentFolderTypeForFile = getContentFolderTypeForFile(virtualFile);
    return contentFolderTypeForFile != null && predicate.test(contentFolderTypeForFile);
  }
}
