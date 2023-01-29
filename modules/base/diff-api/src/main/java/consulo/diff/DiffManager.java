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
package consulo.diff;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.diff.chain.DiffRequestChain;
import consulo.diff.merge.MergeRequest;
import consulo.diff.request.DiffRequest;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class DiffManager {
  @Nonnull
  public static DiffManager getInstance() {
    return Application.get().getInstance(DiffManager.class);
  }

  //
  // Usage
  //

  @RequiredUIAccess
  public abstract void showDiff(@Nullable Project project, @Nonnull DiffRequest request);

  @RequiredUIAccess
  public abstract void showDiff(@Nullable Project project, @Nonnull DiffRequest request, @Nonnull DiffDialogHints hints);

  @RequiredUIAccess
  public abstract void showDiff(@Nullable Project project, @Nonnull DiffRequestChain requests, @Nonnull DiffDialogHints hints);

  @Nonnull
  public abstract DiffRequestPanel createRequestPanel(@Nullable Project project, @Nonnull Disposable parent, @Nullable Window window);

  @RequiredUIAccess
  public abstract void showMerge(@Nullable Project project, @Nonnull MergeRequest request);
}
