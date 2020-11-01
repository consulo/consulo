/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.dvcs.branch;

import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.ui.DvcsBundle;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Collection;

public class DvcsBranchUtil {


  @Nullable
  public static <T extends DvcsBranchInfo> T find(@Nullable final Collection<T> branches, @javax.annotation.Nullable Repository repository, @Nonnull String sourceBranch) {
    if (branches == null) return null;
    return ContainerUtil.find(branches, targetInfo -> repoAndSourceAreEqual(repository, sourceBranch, targetInfo));
  }

  private static boolean repoAndSourceAreEqual(@javax.annotation.Nullable Repository repository, @Nonnull String sourceBranch, @Nonnull DvcsBranchInfo targetInfo) {
    return getPathFor(repository).equals(targetInfo.repoPath) && StringUtil.equals(targetInfo.sourceName, sourceBranch);
  }

  @Nonnull
  public static String getPathFor(@Nullable Repository repository) {
    return repository == null ? "" : repository.getRoot().getPath();
  }

  @Nls
  @Nonnull
  public static String shortenBranchName(@Nls @Nonnull String fullBranchName) {
    // -1, because there are arrows indicating that it is a popup
    int maxLength = DvcsBundle.message("branch.popup.maximum.branch.length.sample").length() - 1;
    return StringUtil.shortenTextWithEllipsis(fullBranchName, maxLength, 5);
  }
}
