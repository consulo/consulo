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
import com.intellij.diff.editor.ChainDiffVirtualFile;
import com.intellij.diff.editor.DiffEditorTabFilesManager;
import com.intellij.diff.impl.DiffRequestPanelImpl;
import com.intellij.diff.impl.DiffSettingsHolder;
import com.intellij.diff.impl.DiffWindow;
import com.intellij.diff.merge.*;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.binary.BinaryDiffTool;
import com.intellij.diff.tools.dir.DirDiffTool;
import com.intellij.diff.tools.external.ExternalDiffTool;
import com.intellij.diff.tools.external.ExternalMergeTool;
import com.intellij.diff.tools.fragmented.UnifiedDiffTool;
import com.intellij.diff.tools.simple.SimpleDiffTool;
import com.intellij.diff.util.DiffUtil;
import com.intellij.openapi.diff.DiffBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.WindowWrapper;
import com.intellij.openapi.wm.IdeFocusManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DiffManagerImpl extends DiffManagerEx {
  @RequiredUIAccess
  @Override
  public void showDiff(@Nullable Project project, @Nonnull DiffRequest request) {
    showDiff(project, request, DiffDialogHints.DEFAULT);
  }

  @RequiredUIAccess
  @Override
  public void showDiff(@Nullable Project project, @Nonnull DiffRequest request, @Nonnull DiffDialogHints hints) {
    DiffRequestChain requestChain = new SimpleDiffRequestChain(request);
    showDiff(project, requestChain, hints);
  }

  @RequiredUIAccess
  @Override
  public void showDiff(@Nullable Project project, @Nonnull DiffRequestChain requests, @Nonnull DiffDialogHints hints) {
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
  public void showDiffBuiltin(@Nullable Project project, @Nonnull DiffRequest request, @Nonnull DiffDialogHints hints) {
    DiffRequestChain requestChain = new SimpleDiffRequestChain(request);
    showDiffBuiltin(project, requestChain, hints);
  }

  @RequiredUIAccess
  @Override
  public void showDiffBuiltin(@Nullable Project project, @Nonnull DiffRequestChain requests, @Nonnull DiffDialogHints hints) {
    DiffEditorTabFilesManager diffEditorTabFilesManager = project != null ? DiffEditorTabFilesManager.getInstance(project) : null;
    if (diffEditorTabFilesManager != null &&
        DiffSettingsHolder.DiffSettings.getSettings().isShowDiffInEditor() &&
        DiffUtil.getWindowMode(hints) == WindowWrapper.Mode.FRAME &&
        !isFromDialog(project) &&
        hints.getWindowConsumer() == null) {
      ChainDiffVirtualFile diffFile = new ChainDiffVirtualFile(requests, DiffBundle.message("label.default.diff.editor.tab.name"));
      diffEditorTabFilesManager.showDiffFile(diffFile, true);
      return;
    }

    new DiffWindow(project, requests, hints).show();
  }

  private static boolean isFromDialog(@Nullable Project project) {
    return DialogWrapper.findInstance(IdeFocusManager.getInstance(project).getFocusOwner()) != null;
  }

  @Nonnull
  @Override
  public DiffRequestPanel createRequestPanel(@Nullable Project project, @Nonnull Disposable parent, @Nullable Window window) {
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
  public void showMergeBuiltin(@Nullable Project project, @Nonnull MergeRequest request) {
    new MergeWindow(project, request).show();
  }
}
