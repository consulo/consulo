// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

/**
 * Defines a search engine which will be used to find results in "Find in Path" and "Replace in Path" actions.
 * Several search engines can be used at the same moment to achieve best performance (time-to-result).
 */
public interface FindInProjectSearchEngine {
  ExtensionPointName<FindInProjectSearchEngine> EP_NAME = ExtensionPointName.create("com.intellij.findInProjectSearchEngine");

  /**
   * Constructs a searcher for a given {@param findModel} which serves as a input query.
   */
  @Nullable
  FindInProjectSearcher createSearcher(@Nonnull FindModel findModel, @Nonnull Project project);

  interface FindInProjectSearcher {
    /**
     * @return files that contain non-trivial search results for corresponding {@link FindModel}.
     */
    @Nonnull
    Collection<VirtualFile> searchForOccurrences();

    /**
     * @return true if there are no occurrences can be found outside result of {@link FindInProjectSearcher#searchForOccurrences()},
     * otherwise false.
     */
    boolean isReliable();

    /**
     * Returns true if {@param file} is a part of "indexed" scope of corresponding search engine and no need to open file's content to find a query,
     * otherwise false.
     * <p>
     * Called only in case when searcher is not reliable (see {@link FindInProjectSearcher#isReliable()}).
     */
    boolean isCovered(@Nonnull VirtualFile file);
  }
}
