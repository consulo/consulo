/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.util.indexing;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.Set;

/**
 * @author peter
 */
public abstract class IndexableSetContributor {

  public static final ExtensionPointName<IndexableSetContributor> EP_NAME = ExtensionPointName.create("com.intellij.indexedRootsContributor");
  private static final Logger LOG = Logger.getInstance(IndexableSetContributor.class);

  @Nonnull
  public static Set<VirtualFile> getProjectRootsToIndex(@Nonnull IndexableSetContributor contributor, @Nonnull Project project) {
    Set<VirtualFile> roots = contributor.getAdditionalProjectRootsToIndex(project);
    return filterOutNulls(contributor, "getAdditionalProjectRootsToIndex(Project)", roots);
  }

  @Nonnull
  public static Set<VirtualFile> getRootsToIndex(@Nonnull IndexableSetContributor contributor) {
    Set<VirtualFile> roots = contributor.getAdditionalRootsToIndex();
    return filterOutNulls(contributor, "getAdditionalRootsToIndex()", roots);
  }

  /**
   * @return an additional project-dependent set of {@link VirtualFile} instances to index,
   * the returned set should not contain nulls or invalid files
   */
  @Nonnull
  public Set<VirtualFile> getAdditionalProjectRootsToIndex(@Nonnull Project project) {
    return Collections.emptySet();
  }

  /**
   * @return an additional project-independent set of {@link VirtualFile} instances to index,
   * the returned set should not contain nulls or invalid files
   */
  @Nonnull
  public abstract Set<VirtualFile> getAdditionalRootsToIndex();

  @Nonnull
  private static Set<VirtualFile> filterOutNulls(@Nonnull IndexableSetContributor contributor, @Nonnull String methodInfo, @Nonnull Set<VirtualFile> roots) {
    for (VirtualFile root : roots) {
      if (root == null || !root.isValid()) {
        LOG.error("Please fix " + contributor.getClass().getName() + "#" + methodInfo + ".\n" +
                  (root == null ? "The returned set is not expected to contain nulls, but it is " + roots : "Invalid file returned: " + root));
        return ContainerUtil.newLinkedHashSet(ContainerUtil.filter(roots, virtualFile -> virtualFile != null && virtualFile.isValid()));
      }
    }
    return roots;
  }
}
