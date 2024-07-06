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
package consulo.diff.internal;

import consulo.diff.DiffDialogHints;
import consulo.diff.DiffManager;
import consulo.diff.DiffTool;
import consulo.diff.chain.DiffRequestChain;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeTool;
import consulo.diff.request.DiffRequest;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public abstract class DiffManagerEx extends DiffManager {
  @Nonnull
  public static DiffManagerEx getInstanceEx() {
    return (DiffManagerEx)DiffManager.getInstance();
  }

  //
  // Usage
  //

  @RequiredUIAccess
  public abstract void showDiffBuiltin(@Nullable Project project, @Nonnull DiffRequest request);

  @RequiredUIAccess
  public abstract void showDiffBuiltin(@Nullable Project project, @Nonnull DiffRequest request, @Nonnull DiffDialogHints hints);

  @RequiredUIAccess
  public abstract void showDiffBuiltin(@Nullable Project project, @Nonnull DiffRequestChain requests, @Nonnull DiffDialogHints hints);

  @RequiredUIAccess
  public abstract void showMergeBuiltin(@Nullable Project project, @Nonnull MergeRequest request);

  @Nonnull
  public abstract List<DiffTool> getDiffTools();

  @Nonnull
  public abstract List<MergeTool> getMergeTools();
}
