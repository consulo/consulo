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
package com.intellij.openapi.vcs.changes.patch.tool;

import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.openapi.vcs.changes.patch.AppliedTextPatch;
import javax.annotation.Nonnull;

public class ApplyPatchDiffRequest extends DiffRequest implements ApplyPatchRequest {
  @Nonnull
  private final DocumentContent myResultContent;
  @Nonnull
  private final AppliedTextPatch myAppliedPatch;

  @Nonnull
  private final String myLocalContent;

  @javax.annotation.Nullable
  private final String myWindowTitle;
  @Nonnull
  private final String myLocalTitle;
  @Nonnull
  private final String myResultTitle;
  @Nonnull
  private final String myPatchTitle;

  public ApplyPatchDiffRequest(@Nonnull DocumentContent resultContent,
                               @Nonnull AppliedTextPatch appliedPatch,
                               @Nonnull String localContent,
                               @javax.annotation.Nullable String windowTitle,
                               @Nonnull String localTitle,
                               @Nonnull String resultTitle,
                               @Nonnull String patchTitle) {
    myResultContent = resultContent;
    myAppliedPatch = appliedPatch;
    myLocalContent = localContent;
    myWindowTitle = windowTitle;
    myLocalTitle = localTitle;
    myResultTitle = resultTitle;
    myPatchTitle = patchTitle;
  }

  @Override
  @Nonnull
  public DocumentContent getResultContent() {
    return myResultContent;
  }

  @Override
  @Nonnull
  public String getLocalContent() {
    return myLocalContent;
  }

  @Override
  @Nonnull
  public AppliedTextPatch getPatch() {
    return myAppliedPatch;
  }

  @javax.annotation.Nullable
  @Override
  public String getTitle() {
    return myWindowTitle;
  }

  @Override
  @Nonnull
  public String getLocalTitle() {
    return myLocalTitle;
  }

  @Override
  @Nonnull
  public String getResultTitle() {
    return myResultTitle;
  }

  @Override
  @Nonnull
  public String getPatchTitle() {
    return myPatchTitle;
  }
}
