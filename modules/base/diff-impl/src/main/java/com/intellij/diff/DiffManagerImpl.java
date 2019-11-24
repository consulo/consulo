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
package com.intellij.diff;

import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.SimpleDiffRequestChain;
import com.intellij.diff.impl.DiffRequestPanelImpl;
import com.intellij.diff.impl.DiffWindow;
import com.intellij.diff.merge.*;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.binary.BinaryDiffTool;
import com.intellij.diff.tools.dir.DirDiffTool;
import com.intellij.diff.tools.external.ExternalDiffTool;
import com.intellij.diff.tools.external.ExternalMergeTool;
import com.intellij.diff.tools.fragmented.UnifiedDiffTool;
import com.intellij.diff.tools.simple.SimpleDiffTool;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DiffManagerImpl extends DiffManagerEx {
  @RequiredUIAccess
  @Override
  public void showDiff(@javax.annotation.Nullable Project project, @Nonnull DiffRequest request) {
    showDiff(project, request, DiffDialogHints.DEFAULT);
  }

  @RequiredUIAccess
  @Override
  public void showDiff(@javax.annotation.Nullable Project project, @Nonnull DiffRequest request, @Nonnull DiffDialogHints hints) {
    DiffRequestChain requestChain = new SimpleDiffRequestChain(request);
    showDiff(project, requestChain, hints);
  }

  @RequiredUIAccess
  @Override
  public void showDiff(@javax.annotation.Nullable Project project, @Nonnull DiffRequestChain requests, @Nonnull DiffDialogHints hints) {
    if (ExternalDiffTool.isDefault()) {
      ExternalDiffTool.show(project, requests, hints);
      return;
    }

    showDiffBuiltin(project, requests, hints);
  }

  @RequiredUIAccess
  @Override
  public void showDiffBuiltin(@Nullable Project project, @Nonnull DiffRequest request) {
    showDiffBuiltin(project, request, DiffDialogHints.DEFAULT);
  }

  @RequiredUIAccess
  @Override
  public void showDiffBuiltin(@javax.annotation.Nullable Project project, @Nonnull DiffRequest request, @Nonnull DiffDialogHints hints) {
    DiffRequestChain requestChain = new SimpleDiffRequestChain(request);
    showDiffBuiltin(project, requestChain, hints);
  }

  @RequiredUIAccess
  @Override
  public void showDiffBuiltin(@javax.annotation.Nullable Project project, @Nonnull DiffRequestChain requests, @Nonnull DiffDialogHints hints) {
    new DiffWindow(project, requests, hints).show();
  }

  @Nonnull
  @Override
  public DiffRequestPanel createRequestPanel(@javax.annotation.Nullable Project project, @Nonnull Disposable parent, @Nullable Window window) {
    DiffRequestPanelImpl panel = new DiffRequestPanelImpl(project, window);
    Disposer.register(parent, panel);
    return panel;
  }

  @Nonnull
  @Override
  public List<DiffTool> getDiffTools() {
    List<DiffTool> result = new ArrayList<DiffTool>();
    result.addAll(DiffTool.EP_NAME.getExtensionList());
    result.add(SimpleDiffTool.INSTANCE);
    result.add(UnifiedDiffTool.INSTANCE);
    result.add(BinaryDiffTool.INSTANCE);
    result.add(DirDiffTool.INSTANCE);
    return result;
  }

  @Nonnull
  @Override
  public List<MergeTool> getMergeTools() {
    List<MergeTool> result = new ArrayList<MergeTool>();
    result.addAll(MergeTool.EP_NAME.getExtensionList());
    result.add(TextMergeTool.INSTANCE);
    result.add(BinaryMergeTool.INSTANCE);
    return result;
  }

  @Override
  @RequiredUIAccess
  public void showMerge(@Nullable Project project, @Nonnull MergeRequest request) {
    if (ExternalMergeTool.isDefault()) {
      ExternalMergeTool.show(project, request);
      return;
    }

    showMergeBuiltin(project, request);
  }

  @Override
  @RequiredUIAccess
  public void showMergeBuiltin(@javax.annotation.Nullable Project project, @Nonnull MergeRequest request) {
    new MergeWindow(project, request).show();
  }
}
